; Copyright (C) 2015-2023 Lymia Kanokawa <lymia@lymia.moe>
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

%include "symbols.s"

%macro symbol_toString 2
    %defstr tmp_str %2
    segment .rdata
        %1: dd tmp_str, 0
    segment .text
    %undef tmp_str
%endmacro

segment .rdata_proxy_symbols
global cif_symbolTable
cif_symbolTable:
segment .text

%macro symbol_add 2
    segment .rdata_proxy_symbols
        dd 1, %1, %2
    segment .text
%endmacro
%macro symbol_end 0
    segment .rdata_proxy_symbols
        dd 0, 0, 0
    segment .text
%endmacro

%macro generate_variables 1
    symbol_toString %1_name, %1

    segment .text_proxy
        global _%1
        _%1: dd 0, 0
        export %1
    segment .text

    symbol_add _%1, %1_name
%endmacro
proxy_symbols generate_variables

symbol_end