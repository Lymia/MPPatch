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

use std::{
    ffi::{c_int, c_void},
    fmt::{Debug, Formatter},
    marker::PhantomData,
    mem,
};

#[cfg(windows)]
#[repr(C)]
struct CppListLink {
    next: *mut CppListLink,
    prev: *mut CppListLink,
    data: (),
}

#[cfg(unix)]
#[repr(C)]
struct CppListLink {
    prev: *mut CppListLink,
    next: *mut CppListLink,
    data: (),
}

impl CppListLink {
    unsafe fn new<T>(t: T) -> *mut Self {
        let list = libc::malloc(mem::size_of::<Self>() + mem::size_of::<T>()) as *mut Self;
        (*list).prev = list;
        (*list).next = list;
        std::ptr::write(&mut (*list).data as *mut _ as *mut T, t);
        list
    }

    unsafe fn data<T>(this: *mut Self) -> *mut T {
        &mut (*this).data as *mut _ as *mut T
    }

    // originally: CppListLink_newLink
    unsafe fn append_before_link(this: *mut Self, list: *mut Self) {
        (*this).prev = (*list).prev;
        (*this).next = list;

        (*(*list).prev).next = this;
        (*list).prev = this;
    }

    unsafe fn clear(this: *mut Self) {
        let mut link = (*this).next;
        while link != this {
            let next_link = (*link).next;
            libc::free(link as *mut c_void);
            link = next_link;
        }

        (*this).prev = this;
        (*this).next = this;
    }

    unsafe fn free(this: *mut Self) {
        Self::clear(this);
        libc::free(this as *mut c_void);
    }
}

#[cfg(windows)]
#[repr(C)]
pub struct CppListRaw<T> {
    unk0: u32, // refcount?
    head: *mut CppListLink,
    length: c_int,
}

#[cfg(unix)]
#[repr(transparent)]
pub struct CppListRaw<T>(CppListLink, PhantomData<T>);

impl<T> CppListRaw<T> {
    #[cfg(windows)]
    unsafe fn len_ptr(this: *mut Self) -> *mut c_int {
        &mut (*this).length
    }

    #[cfg(unix)]
    unsafe fn len_ptr(this: *mut Self) -> *mut c_int {
        CppListLink::data(&mut (*this).0)
    }

    #[cfg(windows)]
    unsafe fn root(this: *mut Self) -> *mut CppListLink {
        (*this).head
    }

    #[cfg(unix)]
    unsafe fn root(this: *mut Self) -> *mut CppListLink {
        mem::transmute(this)
    }

    #[cfg(windows)]
    unsafe fn alloc() -> *mut Self {
        let alloc: *mut Self = libc::malloc(mem::size_of::<Self>());
        (*alloc).unk0 = 0;
        (*alloc).head = CppListLink::new::<()>(());
        (*alloc).length = 0;
        alloc
    }

    #[cfg(unix)]
    unsafe fn alloc() -> *mut Self {
        let alloc: *mut Self = mem::transmute(CppListLink::new::<c_int>(0));
        *Self::len_ptr(alloc) = 0;
        alloc
    }

    #[cfg(windows)]
    unsafe fn free(this: *mut Self) {
        CppListLink::free((*this).head);
        libc::free(this);
    }

    #[cfg(unix)]
    unsafe fn free(this: *mut Self) {
        CppListLink::free(mem::transmute(this));
    }

    unsafe fn begin(this: *mut Self) -> *mut CppListLink {
        (*Self::root(this)).next
    }
    unsafe fn end(this: *mut Self) -> *mut CppListLink {
        (*Self::root(this)).prev
    }
    unsafe fn push(this: *mut Self, t: T) {
        *Self::len_ptr(this) += 1;
        CppListLink::append_before_link(CppListLink::new(t), Self::root(this))
    }
}

pub struct CppList<T> {
    raw: *mut CppListRaw<T>,
    /// VERY bad idea, but this code is very unsafe to begin with, so...
    is_owned: bool,
}
impl<T> CppList<T> {
    pub fn new() -> Self {
        CppList { raw: unsafe { CppListRaw::alloc() }, is_owned: true }
    }
    pub unsafe fn from_raw(raw: *mut CppListRaw<T>) -> Self {
        CppList { raw, is_owned: false }
    }
    pub fn as_raw(&self) -> *mut CppListRaw<T> {
        self.raw
    }
    pub fn push(&mut self, value: T) {
        unsafe {
            CppListRaw::push(self.raw, value);
        }
    }
}
impl<T: Debug> Debug for CppList<T> {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        let mut list = f.debug_list();
        unsafe {
            let mut cur = CppListRaw::begin(self.raw);
            let end = CppListRaw::end(self.raw);

            while cur != end {
                list.entry(&*CppListLink::data::<T>(cur));
                cur = (*cur).next;
            }
        }
        list.finish()
    }
}
impl<T> Drop for CppList<T> {
    fn drop(&mut self) {
        if self.is_owned {
            unsafe {
                CppListRaw::free(self.raw);
            }
        }
    }
}
