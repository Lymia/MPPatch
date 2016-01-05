; Copyright (C) 2015-2016 Lymia Aluysia <lymiahugs@gmail.com>
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

; Return addresses
XMLParserHook_ReturnAddr  : equ 0x100764A5
XMLParserHook_ContinueAddr: equ 0x1007652C
LuaTableHook_ReturnAddr   : equ 0x1000B49E

%macro XMLParserHook_LoadVariables 0
    mov edi, esi              ; this
    lea esi, [esp+0x10+8*4+8] ; name_node
    mov edi, [edi + 768]      ; connection
%endmacro
%define XMLParserHook_ContinueSafeRegister ecx
%define XMLParserHook_ContinueStatusRegister eax
%macro XMLParserHook_ContinuePatchInstructions 0
    ; Nothing here!
%endmacro
%define XMLParserHook_SafeRegister ecx
%macro XMLParserHook_PatchInstructions 0
    lea edx, [esp+0x20-0x14]
    push edx
%endmacro

%macro LuaTableHook_LoadVariables 0
    mov edi, esi ; Lua state
    mov esi, ebx ; Lua table
%endmacro
%define LuaTableHook_SafeRegister ebx
%macro LuaTableHook_PatchInstructions 0
    add esp, 0x38
    pop edi
    pop esi
%endmacro
