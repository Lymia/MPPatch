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

use crate::{
    rt_init,
    rt_init::MppatchCtx,
    rt_patch::PatchedFunction,
    versions::{ProxySource, SymbolInfo},
};
use anyhow::{bail, Result};
use atomic::Atomic;
use bytemuck::NoUninit;
use dlopen::raw::Library;
use log::debug;
use std::{
    any::type_name,
    ffi::c_void,
    marker::PhantomData,
    mem::size_of,
    ptr,
    sync::atomic::{AtomicUsize, Ordering},
};

unsafe fn runtime_transmute<T: Copy, V: Copy>(t: T) -> V {
    if size_of::<T>() == size_of::<V>() {
        ptr::read(&t as *const T as *const V)
    } else {
        panic!("size_of::<{}>() != size_of::<{}>()", type_name::<T>(), type_name::<V>())
    }
}

#[cfg(windows)]
fn lib_for_source(source: ProxySource) -> &'static dyn crate::hook_proxy::ProxyLibrary {
    match source {
        ProxySource::CvGameDatabase => &crate::hook_proxy::Proxy_CvGameDatabase,
    }
}

enum PatchTarget {
    PatchedFunction(PatchedFunction),
    #[cfg_attr(unix, allow(dead_code))]
    ProxyTarget(ProxySource, &'static str, *const c_void),
}
impl PatchTarget {
    unsafe fn as_func<T: Copy>(&self) -> T {
        let ptr = match self {
            PatchTarget::PatchedFunction(ptr) => ptr.old_function(),
            PatchTarget::ProxyTarget(_, _, ptr) => *ptr,
        };
        runtime_transmute(ptr)
    }
}
unsafe impl Sync for PatchTarget {}
unsafe impl Send for PatchTarget {}
#[cfg(windows)]
impl Drop for PatchTarget {
    fn drop(&mut self) {
        match self {
            PatchTarget::PatchedFunction(_) => {}
            PatchTarget::ProxyTarget(source, sym, _) => unsafe {
                rt_init::check_error(lib_for_source(*source).unpatch(sym))
            },
        }
    }
}

pub struct PatcherContext<T: Copy> {
    patch_info: Option<PatchTarget>,
    _phantom: PhantomData<T>,
}
impl<T: Copy> PatcherContext<T> {
    pub const fn new() -> Self {
        PatcherContext { patch_info: None, _phantom: PhantomData }
    }

    unsafe fn patch_offset(
        &mut self,
        (offset, size): (usize, usize),
        name: &str,
        target: T,
    ) -> Result<()> {
        let offset = adjust_offset(offset);
        let patch =
            PatchedFunction::create(offset as *mut c_void, runtime_transmute(target), size, name);
        self.patch_info = Some(PatchTarget::PatchedFunction(patch));
        Ok(())
    }
    pub unsafe fn patch(&mut self, sym_info: SymbolInfo, target: T) -> Result<()> {
        unsafe {
            // remove an old patch if one existed
            self.patch_info.take();

            // patch the target location
            match sym_info {
                #[cfg(windows)]
                SymbolInfo::DllProxy(source, sym) => {
                    let orig = lib_for_source(source).patch(sym, runtime_transmute(target))?;
                    self.patch_info = Some(PatchTarget::ProxyTarget(source, sym, orig));
                    debug!("Original function pointer ({sym}) = {orig:?}");
                }
                #[cfg(unix)]
                SymbolInfo::DllProxy(_, _) => panic!("DllProxy does not exist on Unix."),
                SymbolInfo::PublicNamed(sym, sym_size) => {
                    let dylib_civ = Library::open_self()?;
                    let patch = PatchedFunction::create(
                        dylib_civ.symbol(sym)?,
                        runtime_transmute(target),
                        sym_size,
                        sym,
                    );
                    self.patch_info = Some(PatchTarget::PatchedFunction(patch));
                }
                SymbolInfo::Win32Offsets(offsets) => match BINARY_TYPE.load(Ordering::Relaxed) {
                    BinaryType::Dx9 => self.patch_offset(offsets.dx9, offsets.name, target)?,
                    BinaryType::Dx11 => self.patch_offset(offsets.dx11, offsets.name, target)?,
                    BinaryType::Tablet => {
                        self.patch_offset(offsets.tablet, offsets.name, target)?
                    }
                    _ => bail!("incorrect BINARY_TYPE!"),
                },
            }
        }

        Ok(())
    }

