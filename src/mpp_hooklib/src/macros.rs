#[macro_export]
macro_rules! make_func_proxy {
    (
        $mod_name:ident, $old_func:ident, $sym_ident:ident,
        (posix_repr: $posix_repr:literal, win_repr: $win_repr:literal $(,)?),
        ($($arg_name:ident: $arg_ty:ty),* $(,)?) -> $ret_ty:ty
    ) => {
        mod $mod_name {
            use errcode::prelude::Result;
            use std::sync::Mutex;
            use super::*;

            #[cfg(unix)]
            pub type FnType = unsafe extern $posix_repr fn($($arg_ty,)*) -> $ret_ty;

            #[cfg(windows)]
            pub type FnType = unsafe extern $win_repr fn($($arg_ty,)*) -> $ret_ty;

            pub static FUNCTION_PTR: Mutex<Option<FnType>> =
                Mutex::new(None);

            #[linktime::ctor(unsafe, priority = late)]
            fn init() {
                check_error(init_impl())
            }

            fn init_impl() -> Result<()> {
                let ctx = crate::context::get_ctx();
                unsafe {
                    *FUNCTION_PTR.lock().unwrap() =
                        Some(crate::patcher::get_symbol(ctx.version_info.$sym_ident)?);
                }
                Ok(())
            }
        }

        unsafe fn $old_func($($arg_name: $arg_ty,)*) -> $ret_ty {
            unsafe {
                let func = $mod_name::FUNCTION_PTR.lock().unwrap().clone().unwrap();
                func($($arg_name,)*)
            }
        }
    };
}

#[macro_export]
macro_rules! make_func_overwrite {
    (
        $mod_name:ident, $old_func:ident, $new_func:ident, $sym_ident:ident, $feature:expr,
        (posix_repr: $posix_repr:literal, win_repr: $win_repr:literal $(,)?),
        ($($arg_name:ident: $arg_ty:ty),* $(,)?) -> $ret_ty:ty
        $(,)?
    ) => {
        mod $mod_name {
            use errcode::prelude::Result;
            use std::sync::Mutex;
            use super::*;

            #[cfg(unix)]
            pub type FnType = unsafe extern $posix_repr fn($($arg_ty,)*) -> $ret_ty;

            #[cfg(windows)]
            pub type FnType = unsafe extern $win_repr fn($($arg_ty,)*) -> $ret_ty;

            pub static FUNCTION_PTR: Mutex<PatcherContext<FnType>> =
                Mutex::new(PatcherContext::new());

            #[linktime::ctor(unsafe, priority = late)]
            fn init() {
                check_error(init_impl())
            }

            fn init_impl() -> Result<()> {
                let ctx = crate::context::get_ctx();
                let allowed = match $feature {
                    Some(x) => ctx.has_feature(x),
                    None => true,
                };
                if allowed {
                    log::info!("Applying patch to {}...", stringify!($sym_ident));
                    unsafe {
                        FUNCTION_PTR
                            .lock()
                            .unwrap()
                            .patch(ctx.version_info.$sym_ident, proxy_func)?;
                    }
                }
                Ok(())
            }

            #[linktime::dtor(unsafe)]
            fn dtor() {
                FUNCTION_PTR.lock().unwrap().unpatch();
            }

            #[cfg(unix)]
            pub unsafe extern $posix_repr fn proxy_func($($arg_name: $arg_ty,)*) -> $ret_ty {
                unsafe { super::$new_func($($arg_name,)*) }
            }

            #[cfg(windows)]
            pub unsafe extern $win_repr fn proxy_func($($arg_name: $arg_ty,)*) -> $ret_ty {
                unsafe { super::$new_func($($arg_name,)*) }
            }
        }

        unsafe fn $old_func($($arg_name: $arg_ty,)*) -> $ret_ty {
            unsafe {
                let func = $mod_name::FUNCTION_PTR.lock().unwrap().as_func();
                func($($arg_name,)*)
            }
        }
    };
}
