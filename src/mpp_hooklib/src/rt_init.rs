use enumset::*;
use errcode::prelude::*;
use log::{error, info, trace, LevelFilter};
use mpp_common::logging::LoggerOptions;
use mpp_version::{Platform, VersionInfo};
use std::{
    fs::File,
    panic,
    path::{Path, PathBuf},
    ptr::null_mut,
    sync::atomic::{AtomicPtr, Ordering},
};

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

#[cfg(windows)]
fn win32_text_hash(path: &Path) -> Result<String> {
    use sha2::{Digest, Sha256};

    let data = std::fs::read(path)?;
    let pe = pe_parser::pe::parse_portable_executable(&data)?;
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
            return Ok(out);
        }
    }
    bail!("Could not find .text section!")
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

        MppatchConfig { bin_sha256: sha256.to_string(), features: packages }
    };

    #[cfg(unix)]
    let text_hash = String::new();

    #[cfg(windows)]
    let text_hash =
        win32_text_hash(&exe_path).with_context(error_info!("Parsing hash for the main binary"))?;

    let version_info = mpp_version::find_info(&config.bin_sha256, &text_hash)?;

    Ok(MppatchCtx { exe_dir, config, version_info })
}

static CTX: AtomicPtr<MppatchCtx> = AtomicPtr::new(null_mut());

pub fn run() -> Result<&'static MppatchCtx> {
    let ctx = create_ctx()?;
    mpp_common::logging::register_logger(LoggerOptions {
        log_file_name: "mppatch_debug.log",
        log_append: false,
        log_level: if ctx.has_feature(MppatchFeature::Debug) {
            LevelFilter::Trace
        } else {
            LevelFilter::Info
        },
    });
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
        fatal_error(&format!("Internal error: {str}"));
    }));
}

fn fatal_error(_: &str) -> ! {
    // TODO: Show a msgbox
    std::process::abort()
}
