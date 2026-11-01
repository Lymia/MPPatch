use enumset::*;
use errcode::prelude::*;
use log::{debug, info, trace, LevelFilter};
use mpp_common::logging::{check_error, LoggerOptions};
use mpp_version::{Platform, VersionInfo};
use std::{
    fs::File,
    panic,
    path::{Path, PathBuf},
    sync::{Arc, RwLock},
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

static CTX: RwLock<Option<Arc<MppatchCtx>>> = RwLock::new(None);

#[linktime::ctor(unsafe, priority = early)]
fn init() {
    check_error(init_impl())
}

fn init_impl() -> Result<()> {
    // construct the MPPatch context
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
    if ctx.version_info.platform != Platform::CURRENT {
        panic!("Specified Civilization version is not compatible with this patch.");
    }

    // log information about the loaded MPPatch version
    info!("mpp_hooklib v{} loaded", ctx.version());
    info!("Game version: {}", ctx.version_info.name);
    info!("");
    trace!("build-id: {}", ctx.build_id());
    trace!("ctx: {ctx:#?}");

    // find the main executable base address
    unsafe {
        #[cfg(windows)]
        let base_offset = winapi::um::libloaderapi::GetModuleHandleA(std::ptr::null()) as usize;
        #[cfg(unix)]
        let base_offset = 0; // we don't use this mechanism on unix

        debug!("Relocated binary offset: 0x{:08x}", base_offset);
        mpp_common::offset::set_relocation_info(ctx.version_info.binary_base, base_offset);
    }

    // add the context to memory
    *CTX.write()? = Some(Arc::new(ctx));

    Ok(())
}

pub fn get_ctx() -> Arc<MppatchCtx> {
    if let Some(x) = check_error(CTX.read().map_err(Into::into)).as_ref() {
        x.clone()
    } else {
        panic!("get_ctx after MppatchCtx has been destroyed!")
    }
}

#[linktime::dtor(unsafe)]
fn destroy_ctx() {
    if let Ok(mut ctx) = CTX.write() {
        drop(ctx.take());
    }
}
