use crate::offset::{
    Fixup, PatchInfo, Register,
    platform::{ExecutableMemory, reprotect_region, unprotect_region},
};
use log::debug;
use std::{ffi::c_void, ptr};

unsafe fn write_jmp(patch_addr: *mut c_void, to_addr: *const c_void, reason: &str) {
    let offset_diff = (to_addr as i32) - (patch_addr as i32) - 5;
    debug!("Writing JMP ({reason}) - {patch_addr:?} => {to_addr:?} (diff: 0x{offset_diff:08x})");
    unsafe {
        (patch_addr as *mut u8).write_unaligned(0xe9);
        (patch_addr.offset(1) as *mut i32).write_unaligned(offset_diff);
    }
}

pub unsafe fn patch_jmp_instruction(patch_addr: *mut c_void, to_addr: *const c_void, reason: &str) {
    unsafe {
        let old_prot = unprotect_region(patch_addr, 5);
        write_jmp(patch_addr, to_addr, reason);
        reprotect_region(patch_addr, 5, old_prot);
    }
}

unsafe fn write_mov(patch_addr: *mut c_void, register: Register, target: u32) {
    debug!("Writing MOV - {patch_addr:?} => mov {register:?}, 0x{target:x}");
    unsafe {
        (patch_addr as *mut u8).write_unaligned(match register {
            Register::Eax => 0xb8,
            Register::Ebx => 0xbb,
            Register::Ecx => 0xb9,
            Register::Edx => 0xba,
            Register::Esi => 0xbe,
            Register::Edi => 0xbf,
        });
        (patch_addr.offset(1) as *mut u32).write_unaligned(target);
    }
}

pub struct PatchedFunction {
    patch_addr: *mut c_void,
    info: &'static PatchInfo,
    patch_target: ExecutableMemory,
    old_function: Vec<u8>,
}
impl PatchedFunction {
    pub(super) unsafe fn create(
        patch_addr: *mut c_void,
        to_addr: *const c_void,
        info: &'static PatchInfo,
        reason: &str,
    ) -> Self {
        debug!("Proxying function ({reason}) - {patch_addr:?} => {to_addr:?}");

        let binary_len = info.copy_len + 5 + info.fixups.iter().map(fixup_len).sum::<usize>();

        assert_ne!(info.prologue_len, 0, "Function does not have patcher metainfo.");
        unsafe {
            let mut old_function = vec![0u8; info.prologue_len];
            ptr::copy(patch_addr as *const u8, old_function.as_mut_ptr(), info.prologue_len);

            let mut patch_target = ExecutableMemory::alloc(binary_len);
            ptr::copy(patch_addr as *const u8, patch_target.ptr(), info.copy_len);
            let mut patch_idx = patch_target.ptr().offset(info.copy_len as isize) as *mut c_void;
            for fixup in info.fixups {
                match fixup {
                    Fixup::Set(reg, val) => {
                        write_mov(patch_idx, *reg, *val);
                        patch_idx = patch_idx.offset(5);
                    }
                    Fixup::SetOffset(reg, off) => {
                        write_mov(patch_idx, *reg, off.relocate().0 as u32);
                        patch_idx = patch_idx.offset(5);
                    }
                }
            }
            write_jmp(
                patch_idx,
                patch_addr.offset(info.prologue_len as isize),
                &format!("function fragment epilogue for {reason}"),
            );
            patch_target.prepare();

            patch_jmp_instruction(patch_addr, to_addr, &format!("hook for {reason}"));

            PatchedFunction { patch_addr, info, patch_target, old_function }
        }
    }

    pub fn old_function(&self) -> *const c_void {
        self.patch_target.ptr() as *const c_void
    }
}
impl Drop for PatchedFunction {
    fn drop(&mut self) {
        debug!("Unpatching proxied function at {:?}", self.patch_addr);

        unsafe {
            let old_prot = unprotect_region(self.patch_addr, self.info.prologue_len);
            ptr::copy(
                self.old_function.as_ptr(),
                self.patch_addr as *mut u8,
                self.info.prologue_len,
            );
            reprotect_region(self.patch_addr, self.info.prologue_len, old_prot);
        }

        debug!("(done)")
    }
}
unsafe impl Send for PatchedFunction {}
unsafe impl Sync for PatchedFunction {}

fn fixup_len(fixup: &Fixup) -> usize {
    match fixup {
        Fixup::Set(_, _) => 5,
        Fixup::SetOffset(_, _) => 5,
    }
}

pub unsafe fn stub_function_value(patch_addr: *mut c_void, return_value: u32) {
    unsafe {
        debug!("Disabling function @ {patch_addr:?} => 0x{return_value:x}");
        let old_prot = unprotect_region(patch_addr, 6);
        (patch_addr as *mut u8).write_unaligned(0xB8);
        (patch_addr.offset(1) as *mut u32).write_unaligned(return_value);
        (patch_addr.offset(5) as *mut u8).write_unaligned(0xC3);
        reprotect_region(patch_addr, 6, old_prot);
    }
}

pub unsafe fn stub_function_true(patch_addr: *mut c_void) {
    unsafe {
        debug!("Disabling function @ {patch_addr:?} => true");
        let old_prot = unprotect_region(patch_addr, 3);
        (patch_addr as *mut u8).write_unaligned(0xB0);
        (patch_addr.offset(1) as *mut u8).write_unaligned(1);
        (patch_addr.offset(2) as *mut u8).write_unaligned(0xC3);
        reprotect_region(patch_addr, 3, old_prot);
    }
}
