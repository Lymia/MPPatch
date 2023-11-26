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

macro_rules! make_proxy_base {
    (($sym:ident proxy $info:tt $target:literal)) => {
        make_proxy_base!(($sym $target));
    };
    (($sym:ident $target:literal)) => {
        #[naked]
        #[export_name = concat!("mppatch_proxy_CvGameDatabase_", $target)]
        #[link_section = ".text_proxy"]
        fn $sym(number: usize) -> usize {
            unsafe {
                std::arch::asm!(".byte 0,0,0,0,0", options(noreturn))
            }
        }
    };
}
macro_rules! make_proxy {
    ($($entry:tt)*) => {
        $(
            make_proxy_base!($entry);
        )*
    };
}

make_proxy! {
    (proxy_s0000 "??0BinaryIO@Database@@QAE@PBD@Z")
    (proxy_s0001 "??0Connection@Database@@QAE@PBDH@Z")
    (proxy_s0002 "??0Connection@Database@@QAE@XZ")
    (proxy_s0003 "??0Results@Database@@QAE@ABV01@@Z")
    (proxy_s0004 "??0Results@Database@@QAE@PBD@Z")
    (proxy_s0005 "??0ResultsCache@Database@@QAE@AAVConnection@1@@Z")
    (proxy_s0006 "??0SingleResult@Database@@QAE@PBD@Z")
    (proxy_s0007 "??0XMLSerializer@Database@@QAE@AAVConnection@1@@Z")
    (proxy_s0008 "??1Connection@Database@@QAE@XZ")
    (proxy_s0009 "??1Results@Database@@UAE@XZ")
    (proxy_s000a "??1ResultsCache@Database@@QAE@XZ")
    (proxy_s000b "??1XMLSerializer@Database@@QAE@XZ")
    (proxy_s000c "??2Connection@Database@@SAPAXI@Z")
    (proxy_s000d "??2Results@Database@@SAPAXI@Z")
    (proxy_s000e "??3Connection@Database@@SAXPAX@Z")
    (proxy_s000f "??3Results@Database@@SAXPAX@Z")
    (proxy_s0010 "??5BinaryIO@Database@@QAE_NAAVConnection@1@@Z")
    (proxy_s0011 "??6BinaryIO@Database@@QAE_NAAVConnection@1@@Z")
    (proxy_s0012 "??_FResults@Database@@QAEXXZ")
    (proxy_s0013 "??_FSingleResult@Database@@QAEXXZ")
    (proxy_s0014 "?AddTableToPackageTagList@XMLSerializer@Database@@QAEXPBD@Z")
    (proxy_s0015 "?Analyze@Connection@Database@@QBEXXZ")
    (proxy_s0016 "?BeginDeferredTransaction@Connection@Database@@QAEXXZ")
    (proxy_s0017 "?BeginExclusiveTransaction@Connection@Database@@QAEXXZ")
    (proxy_s0018 "?BeginImmediateTransaction@Connection@Database@@QAEXXZ")
    (proxy_s0019 "?BeginTransaction@Connection@Database@@QAEXXZ")
    (proxy_s001a "?Bind@Results@Database@@QAE_NHH@Z")
    (proxy_s001b "?Bind@Results@Database@@QAE_NHM@Z")
    (proxy_s001c "?Bind@Results@Database@@QAE_NHN@Z")
    (proxy_s001d "?Bind@Results@Database@@QAE_NHPBDH_N@Z")
    (proxy_s001e "?Bind@Results@Database@@QAE_NHPBD_N@Z")
    (proxy_s001f "?Bind@Results@Database@@QAE_NHPB_WH_N@Z")
    (proxy_s0020 "?Bind@Results@Database@@QAE_NHPB_W_N@Z")
    (proxy_s0021 "?Bind@Results@Database@@QAE_NH_J@Z")
    (proxy_s0022 "?BindNULL@Results@Database@@QAE_NH@Z")
    (proxy_s0023 "?CalculateMemoryStats@Connection@Database@@QAEPBDXZ")
    (proxy_s0024 "?Clear@ResultsCache@Database@@QAEXABV?$basic_string@DU?$char_traits@D@std@@V?$allocator@D@2@@std@@@Z")
    (proxy_s0025 "?Clear@ResultsCache@Database@@QAEXXZ")
    (proxy_s0026 "?ClearCountCache@Connection@Database@@QAEXXZ")
    (proxy_s0027 "?ClearPackageTagList@XMLSerializer@Database@@QAEXXZ")
    (proxy_s0028 "?Close@Connection@Database@@QAEXXZ")
    (proxy_s0029 "?ColumnCount@Results@Database@@QBEHXZ")
    (proxy_s002a "?ColumnName@Results@Database@@QAEPBDH@Z")
    (proxy_s002b "?ColumnPosition@Results@Database@@QAEHPBD@Z")
    (proxy_s002c "?ColumnType@Results@Database@@QAE?AW4ColumnTypes@2@H@Z")
    (proxy_s002d "?ColumnTypeName@Results@Database@@QAEPBDH@Z")
    (proxy_s002e "?CommitTransaction@Connection@Database@@QAEXXZ")
    (proxy_s002f "?Connection@ResultsCache@Database@@QAEAAV02@XZ")
    (proxy_s0030 "?Count@Connection@Database@@QAEHPBD_N@Z")
    (proxy_s0031 "?EndTransaction@Connection@Database@@QAEXXZ")
    (proxy_s0032 "?ErrorCode@Connection@Database@@QBEHXZ")
    (proxy_s0033 "?ErrorMessage@Connection@Database@@QBEPBDXZ")
    (proxy_s0034 "?ErrorMessage@ResultsCache@Database@@QBEPBDXZ")
    (proxy_s0035 "?ErrorMessage@XMLSerializer@Database@@QBEPBDXZ")
    (proxy_s0036 "?Execute@Connection@Database@@QBE_NAAVResults@2@PBDH@Z")
    (proxy_s0037 "?Execute@Connection@Database@@QBE_NPBDH@Z")
    (proxy_s0038 "?Execute@Results@Database@@QAE_NXZ")
    (proxy_s0039 "?ExecuteMultiple@Connection@Database@@QBE_NPBDH@Z")
    (proxy_s003a "?Get@ResultsCache@Database@@QAEPAVResults@2@ABV?$basic_string@DU?$char_traits@D@std@@V?$allocator@D@2@@std@@@Z")
    (proxy_s003b "?GetBool@Results@Database@@QAE_NH@Z")
    (proxy_s003c "?GetBool@Results@Database@@QAE_NPBD@Z")
    (proxy_s003d "?GetColumns@Results@Database@@UBEPBDXZ")
    (proxy_s003e "?GetDouble@Results@Database@@QAENH@Z")
    (proxy_s003f "?GetDouble@Results@Database@@QAENPBD@Z")
    (proxy_s0040 "?GetFloat@Results@Database@@QAEMH@Z")
    (proxy_s0041 "?GetFloat@Results@Database@@QAEMPBD@Z")
    (proxy_s0042 "?GetInt64@Results@Database@@QAE_JH@Z")
    (proxy_s0043 "?GetInt64@Results@Database@@QAE_JPBD@Z")
    (proxy_s0044 "?GetInt@Results@Database@@QAEHH@Z")
    (proxy_s0045 "?GetInt@Results@Database@@QAEHPBD@Z")
    (proxy_s0046 "?GetText16@Results@Database@@QAEPB_WH@Z")
    (proxy_s0047 "?GetText16@Results@Database@@QAEPB_WPBD@Z")
    (proxy_s0048 "?GetText@Results@Database@@QAEPBDH@Z")
    (proxy_s0049 "?GetText@Results@Database@@QAEPBDPBD@Z")
    (proxy_s004a "?GetValue@Results@Database@@QAEXHAAH@Z")
    (proxy_s004b "?GetValue@Results@Database@@QAEXHAAM@Z")
    (proxy_s004c "?GetValue@Results@Database@@QAEXHAAN@Z")
    (proxy_s004d "?GetValue@Results@Database@@QAEXHAAPBD@Z")
    (proxy_s004e "?GetValue@Results@Database@@QAEXHAAPB_W@Z")
    (proxy_s004f "?GetValue@Results@Database@@QAEXHAA_J@Z")
    (proxy_s0050 "?GetValue@Results@Database@@QAEXHAA_N@Z")
    (proxy_s0051 "?GetValue@Results@Database@@QAEXPBDAAH@Z")
    (proxy_s0052 "?GetValue@Results@Database@@QAEXPBDAAM@Z")
    (proxy_s0053 "?GetValue@Results@Database@@QAEXPBDAAN@Z")
    (proxy_s0054 "?GetValue@Results@Database@@QAEXPBDAAPBD@Z")
    (proxy_s0055 "?GetValue@Results@Database@@QAEXPBDAAPB_W@Z")
    (proxy_s0056 "?GetValue@Results@Database@@QAEXPBDAA_J@Z")
    (proxy_s0057 "?GetValue@Results@Database@@QAEXPBDAA_N@Z")
    (proxy_s0058 "?HasColumn@Results@Database@@QAE_NPBD@Z")
    (proxy_s0059 "?HashColumnPositions@Results@Database@@IAEXXZ")
    (proxy_s005a "?Load@BinaryIO@Database@@QAE_NAAVConnection@2@@Z")
    (proxy_s005b "?Load@XMLSerializer@Database@@QAE_NPB_W@Z")
    (proxy_s005c "?Load@XMLSerializer@Database@@QAE_NPB_WAAVResultsCache@2@@Z")
    (proxy_s005d "?LoadFromMemory@XMLSerializer@Database@@QAE_NPB_WPADI@Z")
    (proxy_s005e "?LoadFromMemory@XMLSerializer@Database@@QAE_NPB_WPADIAAVResultsCache@2@@Z")
    (proxy_s005f "?LogError@Connection@Database@@QBEXPBD@Z")
    (proxy_s0060 "?LogMessage@Connection@Database@@QBEXPBD@Z")
    (proxy_s0061 "?LogWarning@Connection@Database@@QBEXPBD@Z")
    (proxy_s0062 "?Open@Connection@Database@@QAE_NPBDH@Z")
    (proxy_s0063 "?Prepare@ResultsCache@Database@@QAEPAVResults@2@ABV?$basic_string@DU?$char_traits@D@std@@V?$allocator@D@2@@std@@PBDH@Z")
    (proxy_s0064 "?PushDatabase@Lua@Scripting@Database@@SAXPAUlua_State@@AAVConnection@3@@Z")
    (proxy_s0065 "?PushDatabaseQuery@Lua@Scripting@Database@@SAPAVResults@3@PAUlua_State@@PAVConnection@3@PBD@Z")
    (proxy_s0066 "?PushDatabaseRow@Lua@Scripting@Database@@SAXPAUlua_State@@PAVResults@3@@Z")
    (proxy_s0067 "?PushDatabaseTable@Lua@Scripting@Database@@SAXPAUlua_State@@AAVConnection@3@PBD@Z")
    (proxy_s0068 "?Release@Results@Database@@QAEXXZ")
    (proxy_s0069 "?RemoveTableFromPackageTagList@XMLSerializer@Database@@QAEXPBD@Z")
    (proxy_s006a "?Reset@Results@Database@@QAE_NXZ")
    (proxy_s006b "?RollbackToSavePoint@Connection@Database@@QAE_NPBD@Z")
    (proxy_s006c "?RollbackTransaction@Connection@Database@@QAEXXZ")
    (proxy_s006d "?Save@BinaryIO@Database@@QAE_NAAVConnection@2@@Z")
    (proxy_s006e "?SelectAll@Connection@Database@@QAE_NAAVResults@2@PBD@Z")
    (proxy_s006f "?SelectAt@Connection@Database@@QAE_NAAVResults@2@PBD11@Z")
    (proxy_s0070 "?SelectAt@Connection@Database@@QAE_NAAVResults@2@PBD1H@Z")
    (proxy_s0071 "?SelectAt@Connection@Database@@QAE_NAAVResults@2@PBD1N@Z")
    (proxy_s0072 "?SelectAt@Connection@Database@@QAE_NAAVResults@2@PBDH@Z")
    (proxy_s0073 "?SelectWhere@Connection@Database@@QAE_NAAVResults@2@PBD1@Z")
    (proxy_s0074 "?SetBusyTimeout@Connection@Database@@QAE_NH@Z")
    (proxy_s0075 "?SetColumns@Results@Database@@QAEXPBD@Z")
    (proxy_s0076 "?SetLogger@Connection@Database@@QAEXPAVIDatabaseLogger@2@@Z")
    (proxy_s0077 "?SetPackageTag@XMLSerializer@Database@@QAEXPBD@Z")
    (proxy_s0078 "?SetSavePoint@Connection@Database@@QAE_NPBD@Z")
    (proxy_s0079 "?SingleQuery@Results@Database@@QAEX_N@Z")
    (proxy_s007a "?SingleQuery@Results@Database@@QBE_NXZ")
    (proxy_s007b "?StatementCount@Connection@Database@@QBEHXZ")
    (proxy_s007c "?StatementSQL@Connection@Database@@QBEPBDH@Z")
    (proxy_s007d "?Step@Results@Database@@QAE_NXZ")
    (proxy_s007e "?TotalChanges@Connection@Database@@QBEHXZ")
    (proxy_s007f "?TryExecute@Results@Database@@QAE_NXZ")
    (proxy_s0080 "?TryReset@Results@Database@@QAE_NXZ")
    (proxy_s0081 "?UseTransactions@XMLSerializer@Database@@QAEX_N@Z")
    (proxy_s0082 "?UseTransactions@XMLSerializer@Database@@QBE_NXZ")
    (proxy_s0083 "?Vacuum@Connection@Database@@QBEXXZ")
    (proxy_s0084 "?ValidateFKConstraints@Connection@Database@@QBE_N_N@Z")
    (proxy_s0085 "?lCollectMemoryUsage@Lua@Scripting@Database@@SAHPAUlua_State@@@Z")
    (proxy_s0086 proxy (lGetMemoryUsage, crate::hook_lua::lGetMemoryUsageProxy) "?lGetMemoryUsage@Lua@Scripting@Database@@SAHPAUlua_State@@@Z")
}
