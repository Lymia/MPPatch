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
#![allow(non_camel_case_types)]

use crate::{rt_init::MppatchCtx, rt_patch};
use anyhow::{bail, Result};
use dlopen::raw::Library;
use log::info;
use std::{ffi::c_void, sync::Mutex};

pub trait ProxyLibrary {
    unsafe fn patch(&self, sym: &str, target: *const c_void) -> Result<*const c_void>;
    unsafe fn unpatch(&self, sym: &str) -> Result<()>;
}

macro_rules! make_proxy {
    (
        $dll_name:literal $dll_name_short:literal
        $load_sym:ident $lib_sym:ident $lib_cache_sym:ident

        $(($sym:ident $target:literal))*
    ) => {
        $(
            #[naked]
            #[export_name = concat!("mppatch_proxy_", $dll_name_short, "_", $target)]
            #[link_section = ".text_proxy"]
            unsafe extern "C" fn $sym(number: usize) -> usize {
                static mut UNIQUE_ADDR: u8 = 0; // ensures the MergeFunctions pass doesn't eat us
                std::arch::asm!(
                    ".long {}\n",
                    ".long 0\n",
                    sym UNIQUE_ADDR,
                    options(noreturn)
                )
            }
        )*

        #[allow(non_upper_case_globals)]
        static $lib_cache_sym: Mutex<Option<Library>> = Mutex::new(None);

        fn $load_sym(ctx: &MppatchCtx) -> Result<()> {
            let mut lib_path = ctx.exe_dir().to_path_buf();
            lib_path.push($dll_name);

            let lib = Library::open(lib_path)?;
            unsafe {
                $(
                    rt_patch::patch_jmp_instruction(
                        $sym as unsafe extern "C" fn(usize) -> usize as *mut c_void,
                        lib.symbol($target)?,
                        $target,
                    );
                )*
            }
            *$lib_cache_sym.lock().unwrap() = Some(lib);

            Ok(())
        }

        #[derive(Copy, Clone, Debug)]
        pub struct $lib_sym;
        impl ProxyLibrary for $lib_sym {
            unsafe fn patch(&self, sym: &str, target: *const c_void) -> Result<*const c_void> {
                let lib = $lib_cache_sym.lock().unwrap();
                let lib = lib.as_ref().unwrap();
                match sym {
                    $($target => {
                        rt_patch::patch_jmp_instruction(
                            $sym as unsafe extern "C" fn(usize) -> usize as *mut c_void,
                            target,
                            $target,
                        );
                        Ok(lib.symbol($target)?)
                    })*
                    _ => bail!("Unknown proxy symbol {sym}"),
                }
            }
            unsafe fn unpatch(&self, sym: &str) -> Result<()> {
                let lib = $lib_cache_sym.lock().unwrap();
                let lib = lib.as_ref().unwrap();
                match sym {
                    $($target => {
                        let target = lib.symbol($target)?;
                        rt_patch::patch_jmp_instruction(
                            $sym as unsafe extern "C" fn(usize) -> usize as *mut c_void,
                            target,
                            $target,
                        );
                    })*
                    _ => bail!("Unknown proxy symbol {sym}"),
                }
                Ok(())
            }
        }
    };
}

