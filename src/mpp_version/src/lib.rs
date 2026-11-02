#![allow(non_snake_case)]

use errcode::prelude::*;
use mpp_common::offset::{Fixup, Offset, PatchInfo, Register};
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
    pub sym_SetActiveDLCAndMods: SymbolInfo,
    pub sym_cvLuaPathLibrary_Register: SymbolInfo,
    pub sym_cvLuaUtils_ExposeLibraryToThread: SymbolInfo,
    pub binary_base: usize,
    pub ceg_patches: Option<CegPatches>,
}

#[derive(Copy, Clone, Debug)]
pub enum ProxySource {
    CvGameDatabase,
}

#[derive(Copy, Clone, Debug)]
pub enum SymbolInfo {
    PublicNamed(&'static str, &'static PatchInfo),
    Offset(&'static str, Offset, &'static PatchInfo),
    NotAvailable,
}

#[derive(Copy, Clone)]
pub struct CegPatches {
    pub static_functions: &'static [(Offset, u32)],
    pub relative_functions: &'static [(Offset, Offset)],
    pub stub_functions: &'static [Offset],
}
impl Debug for CegPatches {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(
            f,
            "CegDefinitions({} patches)",
            self.static_functions.len() + self.stub_functions.len()
        )
    }
}

pub fn find_info(sha256: &str, text_hash: &str) -> Result<VersionInfo> {
    Ok(match sha256 {
        "f95637398ce10012c785b0dc952686db82613f702a8511bbc7ac822896949563" => match text_hash {
            "612537ceaf20715e1ca2968b3ac9433db8789214e198c714ad6ec6d28928be0f" => VersionInfo {
                name: "Civilization V / 1.0.3.279 / Win32 (DX9) + Steam",
                platform: Platform::Win32,
                sym_SetActiveDLCAndMods: SymbolInfo::Offset(
                    "SetActiveDLCAndMods",
                    Offset(0x006CD160),
                    const { &PatchInfo::simple(6) },
                ),
                sym_cvLuaPathLibrary_Register: SymbolInfo::NotAvailable,
                sym_cvLuaUtils_ExposeLibraryToThread: SymbolInfo::NotAvailable,
                binary_base: 0x00400000,
                ceg_patches: Some(include!("v1.0.3.279_win32_dx9_ceg.inc")),
            },
            "7c3ee1cabdfaf5242f74521bff3ead1faf72cc4769fcea0267113ed2621e7bca" => VersionInfo {
                name: "Civilization V / 1.0.3.279 / Win32 (DX11) + Steam",
                platform: Platform::Win32,
                sym_SetActiveDLCAndMods: SymbolInfo::Offset(
                    "SetActiveDLCAndMods",
                    Offset(0x006B8E50),
                    const { &PatchInfo::simple(6) },
                ),
                sym_cvLuaPathLibrary_Register: SymbolInfo::NotAvailable,
                sym_cvLuaUtils_ExposeLibraryToThread: SymbolInfo::NotAvailable,
                binary_base: 0x00400000,
                ceg_patches: Some(include!("v1.0.3.279_win32_dx11_ceg.inc")),
            },
            "191653e9d79ee22fe293f2d8a73f3ff918e713845ef8a9269cbff995e599a3b7" => VersionInfo {
                name: "Civilization V / 1.0.3.279 / Win32 (Tablet) + Steam",
                platform: Platform::Win32,
                sym_SetActiveDLCAndMods: SymbolInfo::Offset(
                    "SetActiveDLCAndMods",
                    Offset(0x0065DC10),
                    const { &PatchInfo::simple(6) },
                ),
                sym_cvLuaPathLibrary_Register: SymbolInfo::NotAvailable,
                sym_cvLuaUtils_ExposeLibraryToThread: SymbolInfo::NotAvailable,
                binary_base: 0x00400000,
                ceg_patches: Some(include!("v1.0.3.279_win32_tablet_ceg.inc")),
            },
            _ => bail!("Unknown .text hash for {sha256}: {text_hash}"),
        },
        "cc06b647821ec5e7cca3c397f6b0d4726f0106cdd67bcf074d494bea2607a8ca" => VersionInfo {
            name: "Civilization V / 1.0.3.279 / Linux + Steam",
            platform: Platform::Linux,
            sym_SetActiveDLCAndMods: SymbolInfo::PublicNamed(
                "_ZN25CvModdingFrameworkAppSide19SetActiveDLCandModsERK22cvContentPackageIDListRKNSt3__14listIN15ModAssociations7ModInfoENS3_9allocatorIS6_EEEEbb",
                const { &PatchInfo::simple(10) },
            ),
            sym_cvLuaPathLibrary_Register: SymbolInfo::PublicNamed(
                "_ZN16cvLuaPathLibrary8RegisterEP9lua_StateS1_",
                &PatchInfo {
                    prologue_len: 10,
                    copy_len: 4,
                    fixups: &[Fixup::SetOffset(Register::Ebx, Offset(0x8967EE9))],
                },
            ),
            sym_cvLuaUtils_ExposeLibraryToThread: SymbolInfo::PublicNamed(
                "_ZN10cvLuaUtils21ExposeLibraryToThreadEP9lua_StateS1_PKcPFiS1_E",
                PatchInfo::NONE,
            ),
            binary_base: 0x08048000,
            ceg_patches: None,
        },
        _ => bail!("Unknown version: {sha256}"),
    })
}
