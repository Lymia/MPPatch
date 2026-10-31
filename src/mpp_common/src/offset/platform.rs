//! Low-level platform-specific functions

#[cfg(windows)]
mod platform_impl {
    use libc::*;
    use std::ptr::null_mut;
    use winapi::um::{
        errhandlingapi::GetLastError,
        memoryapi::{VirtualAlloc, VirtualFree, VirtualProtect},
        winnt::{MEM_COMMIT, MEM_RELEASE, PAGE_EXECUTE_READWRITE},
    };

    unsafe fn virtual_protect(start: *mut c_void, len: usize, new: u32) -> ProtectionInfo {
        let mut old = 0;
        unsafe {
            if VirtualProtect(start, len, new, &mut old) == 0 {
                panic!("VirtualProtect failed: 0x{:08x}", GetLastError());
            }
        }
        old
    }

    pub type ProtectionInfo = u32;
    pub unsafe fn unprotect_region(start: *mut c_void, len: usize) -> ProtectionInfo {
        unsafe { virtual_protect(start, len, PAGE_EXECUTE_READWRITE) }
    }
    pub unsafe fn reprotect_region(start: *mut c_void, len: usize, old: ProtectionInfo) {
        unsafe {
            virtual_protect(start, len, old);
        }
    }

    pub unsafe fn exec_mem_mmap(len: usize) -> *mut u8 {
        unsafe {
            let ptr = VirtualAlloc(null_mut(), len, MEM_COMMIT, PAGE_EXECUTE_READWRITE);
            if ptr.is_null() {
                panic!("VirtualAlloc failed: 0x{:08x}", GetLastError())
            }
            ptr as *mut u8
        }
    }
    pub unsafe fn exec_mem_prepare(_: *mut u8, _: usize) {
        // do nothing
    }
    pub unsafe fn exec_mem_munmap(ptr: *mut u8, _: usize) {
        unsafe {
            if VirtualFree(ptr as *mut c_void, 0, MEM_RELEASE) == 0 {
                panic!("VirtualFree failed: 0x{:08x}", GetLastError());
            }
        }
    }
}

#[cfg(unix)]
mod platform_impl {
    use libc::*;
    use std::ptr::null_mut;

    unsafe fn page_size() -> usize {
        unsafe { sysconf(_SC_PAGESIZE) as usize }
    }
    unsafe fn protect_range_impl(start: usize, len: usize, flags: c_int) {
        unsafe {
            let page_size = page_size();
            let end = start + len;
            let start = (start / page_size) * page_size;
            let end = ((end + page_size) / page_size) * page_size;
            if mprotect(start as *mut c_void, end - start, flags) != 0 {
                panic!("mprotect failed")
            }
        }
    }

    pub type ProtectionInfo = ();
    pub unsafe fn unprotect_region(start: *mut c_void, len: usize) -> ProtectionInfo {
        unsafe {
            protect_range_impl(start as usize, len, PROT_READ | PROT_WRITE);
        }
    }
    pub unsafe fn reprotect_region(start: *mut c_void, len: usize, _: ProtectionInfo) {
        unsafe {
            protect_range_impl(start as usize, len, PROT_READ | PROT_EXEC);
        }
    }

    pub unsafe fn exec_mem_mmap(len: usize) -> *mut u8 {
        let ptr = unsafe {
            mmap(null_mut(), len, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0)
        };
        if ptr == MAP_FAILED {
            panic!("mmap failed")
        }
        ptr as *mut u8
    }
    pub unsafe fn exec_mem_prepare(ptr: *mut u8, len: usize) {
        unsafe {
            protect_range_impl(ptr as usize, len, PROT_READ | PROT_EXEC);
        }
    }
    pub unsafe fn exec_mem_munmap(ptr: *mut u8, len: usize) {
        if unsafe { munmap(ptr as *mut c_void, len) } != 0 {
            panic!("munmap failed");
        }
    }
}

#[derive(Debug)]
pub struct ExecutableMemory {
    len: usize,
    is_prepared: bool,
    data: *mut u8,
}
impl ExecutableMemory {
    pub unsafe fn alloc(len: usize) -> Self {
        let new_alloc = unsafe { platform_impl::exec_mem_mmap(len) };
        ExecutableMemory { len, is_prepared: false, data: new_alloc }
    }

    pub fn prepare(&mut self) {
        assert!(!self.is_prepared);
        unsafe {
            platform_impl::exec_mem_prepare(self.data, self.len);
        }
        self.is_prepared = true;
    }

    pub fn ptr(&self) -> *mut u8 {
        self.data
    }
}
impl Drop for ExecutableMemory {
    fn drop(&mut self) {
        unsafe { platform_impl::exec_mem_munmap(self.data, self.len) }
    }
}

pub use platform_impl::{reprotect_region, unprotect_region};
