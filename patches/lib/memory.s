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

; Macro for reserving some memory
%define memory_size 0
%macro reserve_memory 2
    %1: equ memory_size
    %define memory_size %[memory_size] + %2
%endmacro

; Macro for getting the offset of some bit of memory
get_memory_rt:
    eip_rel_long state_store_ptr_offset
    mov eax, [eax]
    ret
%macro get_memory 1
    call get_memory_rt
    add eax, %1
%endmacro

; This is put in a macro so we can delay this part until after all our main code is generated.
%macro memory_init 0
    InitMemorySubsystem:
        call MemoryBootstrap_LoadSyms

        mov byte [eax], 1
        call edi ; GetProcessHeap
        push memory_size
        push 0x8 | 0x4 ; HEAP_ZERO_MEMORY | HEAP_GENERATE_EXCEPTIONS
        push eax
        call esi ; HeapAlloc

        mov ebx, eax
        eip_rel_long state_store_ptr_offset
        mov dword [eax], ebx

        ret
%endmacro