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

import_symbol LoadLibraryA
import_symbol GetProcAddress

%macro load_library 2
    %ifndef %1_loaded
        reserve_memory %%MemoryStore, 4
        %%DllName: db %2, 0
        %%InitFn:
            push_eip_rel %%DllName
            call LoadLibraryA

            mov ebx, eax
            get_memory %%MemoryStore
            mov dword [eax], ebx
            ret
        add_init_command %%InitFn
        %1: equ %%MemoryStore

        %define %1_loaded
    %endif
%endmacro

%macro import_symbol_dynamic 3
    %ifndef %2_loaded
        reserve_memory %%MemoryStore, 4
        %%SymbolName: db %3, 1
        %%InitFn:
            get_memory %1
            mov ebx, [eax]

            push_eip_rel %%SymbolName
            push ebx
            call GetProcAddress
            mov ebx, eax

            get_memory %%MemoryStore
            mov dword [eax], ebx

            ret
        add_init_command %%InitFn
        %2:
            get_memory %%MemoryStore
            call [eax]
        %define %2_loaded
    %endif
%endmacro

; Bootstrap function to get functions the memory allocator uses
MemoryBootstrap_LoadSyms_Library       : db "Kernel32.dll", 0
MemoryBootstrap_LoadSyms_HeapAlloc     : db "HeapAlloc", 0
MemoryBootstrap_LoadSyms_GetProcessHeap: db "GetProcessHeap", 0
MemoryBootstrap_LoadSyms:
    push_eip_rel MemoryBootstrap_LoadSyms_Library
    call LoadLibraryA
    mov ebx, eax

    push_eip_rel MemoryBootstrap_LoadSyms_HeapAlloc
    push ebx
    call GetProcAddress
    mov esi, eax

    push_eip_rel MemoryBootstrap_LoadSyms_GetProcessHeap
    push ebx
    call GetProcAddress
    mov edi, eax

    ret

