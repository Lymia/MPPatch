use crate::rt_platform::{reprotect_region, unprotect_region, ExecutableMemory};
use log::debug;
use std::{ffi::c_void, ptr};

unsafe fn write_jmp(patch_addr: *mut c_void, to_addr: *const c_void, reason: &str) {
    let offset_diff = (to_addr as i32) - (patch_addr as i32) - 5;
    debug!("Writing JMP ({reason}) - {patch_addr:?} => {to_addr:?} (diff: 0x{offset_diff:08x})");
    (patch_addr as *mut u8).write_unaligned(0xe9);
    (patch_addr.offset(1) as *mut i32).write_unaligned(offset_diff);
}

pub unsafe fn patch_jmp_instruction(patch_addr: *mut c_void, to_addr: *const c_void, reason: &str) {
    let old_prot = unprotect_region(patch_addr, 5);
    write_jmp(patch_addr, to_addr, reason);
    reprotect_region(patch_addr, 5, old_prot);
}

pub struct PatchedFunction {
    patch_addr: *mut c_void,
    patch_bytes: usize,
    patch_target: ExecutableMemory,
}
impl PatchedFunction {
    pub unsafe fn create(
        patch_addr: *mut c_void,
        to_addr: *const c_void,
        patch_bytes: usize,
        reason: &str,
    ) -> Self {
        debug!(
            "Proxying function ({reason}) - {patch_addr:?} => {to_addr:?} ({patch_bytes} bytes)"
        );

        let mut patch_target = ExecutableMemory::alloc(patch_bytes + 5);

        ptr::copy(patch_addr as *const u8, patch_target.ptr(), patch_bytes);
        write_jmp(
            patch_target.ptr().offset(patch_bytes as isize) as *mut c_void,
            patch_addr.offset(patch_bytes as isize),
            &format!("function fragment epilogue for {reason}"),
        );
        patch_jmp_instruction(patch_addr, to_addr, &format!("hook for {reason}"));

        patch_target.prepare();
        PatchedFunction { patch_addr, patch_bytes, patch_target }
    }

    pub fn old_function(&self) -> *const c_void {
        self.patch_target.ptr() as *const c_void
    }
}
impl Drop for PatchedFunction {
    fn drop(&mut self) {
        debug!("Unpatching proxied function at {:?}", self.patch_addr);

        unsafe {
            let old_prot = unprotect_region(self.patch_addr, self.patch_bytes);
            ptr::copy(self.patch_target.ptr(), self.patch_addr as *mut u8, self.patch_bytes);
            reprotect_region(self.patch_addr, self.patch_bytes, old_prot);
        }

        debug!("(done)")
    }
}
unsafe impl Send for PatchedFunction {}
unsafe impl Sync for PatchedFunction {}

pub unsafe fn replace_function(patch_addr: *mut c_void, return_value: u32) {
    debug!("Replacing protected function @ {patch_addr:?} => 0x{return_value:x}");
    let old_prot = unprotect_region(patch_addr, 6);
    (patch_addr as *mut u8).write_unaligned(0xB8);
    (patch_addr.offset(1) as *mut u32).write_unaligned(return_value);
    (patch_addr.offset(5) as *mut u8).write_unaligned(0xC3);
    reprotect_region(patch_addr, 6, old_prot);
}

pub unsafe fn stub_function(patch_addr: *mut c_void) {
    debug!("Disabling function @ {patch_addr:?}");
    let old_prot = unprotect_region(patch_addr, 3);
    (patch_addr as *mut u8).write_unaligned(0xB0);
    (patch_addr.offset(1) as *mut u8).write_unaligned(1);
    (patch_addr.offset(2) as *mut u8).write_unaligned(0xC3);
    reprotect_region(patch_addr, 3, old_prot);
}