    pub unsafe fn as_func_fallback(&self, sym_info: SymbolInfo) -> T {
        match self.patch_info.as_ref() {
            Some(x) => x.as_func(),
            None => match sym_info {
                SymbolInfo::DllProxy(_, _) => unimplemented!("DllProxy fallback"),
                SymbolInfo::PublicNamed(_, _) => unimplemented!("PublicNamed fallback"),
                SymbolInfo::Win32Offsets(offsets) => {
                    let offset = match BINARY_TYPE.load(Ordering::Relaxed) {
                        BinaryType::Dx9 => offsets.dx9.0,
                        BinaryType::Dx11 => offsets.dx11.0,
                        BinaryType::Tablet => offsets.tablet.0,
                        _ => panic!("incorrect BINARY_TYPE!"),
                    };
                    let offset = adjust_offset(offset);
                    runtime_transmute(offset)
                }
            },
        }
    }

    pub unsafe fn as_func(&self) -> T {
        self.patch_info.as_ref().unwrap().as_func()
    }

    pub fn unpatch(&mut self) {
        self.patch_info.take();
    }
}

/// The type of binary that was launched.
#[derive(Copy, Clone, Debug, NoUninit)]
#[repr(u8)]
pub enum BinaryType {
    /// DX9 Windows binaries
    Dx9,
    /// DX11 Windows binaries
    Dx11,
    /// Tablet Windows binaries
    Tablet,
    /// Any POSIX binary
    Posix,
    /// Not known
    Unknown,
}

static BINARY_TYPE: Atomic<BinaryType> = Atomic::new(BinaryType::Unknown);
static BINARY_BASE: AtomicUsize = AtomicUsize::new(0);

fn adjust_offset(offset: usize) -> usize {
    let ctx = rt_init::get_ctx();
    (offset - ctx.version_info.binary_base) + BINARY_BASE.load(Ordering::Relaxed)
}

pub fn init(_: &MppatchCtx) -> Result<()> {
    let exe_name = std::env::current_exe()?;
    let exe_name = exe_name.file_name().unwrap();

    // find the binary type (mostly needed for Windows, where we hook a .dll)
    let ty = match exe_name.to_string_lossy().as_ref() {
        "CivilizationV.exe" => BinaryType::Dx9,
        "CivilizationV_DX11.exe" => BinaryType::Dx11,
        "CivilizationV_Tablet.exe" => BinaryType::Tablet,
        "Civ5XP" => BinaryType::Posix,
        "Civ5XP.orig" => BinaryType::Posix,
        exe_name => {
            #[cfg(unix)]
            {
                log::warn!("Unknown exe name: {exe_name}");
                BinaryType::Posix
            }

            #[cfg(windows)]
            bail!("Unknown exe name: {exe_name}")
        }
    };
    BINARY_TYPE.store(ty, Ordering::Relaxed);

    // find the main executable base address
    #[cfg(windows)]
    let base_offset = unsafe { winapi::um::libloaderapi::GetModuleHandleA(ptr::null()) as usize };
    #[cfg(unix)]
    let base_offset = 0; // we don't use this mechanism on unix
    BINARY_BASE.store(base_offset, Ordering::Relaxed);

    // print information and return
    debug!("Binary type: {:?}", BINARY_TYPE.load(Ordering::Relaxed));
    debug!("Binary offset: 0x{:08x}", BINARY_BASE.load(Ordering::Relaxed));
    Ok(())
}
