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
pub enum Platform {
    Win32,
    MacOS,
    Linux,
}
impl Platform {
    pub fn name(&self) -> &'static str {
        match self {
            Platform::Win32 => "win32",
            Platform::MacOS => "macos",
            Platform::Linux => "linux",
        }
    }
}

#[derive(Copy, Clone, Debug)]
pub struct VersionInfo {
    pub name: &'static str,
    pub platform: Platform,
    pub sym_lGetMemoryUsage: SymbolInfo,
    pub sym_SetActiveDLCAndMods: SymbolInfo,
    pub binary_base: Option<usize>,
}

#[derive(Copy, Clone, Debug)]
pub enum ProxySource {
    CvGameDatabase,
}

#[derive(Copy, Clone, Debug)]
pub enum SymbolInfo {
    DllProxy(ProxySource, &'static str),
    PublicNamed(&'static str, usize),
    Win32Offsets(SymWin32Offsets),
}

#[derive(Copy, Clone, Debug)]
pub struct SymWin32Offsets {
    dx9: (usize, usize),
    dx11: (usize, usize),
    tablet: (usize, usize),
}

pub fn find_info(sha256: &str) -> Result<VersionInfo> {
    Ok(match sha256 {
        "f95637398ce10012c785b0dc952686db82613f702a8511bbc7ac822896949563" => VersionInfo {
            name: "Civilization V / 1.0.3.279 / Win32 + Steam",
            platform: Platform::Win32,
            sym_lGetMemoryUsage: SymbolInfo::DllProxy(ProxySource::CvGameDatabase, "?lGetMemoryUsage@Lua@Scripting@Database@@SAHPAUlua_State@@@Z"),
            sym_SetActiveDLCAndMods: SymbolInfo::Win32Offsets(SymWin32Offsets {
                dx9: (0x006CD160, 6),
                dx11: (0x006B8E50, 6),
                tablet: (0x0065DC10, 6),
            }),
            binary_base: Some(0x00400000),
        },
        "cc06b647821ec5e7cca3c397f6b0d4726f0106cdd67bcf074d494bea2607a8ca" => VersionInfo {
            name: "Civilization V / 1.0.3.279 / Linux + Steam",
            platform: Platform::Linux,
            sym_lGetMemoryUsage: SymbolInfo::PublicNamed("_ZN8Database9Scripting3Lua15lGetMemoryUsageEP9lua_State", 7),
            sym_SetActiveDLCAndMods: SymbolInfo::PublicNamed("_ZN25CvModdingFrameworkAppSide19SetActiveDLCandModsERK22cvContentPackageIDListRKNSt3__14listIN15ModAssociations7ModInfoENS3_9allocatorIS6_EEEEbb", 10),
            binary_base: None,
        },
        _ => bail!("Unknown version: {sha256}"),
    })
}
