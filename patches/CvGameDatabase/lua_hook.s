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

import_symbol lua_createtable
import_symbol lua_gettop
import_symbol lua_pushstring
import_symbol lua_pushinteger
import_symbol lua_rawset

LuaTable_storev_rt:
    push dword [esp+4]
    push edi
    call lua_pushstring

    mov eax, [esp+12]
    push dword [esp+8]
    push edi
    call eax

    push esi
    push edi
    call lua_rawset

    retn 12
%macro LuaTable_store_string 2
    push_eip_rel lua_pushstring
    push_eip_rel %2
    push_eip_rel %1
    call LuaTable_storev_rt
%endmacro
%macro LuaTable_store_integer 2
    push_eip_rel lua_pushinteger
    eip_rel %2
    push dword [eax]
    push_eip_rel %1
    call LuaTable_storev_rt
%endmacro

LuaTable_maketable_rt:
    push dword [esp+8]
    push edi
    call lua_pushstring

    push 0
    push 0
    push edi
    call lua_createtable

    push esi
    push edi
    call lua_gettop
    mov esi, eax
    call [esp+4+4]
    pop esi

    push esi
    push edi
    call lua_rawset

    retn 8
%macro LuaTable_maketable 2
    push_eip_rel %1
    push_eip_rel %2
    call LuaTable_maketable_rt
%endmacro


LuaTable_PatchVersionMinor: db "major", 0
LuaTable_PatchVersionMajor: db "minor", 0
LuaTable_PatchMinCompat   : db "mincompat", 0
LuaTable_Version:
    LuaTable_store_integer LuaTable_PatchVersionMajor, PatchVersionMajor
    LuaTable_store_integer LuaTable_PatchVersionMinor, PatchVersionMinor
    LuaTable_store_integer LuaTable_PatchMinCompat   , PatchMinCompat
    ret

LuaTable_PatchMarker      : db "creditstring", 0
LuaTable_VersionTable     : db "versioninfo", 0
LuaTable_Main:
    LuaTable_maketable LuaTable_VersionTable, LuaTable_Version
    LuaTable_store_string  LuaTable_PatchMarker      , PatchMarkerString
    ret

LuaTable_PatchTable       : db "__mod2dlc_patch", 0
LuaTableHookCore:
    LuaTable_maketable LuaTable_PatchTable, LuaTable_Main
    ret
LuaTableHook:
    push_all
    LuaTableHook_LoadVariables
    call LuaTableHookCore
    pop_all

    LuaTableHook_PatchInstructions
    jmp lua_table_hook_return
