; Copyright (C) 2015 Lymia Aluysia <lymiahugs@gmail.com>
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

extern cif_resolveSymbol

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
%include "symbols.s"
%unmacro proxy_symbol 1

%macro proxy_symbol 1
    push %1_name
    call cif_resolveSymbol
    mov [%1_offset], eax
%endmacro
global _InitializeProxy
_InitializeProxy:
    %include "symbols.s"
    ret
%unmacro proxy_symbol 1

%macro redefine_for_c 2
    global %2
    %2: jmp %1
%endmacro

redefine_for_c _?ExecuteMultiple@Connection@Database@@QBE_NPBDH@Z, cif_Database_ExecuteMultiple
redefine_for_c _?LogMessage@Connection@Database@@QBEXPBD@Z       , cif_Database_LogMessage
