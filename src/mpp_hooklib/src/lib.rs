#[macro_use]
mod macros;

mod context;
mod patcher;
mod patches;
mod patches_base;

#[cfg(windows)]
extern crate libz_rs_sys;

#[cfg(windows)]
pub use libz_rs_sys::*;
