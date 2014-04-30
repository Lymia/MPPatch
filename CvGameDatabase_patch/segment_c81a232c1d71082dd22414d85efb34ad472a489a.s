bits 32

text_base_addr: equ 0x10001000

%macro addr_offset 2
    %1: equ %2 - text_base_addr
%endmacro

addr_offset xml_parser_hook_return         , 0x100764A5
addr_offset xml_parser_hook_continue       , 0x1007652C
addr_offset lua_table_hook_return          , 0x1000B49E

addr_offset xml_check_label_offset         , 0x100084F0
addr_offset xml_get_contents_offset        , 0x100084D0
addr_offset Database_ExecuteMultiple_offset, 0x10003AD0
addr_offset Database_LogMessage_offset     , 0x100028A0
addr_offset lua_pushstring_offset          , 0x100768D2
addr_offset lua_pushinteger_offset         , 0x100768CC
addr_offset lua_rawset_offset              , 0x100768C6

; Hook at 100764A0
jmp XMLParserHook
; Hook at 1000B492
jmp LuaTableHook
GetRelocBase:
    mov eax, text_base_addr
    ret

%macro XMLParserHook_LoadVariables 0
    mov edi, esi              ; this
    lea esi, [esp+0x18+8*4+4] ; name_node
    mov edi, [edi + 768]      ; connection
%endmacro
%define XMLParserHook_FailSafeRegister ecx
%define XMLParserHook_SuccSafeRegister ecx
%macro XMLParserHook_PatchInstructions 0
    lea edx, [esp+0x20-0x14]
    push edx
%endmacro

%macro LuaTableHook_LoadVariables 0
    mov edi, esi ; Lua state
    mov esi, ebx ; Lua table
    ret
%endmacro
%define LuaTableHook_SuccSafeRegister eax
%macro LuaTableHook_PatchInstructions 0
    add esp, 0x38
    pop edi
    pop esi
%endmacro

%include "core.s"