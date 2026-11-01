//! This module handles disabling CEG integrity checks on Windows.

use log::info;

#[linktime::ctor(unsafe)]
fn apply_noceg() {
    let ctx = crate::context::get_ctx();
    unsafe {
        if let Some(patches) = &ctx.version_info.ceg_patches {
            info!("Disabling CEG integrity checks...");
            for (offset, ret_v) in patches.static_functions {
                offset.relocate().stub_function_value(*ret_v)
            }
            for (offset, ret_v) in patches.relative_functions {
                offset.relocate().stub_function_offset(ret_v.relocate())
            }
            for offset in patches.stub_functions {
                offset.relocate().stub_function_true()
            }
        }
    }
}
