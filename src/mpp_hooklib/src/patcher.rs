use dlopen::raw::Library;
use errcode::{bail, prelude::Result};
use mpp_common::offset::{PatchedFunction, RelocatedOffset};
use mpp_version::SymbolInfo;
use std::{any::type_name, marker::PhantomData, mem::size_of, ptr};

unsafe fn runtime_transmute<T: Copy, V: Copy>(t: T) -> V {
    if size_of::<T>() == size_of::<V>() {
        ptr::read(&t as *const T as *const V)
    } else {
        panic!("size_of::<{}>() != size_of::<{}>()", type_name::<T>(), type_name::<V>())
    }
}

pub struct PatcherContext<T: Copy> {
    patch_info: Option<PatchedFunction>,
    _phantom: PhantomData<T>,
}
impl<T: Copy> PatcherContext<T> {
    pub const fn new() -> Self {
        PatcherContext { patch_info: None, _phantom: PhantomData }
    }

    pub unsafe fn patch(&mut self, sym_info: SymbolInfo, target: T) -> Result<()> {
        unsafe {
            // remove an old patch if one existed
            self.patch_info.take();

            // patch the target location
            match sym_info {
                SymbolInfo::PublicNamed(sym, info) => {
                    let dylib_civ = Library::open_self()?;
                    let patch_addr = RelocatedOffset::from_ptr(dylib_civ.symbol(sym)?);
                    let target_addr = RelocatedOffset::from_ptr(runtime_transmute(target));
                    let patch = patch_addr.patch_function(target_addr, info, sym);
                    self.patch_info = Some(patch);
                }
                SymbolInfo::Offset(name, offset, info) => {
                    let offset = offset.relocate();
                    let patch = offset.patch_function(runtime_transmute(target), info, name);
                    self.patch_info = Some(patch);
                }
                SymbolInfo::NotAvailable => bail!("Symbol not available in this version."),
            }
        }

        Ok(())
    }

    pub unsafe fn as_func(&self) -> T {
        runtime_transmute(self.patch_info.as_ref().unwrap().old_function())
    }

    pub fn unpatch(&mut self) {
        self.patch_info.take();
    }
}

pub unsafe fn get_symbol<T: Copy>(sym_info: SymbolInfo) -> Result<T> {
    match sym_info {
        SymbolInfo::PublicNamed(sym, _) => {
            let dylib_civ = Library::open_self()?;
            let sym_addr = RelocatedOffset::from_ptr(dylib_civ.symbol(sym)?);
            Ok(runtime_transmute(sym_addr))
        }
        SymbolInfo::Offset(_, offset, _) => {
            let sym_addr = offset.relocate();
            Ok(runtime_transmute(sym_addr))
        }
        SymbolInfo::NotAvailable => bail!("Symbol not available in this version."),
    }
}
