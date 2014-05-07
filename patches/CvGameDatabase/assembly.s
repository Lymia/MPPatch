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

%include "lib/as_rt.s"

extern _LuaTableHookCore@8
global _LuaTableHook
_LuaTableHook:
    push_all
    LuaTableHook_LoadVariables
    push esi ; table
    push edi ; lua_State
    call _LuaTableHookCore@8
    pop_all

    prepare_symbol LuaTableHook_SafeRegister, lua_table_hook_return
    LuaTableHook_PatchInstructions
    jmp LuaTableHook_SafeRegister

extern _XmlParserHookCore@12
global _XmlParserHook
_XmlParserHook:
        push_all

        XMLParserHook_LoadVariables

        add esp, 4
        mov eax, esp

        push eax ; failure_value
        push edi ; connection
        push esi ; xml_node
        call _XmlParserHookCore@12

        pop ebx

        test al, al
        je .proceedExit

        pop_all
        mov al, bl

        prepare_symbol XMLParserHook_ContinueSafeRegister, xml_parser_hook_continue
        XMLParserHook_ContinuePatchInstructions
        jmp XMLParserHook_ContinueSafeRegister

    .proceedExit:
        pop_all

        prepare_symbol XMLParserHook_SafeRegister, xml_parser_hook_return
        XMLParserHook_PatchInstructions
        jmp XMLParserHook_SafeRegister

proxy_symbol_file symbolPath

redefine_for_c _?ExecuteMultiple@Connection@Database@@QBE_NPBDH@Z, Database_ExecuteMultiple
redefine_for_c _?LogMessage@Connection@Database@@QBEXPBD@Z       , Database_LogMessage