make_proxy! {
    "CvGameDatabase_Original.dll" "CvGameDatabase"
    load_CvGameDatabase Proxy_CvGameDatabase LIBRARY_CACHE_CvGameDatabase

    (proxy_CvGameDatabase_0000 "??0BinaryIO@Database@@QAE@PBD@Z")
    (proxy_CvGameDatabase_0001 "??0Connection@Database@@QAE@PBDH@Z")
    (proxy_CvGameDatabase_0002 "??0Connection@Database@@QAE@XZ")
    (proxy_CvGameDatabase_0003 "??0Results@Database@@QAE@ABV01@@Z")
    (proxy_CvGameDatabase_0004 "??0Results@Database@@QAE@PBD@Z")
    (proxy_CvGameDatabase_0005 "??0ResultsCache@Database@@QAE@AAVConnection@1@@Z")
    (proxy_CvGameDatabase_0006 "??0SingleResult@Database@@QAE@PBD@Z")
    (proxy_CvGameDatabase_0007 "??0XMLSerializer@Database@@QAE@AAVConnection@1@@Z")
    (proxy_CvGameDatabase_0008 "??1Connection@Database@@QAE@XZ")
    (proxy_CvGameDatabase_0009 "??1Results@Database@@UAE@XZ")
    (proxy_CvGameDatabase_000a "??1ResultsCache@Database@@QAE@XZ")
    (proxy_CvGameDatabase_000b "??1XMLSerializer@Database@@QAE@XZ")
    (proxy_CvGameDatabase_000c "??2Connection@Database@@SAPAXI@Z")
    (proxy_CvGameDatabase_000d "??2Results@Database@@SAPAXI@Z")
    (proxy_CvGameDatabase_000e "??3Connection@Database@@SAXPAX@Z")
    (proxy_CvGameDatabase_000f "??3Results@Database@@SAXPAX@Z")
    (proxy_CvGameDatabase_0010 "??5BinaryIO@Database@@QAE_NAAVConnection@1@@Z")
    (proxy_CvGameDatabase_0011 "??6BinaryIO@Database@@QAE_NAAVConnection@1@@Z")
    (proxy_CvGameDatabase_0012 "??_FResults@Database@@QAEXXZ")
    (proxy_CvGameDatabase_0013 "??_FSingleResult@Database@@QAEXXZ")
    (proxy_CvGameDatabase_0014 "?AddTableToPackageTagList@XMLSerializer@Database@@QAEXPBD@Z")
    (proxy_CvGameDatabase_0015 "?Analyze@Connection@Database@@QBEXXZ")
    (proxy_CvGameDatabase_0016 "?BeginDeferredTransaction@Connection@Database@@QAEXXZ")
    (proxy_CvGameDatabase_0017 "?BeginExclusiveTransaction@Connection@Database@@QAEXXZ")
    (proxy_CvGameDatabase_0018 "?BeginImmediateTransaction@Connection@Database@@QAEXXZ")
    (proxy_CvGameDatabase_0019 "?BeginTransaction@Connection@Database@@QAEXXZ")
    (proxy_CvGameDatabase_001a "?Bind@Results@Database@@QAE_NHH@Z")
    (proxy_CvGameDatabase_001b "?Bind@Results@Database@@QAE_NHM@Z")
    (proxy_CvGameDatabase_001c "?Bind@Results@Database@@QAE_NHN@Z")
    (proxy_CvGameDatabase_001d "?Bind@Results@Database@@QAE_NHPBDH_N@Z")
    (proxy_CvGameDatabase_001e "?Bind@Results@Database@@QAE_NHPBD_N@Z")
    (proxy_CvGameDatabase_001f "?Bind@Results@Database@@QAE_NHPB_WH_N@Z")
    (proxy_CvGameDatabase_0020 "?Bind@Results@Database@@QAE_NHPB_W_N@Z")
    (proxy_CvGameDatabase_0021 "?Bind@Results@Database@@QAE_NH_J@Z")
    (proxy_CvGameDatabase_0022 "?BindNULL@Results@Database@@QAE_NH@Z")
    (proxy_CvGameDatabase_0023 "?CalculateMemoryStats@Connection@Database@@QAEPBDXZ")
    (proxy_CvGameDatabase_0024 "?Clear@ResultsCache@Database@@QAEXABV?$basic_string@DU?$char_traits@D@std@@V?$allocator@D@2@@std@@@Z")
    (proxy_CvGameDatabase_0025 "?Clear@ResultsCache@Database@@QAEXXZ")
    (proxy_CvGameDatabase_0026 "?ClearCountCache@Connection@Database@@QAEXXZ")
    (proxy_CvGameDatabase_0027 "?ClearPackageTagList@XMLSerializer@Database@@QAEXXZ")
    (proxy_CvGameDatabase_0028 "?Close@Connection@Database@@QAEXXZ")
    (proxy_CvGameDatabase_0029 "?ColumnCount@Results@Database@@QBEHXZ")
    (proxy_CvGameDatabase_002a "?ColumnName@Results@Database@@QAEPBDH@Z")
    (proxy_CvGameDatabase_002b "?ColumnPosition@Results@Database@@QAEHPBD@Z")
    (proxy_CvGameDatabase_002c "?ColumnType@Results@Database@@QAE?AW4ColumnTypes@2@H@Z")
    (proxy_CvGameDatabase_002d "?ColumnTypeName@Results@Database@@QAEPBDH@Z")
    (proxy_CvGameDatabase_002e "?CommitTransaction@Connection@Database@@QAEXXZ")
    (proxy_CvGameDatabase_002f "?Connection@ResultsCache@Database@@QAEAAV02@XZ")
    (proxy_CvGameDatabase_0030 "?Count@Connection@Database@@QAEHPBD_N@Z")
    (proxy_CvGameDatabase_0031 "?EndTransaction@Connection@Database@@QAEXXZ")
    (proxy_CvGameDatabase_0032 "?ErrorCode@Connection@Database@@QBEHXZ")
    (proxy_CvGameDatabase_0033 "?ErrorMessage@Connection@Database@@QBEPBDXZ")
    (proxy_CvGameDatabase_0034 "?ErrorMessage@ResultsCache@Database@@QBEPBDXZ")
    (proxy_CvGameDatabase_0035 "?ErrorMessage@XMLSerializer@Database@@QBEPBDXZ")
    (proxy_CvGameDatabase_0036 "?Execute@Connection@Database@@QBE_NAAVResults@2@PBDH@Z")
    (proxy_CvGameDatabase_0037 "?Execute@Connection@Database@@QBE_NPBDH@Z")
    (proxy_CvGameDatabase_0038 "?Execute@Results@Database@@QAE_NXZ")
    (proxy_CvGameDatabase_0039 "?ExecuteMultiple@Connection@Database@@QBE_NPBDH@Z")
    (proxy_CvGameDatabase_003a "?Get@ResultsCache@Database@@QAEPAVResults@2@ABV?$basic_string@DU?$char_traits@D@std@@V?$allocator@D@2@@std@@@Z")
    (proxy_CvGameDatabase_003b "?GetBool@Results@Database@@QAE_NH@Z")
    (proxy_CvGameDatabase_003c "?GetBool@Results@Database@@QAE_NPBD@Z")
    (proxy_CvGameDatabase_003d "?GetColumns@Results@Database@@UBEPBDXZ")
    (proxy_CvGameDatabase_003e "?GetDouble@Results@Database@@QAENH@Z")
    (proxy_CvGameDatabase_003f "?GetDouble@Results@Database@@QAENPBD@Z")
    (proxy_CvGameDatabase_0040 "?GetFloat@Results@Database@@QAEMH@Z")
    (proxy_CvGameDatabase_0041 "?GetFloat@Results@Database@@QAEMPBD@Z")
    (proxy_CvGameDatabase_0042 "?GetInt64@Results@Database@@QAE_JH@Z")
    (proxy_CvGameDatabase_0043 "?GetInt64@Results@Database@@QAE_JPBD@Z")
    (proxy_CvGameDatabase_0044 "?GetInt@Results@Database@@QAEHH@Z")
    (proxy_CvGameDatabase_0045 "?GetInt@Results@Database@@QAEHPBD@Z")
    (proxy_CvGameDatabase_0046 "?GetText16@Results@Database@@QAEPB_WH@Z")
    (proxy_CvGameDatabase_0047 "?GetText16@Results@Database@@QAEPB_WPBD@Z")
    (proxy_CvGameDatabase_0048 "?GetText@Results@Database@@QAEPBDH@Z")
    (proxy_CvGameDatabase_0049 "?GetText@Results@Database@@QAEPBDPBD@Z")
    (proxy_CvGameDatabase_004a "?GetValue@Results@Database@@QAEXHAAH@Z")
    (proxy_CvGameDatabase_004b "?GetValue@Results@Database@@QAEXHAAM@Z")
    (proxy_CvGameDatabase_004c "?GetValue@Results@Database@@QAEXHAAN@Z")
    (proxy_CvGameDatabase_004d "?GetValue@Results@Database@@QAEXHAAPBD@Z")
    (proxy_CvGameDatabase_004e "?GetValue@Results@Database@@QAEXHAAPB_W@Z")
    (proxy_CvGameDatabase_004f "?GetValue@Results@Database@@QAEXHAA_J@Z")
    (proxy_CvGameDatabase_0050 "?GetValue@Results@Database@@QAEXHAA_N@Z")
    (proxy_CvGameDatabase_0051 "?GetValue@Results@Database@@QAEXPBDAAH@Z")
    (proxy_CvGameDatabase_0052 "?GetValue@Results@Database@@QAEXPBDAAM@Z")
    (proxy_CvGameDatabase_0053 "?GetValue@Results@Database@@QAEXPBDAAN@Z")
    (proxy_CvGameDatabase_0054 "?GetValue@Results@Database@@QAEXPBDAAPBD@Z")
    (proxy_CvGameDatabase_0055 "?GetValue@Results@Database@@QAEXPBDAAPB_W@Z")
    (proxy_CvGameDatabase_0056 "?GetValue@Results@Database@@QAEXPBDAA_J@Z")
    (proxy_CvGameDatabase_0057 "?GetValue@Results@Database@@QAEXPBDAA_N@Z")
    (proxy_CvGameDatabase_0058 "?HasColumn@Results@Database@@QAE_NPBD@Z")
    (proxy_CvGameDatabase_0059 "?HashColumnPositions@Results@Database@@IAEXXZ")
    (proxy_CvGameDatabase_005a "?Load@BinaryIO@Database@@QAE_NAAVConnection@2@@Z")
    (proxy_CvGameDatabase_005b "?Load@XMLSerializer@Database@@QAE_NPB_W@Z")
    (proxy_CvGameDatabase_005c "?Load@XMLSerializer@Database@@QAE_NPB_WAAVResultsCache@2@@Z")
    (proxy_CvGameDatabase_005d "?LoadFromMemory@XMLSerializer@Database@@QAE_NPB_WPADI@Z")
    (proxy_CvGameDatabase_005e "?LoadFromMemory@XMLSerializer@Database@@QAE_NPB_WPADIAAVResultsCache@2@@Z")
    (proxy_CvGameDatabase_005f "?LogError@Connection@Database@@QBEXPBD@Z")
    (proxy_CvGameDatabase_0060 "?LogMessage@Connection@Database@@QBEXPBD@Z")
    (proxy_CvGameDatabase_0061 "?LogWarning@Connection@Database@@QBEXPBD@Z")
    (proxy_CvGameDatabase_0062 "?Open@Connection@Database@@QAE_NPBDH@Z")
    (proxy_CvGameDatabase_0063 "?Prepare@ResultsCache@Database@@QAEPAVResults@2@ABV?$basic_string@DU?$char_traits@D@std@@V?$allocator@D@2@@std@@PBDH@Z")
    (proxy_CvGameDatabase_0064 "?PushDatabase@Lua@Scripting@Database@@SAXPAUlua_State@@AAVConnection@3@@Z")
    (proxy_CvGameDatabase_0065 "?PushDatabaseQuery@Lua@Scripting@Database@@SAPAVResults@3@PAUlua_State@@PAVConnection@3@PBD@Z")
    (proxy_CvGameDatabase_0066 "?PushDatabaseRow@Lua@Scripting@Database@@SAXPAUlua_State@@PAVResults@3@@Z")
    (proxy_CvGameDatabase_0067 "?PushDatabaseTable@Lua@Scripting@Database@@SAXPAUlua_State@@AAVConnection@3@PBD@Z")
    (proxy_CvGameDatabase_0068 "?Release@Results@Database@@QAEXXZ")
    (proxy_CvGameDatabase_0069 "?RemoveTableFromPackageTagList@XMLSerializer@Database@@QAEXPBD@Z")
    (proxy_CvGameDatabase_006a "?Reset@Results@Database@@QAE_NXZ")
    (proxy_CvGameDatabase_006b "?RollbackToSavePoint@Connection@Database@@QAE_NPBD@Z")
    (proxy_CvGameDatabase_006c "?RollbackTransaction@Connection@Database@@QAEXXZ")
    (proxy_CvGameDatabase_006d "?Save@BinaryIO@Database@@QAE_NAAVConnection@2@@Z")
    (proxy_CvGameDatabase_006e "?SelectAll@Connection@Database@@QAE_NAAVResults@2@PBD@Z")
    (proxy_CvGameDatabase_006f "?SelectAt@Connection@Database@@QAE_NAAVResults@2@PBD11@Z")
    (proxy_CvGameDatabase_0070 "?SelectAt@Connection@Database@@QAE_NAAVResults@2@PBD1H@Z")
    (proxy_CvGameDatabase_0071 "?SelectAt@Connection@Database@@QAE_NAAVResults@2@PBD1N@Z")
    (proxy_CvGameDatabase_0072 "?SelectAt@Connection@Database@@QAE_NAAVResults@2@PBDH@Z")
    (proxy_CvGameDatabase_0073 "?SelectWhere@Connection@Database@@QAE_NAAVResults@2@PBD1@Z")
    (proxy_CvGameDatabase_0074 "?SetBusyTimeout@Connection@Database@@QAE_NH@Z")
    (proxy_CvGameDatabase_0075 "?SetColumns@Results@Database@@QAEXPBD@Z")
    (proxy_CvGameDatabase_0076 "?SetLogger@Connection@Database@@QAEXPAVIDatabaseLogger@2@@Z")
    (proxy_CvGameDatabase_0077 "?SetPackageTag@XMLSerializer@Database@@QAEXPBD@Z")
    (proxy_CvGameDatabase_0078 "?SetSavePoint@Connection@Database@@QAE_NPBD@Z")
    (proxy_CvGameDatabase_0079 "?SingleQuery@Results@Database@@QAEX_N@Z")
    (proxy_CvGameDatabase_007a "?SingleQuery@Results@Database@@QBE_NXZ")
    (proxy_CvGameDatabase_007b "?StatementCount@Connection@Database@@QBEHXZ")
    (proxy_CvGameDatabase_007c "?StatementSQL@Connection@Database@@QBEPBDH@Z")
    (proxy_CvGameDatabase_007d "?Step@Results@Database@@QAE_NXZ")
    (proxy_CvGameDatabase_007e "?TotalChanges@Connection@Database@@QBEHXZ")
    (proxy_CvGameDatabase_007f "?TryExecute@Results@Database@@QAE_NXZ")
    (proxy_CvGameDatabase_0080 "?TryReset@Results@Database@@QAE_NXZ")
    (proxy_CvGameDatabase_0081 "?UseTransactions@XMLSerializer@Database@@QAEX_N@Z")
    (proxy_CvGameDatabase_0082 "?UseTransactions@XMLSerializer@Database@@QBE_NXZ")
    (proxy_CvGameDatabase_0083 "?Vacuum@Connection@Database@@QBEXXZ")
    (proxy_CvGameDatabase_0084 "?ValidateFKConstraints@Connection@Database@@QBE_N_N@Z")
    (proxy_CvGameDatabase_0085 "?lCollectMemoryUsage@Lua@Scripting@Database@@SAHPAUlua_State@@@Z")
    (proxy_CvGameDatabase_0086 "?lGetMemoryUsage@Lua@Scripting@Database@@SAHPAUlua_State@@@Z")
}

pub fn init(ctx: &MppatchCtx) -> Result<()> {
    info!("Creating proxy definitions...");
    load_CvGameDatabase(ctx)?;
    Ok(())
}
