use std::{
    ffi::c_void,
    sync::atomic::{AtomicUsize, Ordering},
};

mod patcher;
mod platform;

static RELOC_OFFSET: AtomicUsize = AtomicUsize::new(!0);
fn relocate(ptr: usize) -> usize {
    let reloc_offset = RELOC_OFFSET.load(Ordering::Relaxed);
    assert_ne!(reloc_offset, !0);
    ptr.wrapping_add(reloc_offset)
}

pub unsafe fn set_relocation_info(base_offset: usize, relocated_offset: usize) {
    RELOC_OFFSET.store(relocated_offset.wrapping_sub(base_offset), Ordering::Relaxed);
}

#[derive(Copy, Clone, Debug)]
pub struct PatchInfo {
    pub prologue_len: usize,
    pub copy_len: usize,
    pub fixups: &'static [Fixup],
}
impl PatchInfo {
    pub const fn none() -> PatchInfo {
        PatchInfo { prologue_len: 0, copy_len: 0, fixups: &[] }
    }

    pub const NONE: &'static PatchInfo = &PatchInfo::none();

    pub const fn simple(prologue_len: usize) -> PatchInfo {
        PatchInfo { prologue_len, copy_len: prologue_len, fixups: &[] }
    }
}

#[derive(Copy, Clone, Debug)]
pub enum Fixup {
    Set(Register, u32),
    SetOffset(Register, Offset),
}

#[derive(Copy, Clone, Debug)]
pub enum Register {
    Eax,
    Ebx,
    Ecx,
    Edx,
    Esi,
    Edi,
}

#[derive(Copy, Clone, Debug)]
#[repr(transparent)]
pub struct Offset(pub usize);
impl Offset {
    pub unsafe fn relocate(self) -> RelocatedOffset {
        RelocatedOffset(relocate(self.0) as *mut c_void)
    }
}

#[derive(Copy, Clone, Debug)]
#[repr(transparent)]
pub struct RelocatedOffset(*mut c_void);
impl RelocatedOffset {
    pub unsafe fn from_ptr(ptr: *mut c_void) -> Self {
        RelocatedOffset(ptr)
    }

    pub unsafe fn write_jmp(self, target: RelocatedOffset, reason: &str) {
        unsafe { patcher::patch_jmp_instruction(self.0, target.0, reason) }
    }

    pub unsafe fn stub_function_value(self, result: u32) {
        unsafe { patcher::stub_function_value(self.0, result) }
    }

    pub unsafe fn stub_function_offset(self, offset: RelocatedOffset) {
        unsafe { patcher::stub_function_value(self.0, offset.0 as u32) }
    }

    pub unsafe fn stub_function_true(self) {
        unsafe { patcher::stub_function_true(self.0) }
    }

    pub unsafe fn patch_function(
        self,
        target: RelocatedOffset,
        info: &'static PatchInfo,
        reason: &str,
    ) -> PatchedFunction {
        unsafe { PatchedFunction::create(self.0, target.0, info, reason) }
    }
}

pub use patcher::PatchedFunction;
