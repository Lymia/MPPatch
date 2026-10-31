use crate::versions::{find_info, Platform, VersionInfo};
use enumset::*;
use errcode::prelude::{ensure, Result};
use log::{error, info, trace, LevelFilter};
use simplelog::{
    ColorChoice, CombinedLogger, ConfigBuilder, SharedLogger, TermLogger, TerminalMode,
    ThreadLogMode, WriteLogger,
};
use std::{
    backtrace::Backtrace,
    fs::File,
    panic,
    path::{Path, PathBuf},
    ptr::null_mut,
    sync::atomic::{AtomicPtr, Ordering},
};
use errcode::bail;

/// The context for a particular load of the binary.
#[derive(Clone, Debug)]
pub struct MppatchCtx {
    exe_dir: PathBuf,
    config: MppatchConfig,
    pub version_info: VersionInfo,
}
impl MppatchCtx {
    pub fn exe_dir(&self) -> &Path {
        &self.exe_dir
    }
    pub fn sha256(&self) -> &str {
        &self.config.bin_sha256
    }
    pub fn version(&self) -> &'static str {
        option_env!("MPPATCH_VERSION").unwrap_or_else(|| "<unknown>")
    }
    pub fn build_id(&self) -> &'static str {
        option_env!("MPPATCH_BUILDID").unwrap_or_else(|| "00000000-0000-0000-0000-000000000000")
    }
    pub fn has_feature(&self, feature: MppatchFeature) -> bool {
        self.config.features.contains(feature)
    }
}

/// The configuration file for MPPatch
#[derive(Clone, Debug)]
struct MppatchConfig {
    bin_sha256: String,
    features: EnumSet<MppatchFeature>,
}

#[derive(EnumSetType, Debug)]
pub enum MppatchFeature {
    Multiplayer,
    LuaJit,
    Logging,
    Debug,
}

fn early_setup() {
    // force enable backtraces
    if std::env::var("RUST_BACKTRACE").is_err() {
        std::env::set_var("RUST_BACKTRACE", "1")
    }
}

#[cfg(windows)]
fn win32_text_hash(path: &Path) -> String {
    use sha2::{Digest, Sha256};

    let data = std::fs::read(path)
        .ok()
        .expect("Could not read Civilization V binary.");
    let pe = pe_parser::pe::parse_portable_executable(&data).expect("Could not parse PE file.");
    for section in pe.section_table {
        if section.name == *b".text\0\0\0" {
            let start = section.pointer_to_raw_data as usize;
            let len = section.size_of_raw_data.min(section.virtual_size) as usize;
            let segment = &data[start..start + len];

            let mut out = String::new();
            for byte in Sha256::digest(segment) {
                out.push_str(&format!("{byte:02x}"));
            }
            log::debug!(".text hash: {out}");
            return out;
        }
    }
    panic!("Could not find .text section!")
}

fn create_ctx() -> Result<MppatchCtx> {
    let exe_path = std::env::current_exe()?;
    let mut exe_dir = exe_path.canonicalize()?;
    exe_dir.pop();

    let mut config_file = exe_dir.clone();
    config_file.push("mppatch_config.cfg");
    ensure!(config_file.exists(), "mppatch_config.cfg is missing!");
    let config = std::io::read_to_string(File::open(config_file)?)?;
    let config = {
        assert!(config.contains("\n"));
        let mut split = config.split("\n");
        let sha256 = split.next().unwrap();
        let packages_str = split.next().unwrap();

        let mut packages = EnumSet::new();
        for package in packages_str.split(",") {
            packages.insert(match package {
                "Multiplayer" => MppatchFeature::Multiplayer,
                "LuaJit" => MppatchFeature::LuaJit,
                "Logging" => MppatchFeature::Logging,
                "Debug" => MppatchFeature::Debug,
                _ => bail!("Unknown package: {package}"),
            });
        }

        MppatchConfig {
            bin_sha256: sha256.to_string(),
            features: packages,
        }
    };

    #[cfg(unix)]
    let text_hash = String::new();

    #[cfg(windows)]
    let text_hash = win32_text_hash(&exe_path);

    let version_info = find_info(&config.bin_sha256, &text_hash)?;

    Ok(MppatchCtx { exe_dir, config, version_info })
}

