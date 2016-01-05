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

extern cif_LuaTableHookCore
global cif_LuaTableHook
cif_LuaTableHook:
    push_all
    LuaTableHook_LoadVariables
    push esi ; table
    push edi ; lua_State
    call cif_LuaTableHookCore
    pop_all

    prepare_symbol LuaTableHook_SafeRegister, LuaTableHook_ReturnAddr
    LuaTableHook_PatchInstructions
    jmp LuaTableHook_SafeRegister

extern cif_XmlParserHookCore
global cif_XmlParserHook
cif_XmlParserHook:
        sub esp, 4
        push_all

        XMLParserHook_LoadVariables

        sub esp, 4
        mov eax, esp

        push eax ; failure_value
        push edi ; connection
        push esi ; xml_node
        call cif_XmlParserHookCore

        pop ebx

        test al, al
        jz .proceedExit

        mov [esp+4+4*8], ebx
        pop_all
        pop XMLParserHook_ContinueStatusRegister

        prepare_symbol XMLParserHook_ContinueSafeRegister, XMLParserHook_ContinueAddr
        XMLParserHook_ContinuePatchInstructions
        jmp XMLParserHook_ContinueSafeRegister

    .proceedExit:
        pop_all
        add esp, 4

        prepare_symbol XMLParserHook_SafeRegister, XMLParserHook_ReturnAddr
        XMLParserHook_PatchInstructions
        jmp XMLParserHook_SafeRegister

