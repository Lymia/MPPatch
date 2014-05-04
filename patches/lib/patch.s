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

; Patch list
%define patch_commands nop
%macro add_patch_command 1
    %define patch_commands %[init_commands]; call %1
%endmacro

; Patch definition
; edi = lpAddress, esi = dwSize, ebp = patchSource
patch_SetSegmentWritable:
        sub esp, 4
        mov ebx, esp

        push ebx
        push 0x40 ; PAGE_EXECUTE_READWRITE
        push esi
        push edi
        call VirtualProtect

        pop ebx

        push edi
        push esi

        mov ecx, esi
        mov esi, ebp

        cld
        rep movsb

        pop esi
        pop edi

        sub esp, 4
        mov ecx, esp

        push ecx
        push ebx ; old page permissions
        push esi
        push edi
        call VirtualProtect

        add esp, 4 ; just ditch the protection value we set ourselves.

        ret
%macro patch 2+
    %%patch_instr_start:
        [section %%patch_section vstart=%1]
        %2
        [section main]
    %%patch_instr_length: equ $ - %%patch_instr_start

    %%PatchFn:
        eip_rel %%patch_instr_start
        mov ebp, eax
        eip_rel_long %1
        mov edi, eax
        mov esi, %%patch_instr_length

        jmp patch_SetSegmentWritable
    add_patch_command %%PatchFn
%endmacro
