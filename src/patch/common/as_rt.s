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

; Convenience functions
%macro push_all 0
    pushad
    pushfd
%endmacro
%macro pop_all 0
    popfd
    popad
%endmacro

%macro symbol_toString 2
    %defstr tmp_str %2
    segment .rdata
        %1: dd tmp_str, 0
    segment .text
    %undef tmp_str
%endmacro

; Generate jump target symbol
segment .rdata_jmplist
global cif_jmplist
cif_jmplist:
segment .text

%macro jmptarget 1
    %1: dd 0, 0
%endmacro

%macro jmplist_add 4
    segment .rdata_jmplist
        dd 1, %1, %2, %3, %4
    segment .text
%endmacro
%macro jmplist_end 0
    segment .rdata_jmplist
        dd 0, 0, 0, 0, 0
    segment .text
%endmacro

%macro dynamic_jmp 1
    symbol_toString .%1_name, %1

    segment .text_jmplist
        jmptarget .%1_jmp

    jmplist_add .%1_jmp, 0, %1, .%1_name

    segment .text
        jmp .%1_jmp
%endmacro