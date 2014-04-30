%macro const 3+
    %%start:
        push esi
        call GetAddress
        jmp %%next
    %%store_offset: equ $ - %%start
    %%store_data: %2 %3
    %%next:
        add esi, %%store_offset
        mov %1, esi
        pop esi
%endmacro
%macro invoke_offset 1
    push ebx
    mov ebx, %1
    call ResolveAddress
    pop ebx
    jmp eax
%endmacro
%macro import_symbol 1
    %1: invoke_offset %1_offset
%endmacro

GetAddress:
    mov esi, [esp]
    ret
ResolveAddress:
    call GetRelocBase
    add eax, ebx
    ret

BuildReturnAddress:
    push eax
    push ebx
    pushfd
    mov ebx, [esp+4+3*4]
    call ResolveAddress
    mov [esp+4+3*4], eax
    popfd
    pop ebx
    pop eax
    ret