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

use crate::rt_patch::PatchedFunction;
use anyhow::Result;
use dlopen::raw::Library;
use std::{any::type_name, ffi::c_void, marker::PhantomData, mem::size_of, ptr};

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

    pub unsafe fn patch_exe_sym(
        &mut self,
        (sym, sym_size): (&str, usize),
        target: T,
    ) -> Result<()> {
        unsafe {
            let dylib_civ = Library::open_self()?;
            let patch = PatchedFunction::create(
                dylib_civ.symbol(sym)?,
                runtime_transmute::<T, *const c_void>(target),
                sym_size,
                sym,
            );
            self.patch_info = Some(patch);
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
