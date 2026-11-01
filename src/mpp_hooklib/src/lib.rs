mod hook_lua;
#[cfg(unix)]
mod hook_luajit;
mod hook_netpatch;
#[cfg(windows)]
mod hook_proxy;
mod rt_cpplist;
mod rt_init;
mod rt_linking;

unsafe fn ctor_impl() -> errcode::prelude::Result<()> {
    let ctx = rt_init::run()?;
    rt_linking::init(ctx)?;
    rt_linking::apply_noceg(&ctx.version_info.ceg_patches);
    if ctx.has_feature(crate::rt_init::MppatchFeature::Multiplayer) {
        #[cfg(windows)]
        hook_proxy::init(ctx)?;
        hook_lua::init(&ctx)?;
        hook_netpatch::init(&ctx)?;
    }
    #[cfg(unix)]
    if ctx.has_feature(crate::rt_init::MppatchFeature::LuaJit) {
        hook_luajit::init(&ctx)?;
    }
    Ok(())
}

#[linktime::ctor(unsafe)]
unsafe fn ctor() {
    mpp_common::logging::check_error(ctor_impl());
}
