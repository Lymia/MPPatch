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

use anyhow::*;

#[derive(Copy, Clone, Debug)]
pub enum VersionInfo {
    Windows(VersionInfoWindows),
    Linux(VersionInfoLinux),
}

#[derive(Copy, Clone, Debug)]
pub struct VersionInfoWindows {
    pub sym_lGetMemoryUsage: &'static str,
    pub sym_SetActiveDLCAndMods_dx9: (usize, usize),
    pub sym_SetActiveDLCAndMods_dx11: (usize, usize),
    pub sym_SetActiveDLCAndMods_tablet: (usize, usize),
    pub binary_base: usize,
}

#[derive(Copy, Clone, Debug)]
pub struct VersionInfoLinux {
    pub sym_lGetMemoryUsage: (&'static str, usize),
    pub sym_SetActiveDLCAndMods: (&'static str, usize),
}

pub fn find_info(sha256: &str) -> Result<VersionInfo> {
    Ok(match sha256 {
        "cc06b647821ec5e7cca3c397f6b0d4726f0106cdd67bcf074d494bea2607a8ca" => VersionInfo::Linux(VersionInfoLinux {
            sym_lGetMemoryUsage: ("_ZN8Database9Scripting3Lua15lGetMemoryUsageEP9lua_State", 7),
            sym_SetActiveDLCAndMods: ("_ZN25CvModdingFrameworkAppSide19SetActiveDLCandModsERK22cvContentPackageIDListRKNSt3__14listIN15ModAssociations7ModInfoENS3_9allocatorIS6_EEEEbb", 10),
        }),
        "f95637398ce10012c785b0dc952686db82613f702a8511bbc7ac822896949563" => VersionInfo::Windows(VersionInfoWindows {
            sym_lGetMemoryUsage: "?lGetMemoryUsage@Lua@Scripting@Database@@SAHPAUlua_State@@@Z",
            sym_SetActiveDLCAndMods_dx9: (0x006CD160, 6),
            sym_SetActiveDLCAndMods_dx11: (0x006B8E50, 6),
            sym_SetActiveDLCAndMods_tablet: (0x0065DC10, 6),
            binary_base: 0x00400000,
        }),
        _ => bail!("Unknown version: {sha256}"),
    })
}
