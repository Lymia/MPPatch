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

; Load addresses relative to EIP. This encoding lets us get away with not having to use relocations,
; or jump into x86_64, which for obvious reasons,  we can't do.
eip_rel_rt:
    ; I could have done call/pop, but, apparently that screws up the CPU's optimizations a lot.
    ; Since some of this could easily be on a hot code path, I'll avoid doing anything fragile like that.
    mov eax, [esp]
    ret
%macro eip_rel 1
        call eip_rel_rt
    %%return_addr:
        add eax, %1 - %%return_addr
%endmacro
%macro eip_rel_offset 1
        call eip_rel_rt
    %%return_addr:
        add eax, 0 - (%%return_addr - __start) - (segment_base_addr - %1)
%endmacro
%macro push_eip_rel 1
    eip_rel %1
    push eax
%endmacro
%macro const 2+
        eip_rel %%store
        jmp %%next
    %%store: %1 %2
    %%next:
%endmacro

; Convenience functions for storing registers/etc
%macro push_all 0
    pushad
    pushfd
%endmacro
%macro pop_all 0
    popfd
    popad
%endmacro

; Convenience function for importing functions that we need.
%macro import_symbol 1
    %1: call %1_offset
%endmacro

; Initialization
%define init_commands nop
%macro add_init_command 1
    %define init_commands %[init_commands]; call %1
%endmacro
