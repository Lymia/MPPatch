use std::{
    ffi::c_void,
    sync::atomic::{AtomicUsize, Ordering},
};

mod platform;
mod writer;

static RELOC_OFFSET: AtomicUsize = AtomicUsize::new(!0);
fn relocate(ptr: usize) -> usize {
    let reloc_offset = RELOC_OFFSET.load(Ordering::Relaxed);
    assert_ne!(reloc_offset, !0);
    ptr.wrapping_add(ptr)
}

pub unsafe fn set_relocation_info(base_offset: usize, relocated_offset: usize) {
    RELOC_OFFSET.store(relocated_offset.wrapping_sub(base_offset), Ordering::Relaxed);
}

#[derive(Copy, Clone, Debug)]
pub struct Offset(pub usize);
impl Offset {
    pub unsafe fn relocate(self) -> RelocatedOffset {
        RelocatedOffset(relocate(self.0) as *mut c_void)
    }
}

#[derive(Copy, Clone, Debug)]
pub struct RelocatedOffset(*mut c_void);
impl RelocatedOffset {
    pub unsafe fn from_ptr(ptr: *mut c_void) -> Self {
        RelocatedOffset(ptr)
    }

    pub unsafe fn write_jmp(self, target: RelocatedOffset, reason: &str) {
        unsafe { writer::patch_jmp_instruction(self.0, target.0, reason) }
    }

    pub unsafe fn stub_function_value(self, result: u32) {
        unsafe { writer::stub_function_value(self.0, result) }
    }

    pub unsafe fn stub_function_offset(self, offset: RelocatedOffset) {
        unsafe { writer::stub_function_value(self.0, offset.0 as u32) }
    }

    pub unsafe fn stub_function_true(self) {
        unsafe { writer::stub_function_true(self.0) }
    }

    pub unsafe fn patch_function(
        self,
        target: RelocatedOffset,
        prologue_len: usize,
        reason: &str,
    ) -> PatchedFunction {
        unsafe { PatchedFunction::create(self.0, target.0, prologue_len, reason) }
    }
}

pub use writer::PatchedFunction;
