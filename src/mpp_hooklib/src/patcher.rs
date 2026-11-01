use dlopen::raw::Library;
use errcode::prelude::Result;
use mpp_common::offset::{Offset, PatchedFunction, RelocatedOffset};
use mpp_version::{ProxySource, SymbolInfo};
use std::{any::type_name, ffi::c_void, marker::PhantomData, mem::size_of, ptr};

unsafe fn runtime_transmute<T: Copy, V: Copy>(t: T) -> V {
    if size_of::<T>() == size_of::<V>() {
        ptr::read(&t as *const T as *const V)
    } else {
        panic!("size_of::<{}>() != size_of::<{}>()", type_name::<T>(), type_name::<V>())
    }
}

#[cfg(windows)]
fn lib_for_source(
    source: ProxySource,
) -> &'static dyn crate::patches_base::win32_dll_proxy::ProxyLibrary {
    match source {
        ProxySource::CvGameDatabase => &crate::patches_base::win32_dll_proxy::Proxy_CvGameDatabase,
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
                mpp_common::logging::check_error(lib_for_source(*source).unpatch(sym))
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
        offset: Offset,
        size: usize,
        name: &str,
        target: T,
    ) -> Result<()> {
        let offset = offset.relocate();
        let patch = offset.patch_function(runtime_transmute(target), size, name);
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
                    log::debug!("Original function pointer ({sym}) = {orig:?}");
                }
                #[cfg(unix)]
                SymbolInfo::DllProxy(source, name) => {
                    panic!("DllProxy does not exist on Unix: {source:?} / {name:?}")
                }
                SymbolInfo::PublicNamed(sym, sym_size) => {
                    let dylib_civ = Library::open_self()?;
                    let patch_addr = RelocatedOffset::from_ptr(dylib_civ.symbol(sym)?);
                    let target_addr = RelocatedOffset::from_ptr(runtime_transmute(target));
                    let patch = patch_addr.patch_function(target_addr, sym_size, sym);
                    self.patch_info = Some(PatchTarget::PatchedFunction(patch));
                }
                SymbolInfo::Offset(name, offset, length) => {
                    self.patch_offset(offset, length, name, target)?
                }
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
                SymbolInfo::Offset(_, offset, _) => runtime_transmute(offset.relocate()),
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