fn setup_logging(ctx: &MppatchCtx) {
    let mut log_file = ctx.exe_dir.clone();
    log_file.push("mppatch_debug.log");

    let mut loggers: Vec<Box<dyn SharedLogger>> = Vec::new();
    loggers.push(TermLogger::new(
        LevelFilter::Trace,
        ConfigBuilder::default()
            .set_thread_level(LevelFilter::Error)
            .set_thread_mode(ThreadLogMode::Both)
            .set_target_level(LevelFilter::Error)
            .set_location_level(LevelFilter::Off)
            .build(),
        TerminalMode::Stderr,
        ColorChoice::Auto,
    ));
    if ctx.has_feature(MppatchFeature::Logging) {
        loggers.push(WriteLogger::new(
            LevelFilter::Trace,
            ConfigBuilder::default()
                .set_thread_level(LevelFilter::Error)
                .set_thread_mode(ThreadLogMode::Both)
                .set_target_level(LevelFilter::Off)
                .set_location_level(LevelFilter::Error)
                .set_time_format_rfc2822()
                .build(),
            File::create(log_file).expect("Cannot open log file."),
        ));
    }
    CombinedLogger::init(loggers).unwrap();
}

static CTX: AtomicPtr<MppatchCtx> = AtomicPtr::new(null_mut());

pub fn run() -> Result<&'static MppatchCtx> {
    early_setup();
    let ctx = create_ctx()?;
    setup_logging(&ctx);
    register_panic_hook();
    if ctx.version_info.platform != Platform::CURRENT {
        panic!("Specified Civilization version is not compatible with this patch.");
    }

    info!("mpp_hooklib v{} loaded", ctx.version());
    info!("Game version: {}", ctx.version_info.name);
    info!("");
    trace!("build-id: {}", ctx.build_id());
    trace!("ctx: {ctx:#?}");

    assert!(CTX
        .swap(Box::leak(Box::new(ctx)), Ordering::SeqCst)
        .is_null());

    Ok(get_ctx())
}

pub fn get_ctx() -> &'static MppatchCtx {
    let ptr = CTX.load(Ordering::SeqCst);
    assert!(!ptr.is_null());
    unsafe { &*ptr }
}

#[linktime::dtor(unsafe)]
fn destroy_ctx() {
    let ptr = CTX.swap(null_mut(), Ordering::SeqCst);
    if !ptr.is_null() {
        unsafe { drop(Box::from_raw(ptr)) }
    }
}

fn register_panic_hook() {
    panic::set_hook(Box::new(move |info| {
        let str = if let Some(str) = info.payload().downcast_ref::<&'static str>() {
            (*str).to_owned()
        } else if let Some(str) = info.payload().downcast_ref::<String>() {
            str.clone()
        } else {
            "(no message found)".to_string()
        };
        error!("{str}");
        dump_backtrace(&Backtrace::force_capture());
        fatal_error(&format!("Internal error: {str}"));
    }));
}

pub fn check_error<T>(err: Result<T>) -> T {
    match err {
        Ok(v) => v,
        Err(e) => {
            error!("Error occurred: {e}");
            fatal_error(&format!("Internal error: {e}"));
        }
    }
}

fn dump_backtrace(backtrace: &Backtrace) {
    error!("Backtrace:");
    let backtrace_str = format!("{backtrace}");

    let mut counter = 0;
    for line in backtrace_str.split('\n') {
        if line.contains("<unknown>") {
            counter += 1;
            if counter == 3 {
                error!("(further lines skipped)");
                break;
            }
        } else {
            counter = 0;
        }
        error!("{line}");
    }
}

fn fatal_error(_: &str) -> ! {
    // TODO: Show a msgbox
    std::process::abort()
}
