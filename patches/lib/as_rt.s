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

bits 32
segment .data
__data_start:
segment .text
__start:

; Segmentation convenience functions
%macro absolute_import 2
    [absolute %2]
    %1: resd 1
    __SECT__
%endmacro

; Import convenience functions
%macro extern_as 2
    extern %2
    %1: equ %2
%endmacro
extern _asm_resolveAddress@4
%macro prepare_symbol 2
    push 0
    push_all
    push %2
    call _asm_resolveAddress@4
    mov dword [esp+4+8*4], eax
    pop_all
    pop %1
%endmacro
%macro import_symbol 1
    %1: jmp_to_symbol %1_offset
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

; A little asm -> C interop
%macro redefine_for_c 2
    global _%2
    global _%2_addr
    _%2: jmp %1
    segment .rdata
        _%2_addr: dd %1
    segment .text
%endmacro

; And for DLL proxying
%macro proxy_symbol_file 1
    extern _asm_resolveSymbol@4

    %macro proxy_symbol 1
        %defstr proxy_symbol_name %1
        segment .data
            %1_offset: dd 0
        segment .rdata
            %1_name: dd proxy_symbol_name, 0
        segment .text
        %undef proxy_symbol_name
        global _%1
        _%1: jmp [%1_offset]
        export %1
    %endmacro
    %include %1
    %unmacro proxy_symbol 1

    %macro proxy_symbol 1
        push %1_name
        call _asm_resolveSymbol@4
        mov [%1_offset], eax
    %endmacro
    global _InitializeProxy
    _InitializeProxy:
        %include %1
        ret
    %unmacro proxy_symbol 1
%endmacro