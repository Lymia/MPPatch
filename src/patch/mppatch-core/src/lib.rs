use crate::rt_init::MppatchFeature;

mod hook_lua;
#[cfg(unix)]
mod hook_luajit;
mod hook_netpatch;
#[cfg(windows)]
mod hook_proxy;
mod rt_cpplist;
mod rt_init;
mod rt_linking;
mod rt_patch;
mod rt_platform;
mod versions;

fn ctor_impl() -> anyhow::Result<()> {
    let ctx = rt_init::run()?;
    rt_linking::init(ctx)?;
    if ctx.has_feature(MppatchFeature::Multiplayer) {
        #[cfg(windows)]
        hook_proxy::init(ctx)?;
        hook_lua::init(&ctx)?;
        hook_netpatch::init(&ctx)?;
    }
    #[cfg(unix)]
    if ctx.has_feature(MppatchFeature::LuaJit) {
        hook_luajit::init(&ctx)?;
    }
    Ok(())
}

#[linktime::ctor(unsafe)]
fn ctor() {
    rt_init::check_error(ctor_impl());
}
