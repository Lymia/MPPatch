#![allow(non_snake_case)]

use anyhow::*;
use std::fmt::{Debug, Formatter};

#[derive(Copy, Clone, Debug, Eq, PartialEq)]
pub enum Platform {
    Win32,
    #[allow(dead_code)]
    MacOS,
    Linux,
}
impl Platform {
    #[cfg(windows)]
    pub const CURRENT: Platform = Platform::Win32;

    #[cfg(target_os = "macos")]
    pub const CURRENT: Platform = Platform::MacOS;

    #[cfg(target_os = "linux")]
    pub const CURRENT: Platform = Platform::Linux;

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
    pub binary_base: usize,
    pub ceg_patches: Option<CegPatches>,
}

#[derive(Copy, Clone, Debug)]
pub enum ProxySource {
    CvGameDatabase,
}

#[derive(Copy, Clone, Debug)]
pub enum SymbolInfo {
    DllProxy(ProxySource, &'static str),
    PublicNamed(&'static str, usize),
    Offset(&'static str, usize, usize),
}

#[derive(Copy, Clone)]
pub struct CegPatches {
    pub stub_functions: &'static [(usize, u32)],
}
impl Debug for CegPatches {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(f, "CegDefinitions({} patches)", self.stub_functions.len())
    }
}

pub fn find_info(sha256: &str, text_hash: &str) -> Result<VersionInfo> {
    Ok(match sha256 {
        "f95637398ce10012c785b0dc952686db82613f702a8511bbc7ac822896949563" => match text_hash {
            "612537ceaf20715e1ca2968b3ac9433db8789214e198c714ad6ec6d28928be0f" => VersionInfo {
                name: "Civilization V / 1.0.3.279 / Win32 (DX9) + Steam",
                platform: Platform::Win32,
                sym_lGetMemoryUsage: SymbolInfo::DllProxy(ProxySource::CvGameDatabase, "?lGetMemoryUsage@Lua@Scripting@Database@@SAHPAUlua_State@@@Z"),
                sym_SetActiveDLCAndMods: SymbolInfo::Offset("SetActiveDLCAndMods", 0x006CD160, 6),
                binary_base: 0x00400000,
                ceg_patches: Some(include!("data/v1.0.3.279_win32_dx9_ceg.inc")),
            },
            "7c3ee1cabdfaf5242f74521bff3ead1faf72cc4769fcea0267113ed2621e7bca" => VersionInfo {
                name: "Civilization V / 1.0.3.279 / Win32 (DX11) + Steam",
                platform: Platform::Win32,
                sym_lGetMemoryUsage: SymbolInfo::DllProxy(ProxySource::CvGameDatabase, "?lGetMemoryUsage@Lua@Scripting@Database@@SAHPAUlua_State@@@Z"),
                sym_SetActiveDLCAndMods: SymbolInfo::Offset("SetActiveDLCAndMods", 0x006B8E50, 6),
                binary_base: 0x00400000,
                ceg_patches: Some(include!("data/v1.0.3.279_win32_dx11_ceg.inc")),
            },
            "191653e9d79ee22fe293f2d8a73f3ff918e713845ef8a9269cbff995e599a3b7" => VersionInfo {
                name: "Civilization V / 1.0.3.279 / Win32 (Tablet) + Steam",
                platform: Platform::Win32,
                sym_lGetMemoryUsage: SymbolInfo::DllProxy(ProxySource::CvGameDatabase, "?lGetMemoryUsage@Lua@Scripting@Database@@SAHPAUlua_State@@@Z"),
                sym_SetActiveDLCAndMods: SymbolInfo::Offset("SetActiveDLCAndMods", 0x0065DC10, 6),
                binary_base: 0x00400000,
                ceg_patches: Some(include!("data/v1.0.3.279_win32_tablet_ceg.inc")),
            },
            _ => bail!("Unknown .text hash for {sha256}: {text_hash}"),
        },
        "cc06b647821ec5e7cca3c397f6b0d4726f0106cdd67bcf074d494bea2607a8ca" => VersionInfo {
            name: "Civilization V / 1.0.3.279 / Linux + Steam",
            platform: Platform::Linux,
            sym_lGetMemoryUsage: SymbolInfo::PublicNamed("_ZN8Database9Scripting3Lua15lGetMemoryUsageEP9lua_State", 7),
            sym_SetActiveDLCAndMods: SymbolInfo::PublicNamed("_ZN25CvModdingFrameworkAppSide19SetActiveDLCandModsERK22cvContentPackageIDListRKNSt3__14listIN15ModAssociations7ModInfoENS3_9allocatorIS6_EEEEbb", 10),
            binary_base: 0x08048000,
            ceg_patches: None,
        },
        _ => bail!("Unknown version: {sha256}"),
    })
}
