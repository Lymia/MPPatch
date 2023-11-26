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

#![allow(non_snake_case)]

use crate::{
    rt_cpplist::{CppList, CppListRaw},
    rt_init::MppatchCtx,
    rt_linking::PatcherContext,
};
use anyhow::Result;
use enumset::*;
use libc::c_int;
use log::debug;
use std::{
    ffi::{c_void, CStr},
    fmt::{Debug, Formatter},
    mem,
    sync::Mutex,
};

type FnType = unsafe extern "C" fn(
    *mut c_void,
    *mut CppListRaw<Guid>,
    *mut CppListRaw<ModInfo>,
    bool,
    bool,
) -> c_int;
static SET_ACTIVE_DLC_AND_MODS: Mutex<PatcherContext<FnType>> = Mutex::new(PatcherContext::new());

pub unsafe fn SetActiveDLCAndMods(
    this: *mut c_void,
    dlc_list: *mut CppListRaw<Guid>,
    mod_list: *mut CppListRaw<ModInfo>,
    reload_dlc: bool,
    reload_mods: bool,
) -> c_int {
    let func = SET_ACTIVE_DLC_AND_MODS.lock().unwrap().as_func();
    func(this, dlc_list, mod_list, reload_dlc, reload_mods)
}

#[ctor::dtor]
fn destroy_set_dlc() {
    SET_ACTIVE_DLC_AND_MODS.lock().unwrap().unpatch();
}

#[cfg(windows)]
mod platform_impl {
    use super::*;
    use crate::versions::VersionInfoLinux;
    use dlopen::raw::Library;

    pub fn init(ctx: &MppatchCtx) -> Result<()> {
        // TODO: init
        Ok(())
    }

    pub fn install() {
        // TODO: solve CEG
    }
    pub fn uninstall() {
        // TODO: solve CEG
    }
}

#[cfg(unix)]
mod platform_impl {
    use super::*;
    use crate::versions::VersionInfoLinux;

    pub fn init(ctx: &MppatchCtx) -> Result<()> {
        log::info!("Applying SetActiveDLCAndMods patch...");
        let linux_info: VersionInfoLinux = ctx.info_linux()?;
        unsafe {
            SET_ACTIVE_DLC_AND_MODS
                .lock()
                .unwrap()
                .patch_exe_sym(linux_info.sym_SetActiveDLCAndMods, SetActiveDLCAndModsProxy)?;
        }
        Ok(())
    }

    pub fn install() {
        // no CEG on unix, so we don't need this mechanism
    }
    pub fn uninstall() {
        // no CEG on unix, so we don't need this mechanism
    }
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
    NetPatchState { dlc_list: Vec::new(), mod_list: Vec::new(), overrides: EnumSet::EMPTY };
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

pub fn install() {
    platform_impl::install();
}
pub fn reset() {
    let mut lock = STATE.lock().unwrap();
    platform_impl::uninstall();
    lock.reset();
}

pub unsafe extern "C" fn SetActiveDLCAndModsProxy(
    this: *mut c_void,
    dlc_list: *mut CppListRaw<Guid>,
    mod_list: *mut CppListRaw<ModInfo>,
    reload_dlc: bool,
    reload_mods: bool,
) -> c_int {
    let state = {
        let mut lock = STATE.lock().unwrap();
        platform_impl::uninstall();
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

pub fn init(ctx: &MppatchCtx) -> Result<()> {
    platform_impl::init(ctx)
}
