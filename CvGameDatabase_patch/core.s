GetCurrentAddress:
    mov eax, [esp]
    ret
GetRelocBase:
    .data_store_inst:
        call GetCurrentAddress
        jmp .next
    .data_store_offset: equ $-.data_store_inst
    .data_store: db text_base_addr
    .next:
        add eax, .data_store_offset
        ret