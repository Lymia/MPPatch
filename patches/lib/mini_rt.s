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
org segment_base_addr
__start:

jmp __main

%macro marker 1+
    %ifdef DEBUG
        db "----- ", %1, " -----"
    %endif
%endmacro

marker "patch rt"
%include "lib/utils.s"
%include "lib/dynamic_load.s"
%include "lib/patch.s"

marker "patch body"
Patch_BodyFunctions

marker "main"
__main:
    push_all
    init_commands
    %ifdef USE_MAIN
        call main
    %endif
    patch_commands
    pop_all
    ret

; just in case
int3
xor eax, eax
mov eax, [eax]
marker "end"
align align_size
