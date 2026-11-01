#![allow(non_snake_case)]

use crate::{
    context::MppatchFeature,
    cpplist::{CppList, CppListRaw},
    patcher::PatcherContext,
};
use enumset::*;
use libc::c_int;
use log::debug;
use mpp_common::logging::check_error;
use std::{
    ffi::{c_void, CStr},
    fmt::{Debug, Formatter},
    mem,
    sync::Mutex,
};

make_proxy_ty! {
    proxy, SetActiveDLCAndMods, SetActiveDLCAndModsProxy_impl, sym_SetActiveDLCAndMods,
    Some(MppatchFeature::Multiplayer), (linux_repr: "thiscall-unwind", win_repr: "C-unwind"),
    (
        this: *mut c_void,
        dlc_list: *mut CppListRaw<Guid>,
        mod_list: *mut CppListRaw<ModInfo>,
        reload_dlc: bool,
        reload_mods: bool,
    ) -> c_int
}

#[derive(Debug)]
struct NetPatchState {
    dlc_list: Vec<Guid>,
    mod_list: Vec<ModInfo>,
    overrides: EnumSet<OverrideType>,
}
impl NetPatchState {
    fn take(&mut self) -> NetPatchState {
        mem::replace(self, DEFAULT_PATCH_STATE)
    }
    fn reset(&mut self) {
        self.take();
    }
}

const DEFAULT_PATCH_STATE: NetPatchState =
    NetPatchState { dlc_list: Vec::new(), mod_list: Vec::new(), overrides: EnumSet::empty() };
static STATE: Mutex<NetPatchState> = Mutex::new(DEFAULT_PATCH_STATE);

#[derive(EnumSetType, Debug)]
pub enum OverrideType {
    OverrideDlcs,
    OverrideMods,
    ForceReloadDlcs,
    ForceReloadMods,
}

pub fn add_dlc(_str: &str) {
    unimplemented!("add_guid")
}
pub fn add_mod(mod_id: &str, version: i32) {
    let mut info = ModInfo { mod_id: [0; 64], version };
    let copy_len = mod_id.len().min(63);
    info.mod_id[..copy_len].copy_from_slice(&mod_id.as_bytes()[..copy_len]);
    (*STATE.lock().unwrap()).mod_list.push(info);
}
pub fn add_override(ty: OverrideType) {
    (*STATE.lock().unwrap()).overrides |= ty;
}

#[derive(Copy, Clone)]
#[repr(C)]
pub struct Guid {
    data1: u32,
    data2: u16,
    data3: u16,
    data4: u64,
}
impl Debug for Guid {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(
            f,
            "{:08x}-{:04x}-{:04x}-{:04x}-{:04x}{:08x}",
            self.data1,
            self.data2,
            self.data3,
            (self.data4 >> 48) & 0xFFFF,
            (self.data4 >> 32) & 0xFFFF,
            self.data4 as u32,
        )
    }
}

#[derive(Copy, Clone)]
#[repr(C)]
pub struct ModInfo {
    mod_id: [u8; 64],
    version: i32,
}
impl Debug for ModInfo {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(
            f,
            "{} v{}",
            CStr::from_bytes_until_nul(&self.mod_id)
                .unwrap()
                .to_string_lossy(),
            self.version
        )
    }
}

pub fn reset() {
    let mut lock = STATE.lock().unwrap();
    lock.reset();
}

unsafe fn SetActiveDLCAndModsProxy_impl(
    this: *mut c_void,
    dlc_list: *mut CppListRaw<Guid>,
    mod_list: *mut CppListRaw<ModInfo>,
    reload_dlc: bool,
    reload_mods: bool,
) -> c_int {
    let state = {
        let mut lock = STATE.lock().unwrap();
        lock.take()
    };

    debug!("[SetActiveDLCAndModsProxy call begin]");
    debug!("dlc_list = {:#?}", CppList::from_raw(dlc_list));
    debug!("mod_list = {:#?}", CppList::from_raw(mod_list));
    debug!("reload_dlc = {}", reload_dlc);
    debug!("reload_mods = {}", reload_mods);
    debug!("state = {:#?}", state);

    let dlc_list = if state.overrides.contains(OverrideType::OverrideDlcs) {
        let mut list = CppList::new();
        for dlc in state.dlc_list {
            list.push(dlc);
        }
        list
    } else {
        CppList::from_raw(dlc_list)
    };
    let mod_list = if state.overrides.contains(OverrideType::OverrideMods) {
        let mut list = CppList::new();
        for entry in state.mod_list {
            list.push(entry);
        }
        list
    } else {
        CppList::from_raw(mod_list)
    };
    let reload_dlc = reload_dlc | state.overrides.contains(OverrideType::ForceReloadDlcs);
    let reload_mods = reload_mods | state.overrides.contains(OverrideType::ForceReloadMods);

    let result =
        SetActiveDLCAndMods(this, dlc_list.as_raw(), mod_list.as_raw(), reload_dlc, reload_mods);

    debug!("[SetActiveDLCAndModsProxy call end]");

    result
}
