; Copyright (C) 2014 Lymia Aluysia <lymiahugs@gmail.com>
;
; Permission is hereby granted, free of charge, to any person obtaining a copy of
; this software and associated documentation files (the "Software"), to deal in
; the Software without restriction, including without limitation the rights to
; use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
; of the Software, and to permit persons to whom the Software is furnished to do
; so, subject to the following conditions:
;
; The above copyright notice and this permission notice shall be included in all
; copies or substantial portions of the Software.
;
; THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
; IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
; FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
; AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
; LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
; OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
; SOFTWARE.

; XML Hook at 100764A0
; Identifying string: 8D 54 24 0C 52 68 A4 26  08 10 8D 4C 24 18 89 7C

; Lua Hook at 1000B499
; Identifying string: 83 C4 38 5F 5E B8 01 00  00 00 5B C3 CC CC CC CC


; General offsets
segment_base_addr      : equ 0x10800000
align_size             : equ 0x1000
state_store_ptr_offset : equ 0x1061BE3C

; Windows API functions we use
LoadLibraryA_offset            : equ 0x10076788
GetProcAddress_offset          : equ 0x100767BE

; Return addresses
xml_parser_hook_return         : equ 0x100764A5
xml_parser_hook_continue       : equ 0x1007652C
lua_table_hook_return          : equ 0x1000B49E

; Imported function offsets
xml_check_label_offset         : equ 0x100084F0
xml_get_contents_offset        : equ 0x100084D0
Database_ExecuteMultiple_offset: equ 0x10003AD0
Database_LogMessage_offset     : equ 0x100028A0

; Bundles of Lua functions
lua_createtable_offset         : equ 0x1007687E
lua_gettop_offset              : equ 0x100768D8
lua_pushstring_offset          : equ 0x100768D2
lua_pushinteger_offset         : equ 0x100768CC
lua_rawset_offset              : equ 0x100768C6

%define library_file_name "CvGameDatabaseWin32Final Release"

%macro special_init 0
    ; Nothing here!
%endmacro

%macro XMLParserHook_LoadVariables 0
    mov edi, esi              ; this
    lea esi, [esp+0x18+8*4+4] ; name_node
    mov edi, [edi + 768]      ; connection
%endmacro
%macro XMLParserHook_ContinuePatchInstructions 0
    ; Nothing here!
%endmacro
%macro XMLParserHook_PatchInstructions 0
    lea edx, [esp+0x20-0x14]
    push edx
%endmacro

%macro LuaTableHook_LoadVariables 0
    mov edi, esi ; Lua state
    mov esi, ebx ; Lua table
    ret
%endmacro
%macro LuaTableHook_PatchInstructions 0
    add esp, 0x38
    pop edi
    pop esi
%endmacro

%include "CvGameDatabase/main.s"