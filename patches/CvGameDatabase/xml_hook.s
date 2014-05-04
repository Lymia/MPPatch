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

import_symbol xml_check_label
import_symbol xml_get_contents
import_symbol Database_ExecuteMultiple
import_symbol Database_LogMessage

XMLParser_InitCheck_Flag: db 0
XMLParser_InitCheck:
        eip_rel XMLParser_InitCheck_Flag
        mov ebx, eax
        mov al, [ebx]
        test al, al
        jne .skipinit

        push_eip_rel PatchMarkerString
        mov ecx, edi
        call Database_LogMessage

        mov byte [ebx], 1
    .skipinit:
        ret

%macro XMLParser_IfTagNoMatch 3
    push_eip_rel %1
    const dd, %2
    push eax
    mov ecx, esi
    call xml_check_label
    test al, al
    je %3
%endmacro

XMLParser_TagIgnore: db "__MOD2DLC_PATCH_IGNORE", 0
XMLParser_TagRawSql: db "__MOD2DLC_PATCH_RAWSQL", 0
XMLParser_ErrorMsg : db "Failed to execute statement while processing __MOD2DLC_RAWSQL tag.", 0
XMLParserHookCore:
        call XMLParser_InitCheck

        ; Test for __MOD2DLC_PATCH_IGNORE
        XMLParser_IfTagNoMatch XMLParser_TagIgnore, 16, .next
    .continue:
        ; Continue onto the next XML node
        mov al, 1
        mov bl, 1
        ret
    .next:
        ; Test for __MOD2DLC_PATCH_RAWSQL
        XMLParser_IfTagNoMatch XMLParser_TagRawSql, 16, .cancel

        ; Get the contents of the XML tag.
        push 0
        mov eax, esi
        push 0
        mov ebx, esi
        push eax ; length
        push ebx ; contents
        mov ecx, esi
        call xml_get_contents

        pop ebx ; contents
        pop eax ; length

        ; Execute the SQL contained in the XML tag
        push eax
        push ebx
        mov ecx, edi
        call Database_ExecuteMultiple
        test al, al
        je .continue

        ; If something went wrong, write a short debug message to Database.log
        push_eip_rel XMLParser_ErrorMsg
        mov ecx, edi
        call Database_LogMessage

        ; Return an error!
        mov al, 1
        mov bl, 0
        ret
    .cancel:
        ; We didn't find any of our new tags-- bail out.
        mov al, 0
        ret
XMLParserHook:
        push_all

        XMLParserHook_LoadVariables
        call XMLParserHookCore

        test al, al
        je .proceedExit

        pop_all
        mov al, bl
        XMLParserHook_PatchInstructions
        jmp xml_parser_hook_continue

    .proceedExit:

        pop_all
        XMLParserHook_PatchInstructions
        jmp xml_parser_hook_return