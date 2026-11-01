//! This module contains the code for applying the base patches required for MPPatch to function
//! correctly on certain platforms.

#[cfg(unix)]
mod unix_luajit;
#[cfg(windows)]
pub mod win32_dll_proxy;
#[cfg(windows)]
mod win32_noceg;
