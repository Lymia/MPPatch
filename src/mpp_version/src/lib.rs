#![allow(non_snake_case)]

use errcode::prelude::*;
use mpp_common::offset::{Fixup, Offset, PatchInfo, Register};
use sha2::{Digest, Sha256};
use std::{
    fmt::{Debug, Formatter},
    path::Path,
};

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

macro_rules! include_out {
    ($frag:literal) => {
        include!(concat!(env!("OUT_DIR"), "/", $frag))
    };
}

pub fn sha256(data: &[u8]) -> String {
    let mut out = String::new();
    for byte in Sha256::digest(data) {
        out.push_str(&format!("{byte:02x}"));
    }
    out
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct ExeHash {
    bin_hash: String,
    text_hash: String,
}
impl ExeHash {
    pub fn hash(path: &Path) -> Result<ExeHash> {
        let data = std::fs::read(path)?;
        let bin_hash = sha256(&data);
        if data.len() > 2 && &data[..2] == b"MZ" {
            let pe = pe_parser::pe::parse_portable_executable(&data)?;
            for section in pe.section_table {
                if section.name == *b".text\0\0\0" {
                    let start = section.pointer_to_raw_data as usize;
                    let len = section.size_of_raw_data.min(section.virtual_size) as usize;
                    let segment = &data[start..start + len];

                    let text_hash = sha256(segment);
                    return Ok(ExeHash { bin_hash, text_hash });
                }
            }
            bail!("Could not find .text section!")
        } else {
            let text_hash = "-".to_string();
            Ok(ExeHash { bin_hash, text_hash })
        }
    }
}

#[derive(Copy, Clone, Debug)]
struct HashInfo {
    bin_hash: &'static str,
    bin_type: HashInfoType,
}

#[derive(Copy, Clone, Debug)]
enum HashInfoType {
    Simple(VersionInfo),
    Subversions(&'static [(&'static str, VersionInfo)]),
    CegSubversions(&'static [(&'static str, VersionInfo)]),
}

struct WindowsOffsets {
    hash: &'static str,
    name: &'static str,
    sym_SetActiveDLCAndMods: usize,
    sym_cvLuaPathLibrary_Register: usize,
    sym_cvLuaUtils_ExposeLibraryToThread: usize,
    binary_base: usize,
    ceg_patches: Option<CegPatches>,
}
const fn win32_1_0_3_279(offsets: WindowsOffsets) -> (&'static str, VersionInfo) {
    (offsets.hash, VersionInfo {
        name: offsets.name,
        platform: Platform::Win32,
        sym_SetActiveDLCAndMods: SymbolInfo::Offset(
            "SetActiveDLCAndMods",
            Offset(offsets.sym_SetActiveDLCAndMods),
            const { &PatchInfo::simple(6) },
        ),
        sym_cvLuaPathLibrary_Register: SymbolInfo::Offset(
            "cvLuaPathLibrary::Register",
            Offset(offsets.sym_cvLuaPathLibrary_Register),
            const { &PatchInfo::simple(8) },
        ),
        sym_cvLuaUtils_ExposeLibraryToThread: SymbolInfo::Offset(
            "cvLuaUtils::ExposeLibraryToThread",
            Offset(offsets.sym_cvLuaUtils_ExposeLibraryToThread),
            PatchInfo::NONE,
        ),
        binary_base: offsets.binary_base,
        ceg_patches: offsets.ceg_patches,
    })
}

