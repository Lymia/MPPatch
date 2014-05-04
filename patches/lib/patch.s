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

load_library Kernel32, "Kernel32.dll"
import_symbol_dynamic Kernel32, VirtualProtect, "VirtualProtect"

%macro patch 2+
    %%patch_instr_start:
        %2
    %%patch_instr_length: equ $ - %%patch_instr_start

    %%PatchFn:
        sub esp, 4
        mov ebx, esp

        push ebx
        push 0x40 ; PAGE_EXECUTE_READWRITE
        push %%patch_instr_length
        push_eip_rel %%patch_instr_start
        call VirtualProtect

        pop ebx

        eip_rel %%patch_instr_start
        mov esi, eax

        eip_rel %1
        mov edi, eax

        mov ecx, %%patch_instr_length

        cld
        rep movsb

        sub esp, 4
        mov ecx, esp

        push ecx
        push ebx ; old page permissions
        push %%patch_instr_length
        push_eip_rel %%patch_instr_start
        call VirtualProtect

        add esp, 4 ; just ditch the protection value we set ourselves.

        ret
    add_init_command %%PatchFn
%endmacro
