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
XMLParserHook_ReturnAddr  : equ 0x08B9C01A
XMLParserHook_ContinueAddr: equ 0x08B9C0BC
LuaTableHook_ReturnAddr   : equ 0x08B9AE7B

%macro XMLParserHook_LoadVariables 0
    mov esi, edi         ; name_node
    mov edi, [ebp + 768] ; connection
%endmacro
%define XMLParserHook_ContinueStatusRegister eax
%macro XMLParserHook_ContinuePatchInstructions 0
    ; Nothing here!
%endmacro
%macro XMLParserHook_PatchInstructions 0
    mov dword [esp+0x5C-0x1C], 5
%endmacro

%macro LuaTableHook_LoadVariables 0
    mov esi, edi ; Lua table
    mov edi, ebp ; Lua state
%endmacro
%macro LuaTableHook_PatchInstructions 0
    mov eax, 1
%endmacro