static VERSION_DATA: &'static [HashInfo] = &[
    // Civilization V / 1.0.3.279 / Win32 + Steam
    HashInfo {
        bin_hash: "f95637398ce10012c785b0dc952686db82613f702a8511bbc7ac822896949563",
        bin_type: HashInfoType::CegSubversions(&[
            win32_1_0_3_279(WindowsOffsets {
                hash: "612537ceaf20715e1ca2968b3ac9433db8789214e198c714ad6ec6d28928be0f",
                name: "Civilization V / 1.0.3.279 / Win32 (DX9) + Steam",
                sym_SetActiveDLCAndMods: 0x006CD160,
                sym_cvLuaPathLibrary_Register: 0x004f73c0,
                sym_cvLuaUtils_ExposeLibraryToThread: 0x006dfb60,
                binary_base: 0x00400000,
                ceg_patches: Some(include_out!("v1.0.3.279_win32_dx9_ceg.inc")),
            }),
            win32_1_0_3_279(WindowsOffsets {
                hash: "7c3ee1cabdfaf5242f74521bff3ead1faf72cc4769fcea0267113ed2621e7bca",
                name: "Civilization V / 1.0.3.279 / Win32 (DX11) + Steam",
                sym_SetActiveDLCAndMods: 0x006B8E50,
                sym_cvLuaPathLibrary_Register: 0x007301B0,
                sym_cvLuaUtils_ExposeLibraryToThread: 0x005292A0,
                binary_base: 0x00400000,
                ceg_patches: Some(include_out!("v1.0.3.279_win32_dx11_ceg.inc")),
            }),
            win32_1_0_3_279(WindowsOffsets {
                hash: "191653e9d79ee22fe293f2d8a73f3ff918e713845ef8a9269cbff995e599a3b7",
                name: "Civilization V / 1.0.3.279 / Win32 (Tablet) + Steam",
                sym_SetActiveDLCAndMods: 0x0065DC10,
                sym_cvLuaPathLibrary_Register: 0x006df4e0,
                sym_cvLuaUtils_ExposeLibraryToThread: 0x00463150,
                binary_base: 0x00400000,
                ceg_patches: Some(include_out!("v1.0.3.279_win32_tablet_ceg.inc")),
            }),
        ]),
    },
    // Civilization V / 1.0.3.279 / Linux + Steam
    HashInfo {
        bin_hash: "cc06b647821ec5e7cca3c397f6b0d4726f0106cdd67bcf074d494bea2607a8ca",
        bin_type: HashInfoType::Simple(VersionInfo {
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
        }),
    },
    // Civilization V / 1.0.3.279 (Nov 2026 Hotfix) / Win32 + Steam
    HashInfo {
        bin_hash: "e0ed78f7cfd64fbcca179ce1be4006900bddbceec3fd6a2cab059ef8004f178e",
        bin_type: HashInfoType::Subversions(&[
            win32_1_0_3_279(WindowsOffsets {
                hash: "2b9444c88477717a19b3ce91db679869ebb52d1529f00a6849179b3ea90d8be0",
                name: "Civilization V / 1.0.3.279 (Nov 2026 Hotfix) / Win32 (DX9) + Steam",
                sym_SetActiveDLCAndMods: 0x007e3630,
                sym_cvLuaPathLibrary_Register: 0x007a0650,
                sym_cvLuaUtils_ExposeLibraryToThread: 0x007abe30,
                binary_base: 0x00400000,
                ceg_patches: None,
            }),
            win32_1_0_3_279(WindowsOffsets {
                hash: "a7b4f52048bb5890b1f316852f1c0e12bbd6e5e9685d20b167fbd352196bd586",
                name: "Civilization V / 1.0.3.279 (Nov 2026 Hotfix) / Win32 (DX11) + Steam",
                sym_SetActiveDLCAndMods: 0x007b47c0,
                sym_cvLuaPathLibrary_Register: 0x00771830,
                sym_cvLuaUtils_ExposeLibraryToThread: 0x0077cb70,
                binary_base: 0x00400000,
                ceg_patches: None,
            }),
            win32_1_0_3_279(WindowsOffsets {
                hash: "67e14681d1ec8c2aca49f0f48b54f8d3ed49e0ff7417a08092f0eed126c648a7",
                name: "Civilization V / 1.0.3.279 (Nov 2026 Hotfix) / Win32 (Tablet) + Steam",
                sym_SetActiveDLCAndMods: 0x007b64a0,
                sym_cvLuaPathLibrary_Register: 0x00773150,
                sym_cvLuaUtils_ExposeLibraryToThread: 0x0077e850,
                binary_base: 0x00400000,
                ceg_patches: None,
            }),
        ]),
    },
];

pub fn find_info(sha256: &str, exe_hash: &ExeHash) -> Result<&'static VersionInfo> {
    let Some(version) = VERSION_DATA.iter().find(|x| x.bin_hash == sha256) else {
        bail!("Cannot find version matching main hash: {sha256}")
    };
    match &version.bin_type {
        HashInfoType::Simple(info) => Ok(info),
        HashInfoType::Subversions(subversions) => Ok(&subversions
            .iter()
            .find(|(x, _)| *x == exe_hash.bin_hash)
            .convert(error_info!("Cannot find subversion matching hash: {}", exe_hash.bin_hash))?
            .1),
        HashInfoType::CegSubversions(subversions) => Ok(&subversions
            .iter()
            .find(|(x, _)| *x == exe_hash.text_hash)
            .convert(error_info!("Cannot find subversion matching .text: {}", exe_hash.text_hash))?
            .1),
    }
}
