/*
 * Copyright (c) 2015-2023 Lymia Kanokawa <lymia@lymia.moe>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

use crate::versions::{find_info, VersionInfo};
use anyhow::{ensure, Result};
use enumset::*;
use log::{info, trace, LevelFilter};
use serde::*;
use simplelog::{
    ColorChoice, CombinedLogger, ConfigBuilder, SharedLogger, TermLogger, TerminalMode,
    ThreadLogMode, WriteLogger,
};
use std::{
    fs::File,
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
        match option_env!("MPPATCH_VERSION") {
            Some(x) => x,
            None => "<unknown>",
        }
    }
    pub fn build_id(&self) -> &'static str {
        match option_env!("MPPATCH_BUILDID") {
            Some(x) => x,
            None => "00000000-0000-0000-0000-000000000000",
        }
    }
    pub fn has_feature(&self, feature: MppatchFeature) -> bool {
        self.config.features.contains(feature)
    }
}

/// The configuration file for MPPatch
#[derive(Deserialize, Clone, Debug)]
struct MppatchConfig {
    bin_sha256: String,
    features: EnumSet<MppatchFeature>,
}

#[derive(EnumSetType, Serialize, Deserialize, Debug)]
#[enumset(serialize_repr = "list")]
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

fn create_ctx() -> Result<MppatchCtx> {
    let mut exe_dir = std::env::current_exe()?.canonicalize()?;
    exe_dir.pop();

    let mut config_file = exe_dir.clone();
    config_file.push("mppatch_config.toml");
    ensure!(config_file.exists(), "mppatch_config.toml is missing!");
    let config = std::io::read_to_string(File::open(config_file)?)?;
    let config = toml::from_str::<MppatchConfig>(&config)?;

    let version_info = find_info(&config.bin_sha256)?;

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
                .set_target_level(LevelFilter::Error)
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

    info!("mppatch-core v{} loaded", ctx.version());
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

#[ctor::dtor]
fn destroy_ctx() {
    let ptr = CTX.swap(null_mut(), Ordering::SeqCst);
    if !ptr.is_null() {
        unsafe { drop(Box::from_raw(ptr)) }
    }
}
