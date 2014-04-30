import_symbol lua_pushstring
import_symbol lua_pushinteger
import_symbol lua_rawset

LuaTableHookCore:
    const eax, db, "_mod2dlc_marker", 0
    push eax
    push edi
    call lua_pushstring
    add esp, 8
    push 0
    push edi
    call lua_pushinteger
    add esp, 8
    push esi
    push edi
    call lua_rawset
    add esp, 8
    ret
LuaTableHook:
    pushad
    pushfd

    XMLParserHook_LoadVariables
    call LuaTableHookCore

    popfd
    popad

    push xml_parser_hook_return
    call BuildReturnAddress
    pop LuaTableHook_SuccSafeRegister

    LuaTableHook_PatchInstructions

    jmp LuaTableHook_SuccSafeRegister
