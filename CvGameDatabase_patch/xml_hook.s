import_symbol xml_check_label
import_symbol xml_get_contents
import_symbol Database_ExecuteMultiple
import_symbol Database_LogMessage

XMLParserHookCore:
        const eax, db, "__MOD2DLC_IGNORE", 0
        push eax
        const eax, dd, 16
        push eax
        mov ecx, esi
        call xml_check_label
        add esp, 8
        test al, al
        je .next
        jmp .continue
    .next:
        const eax, db, "__MOD2DLC_RAWSQL", 0
        push eax
        const eax, dd, 16
        push eax
        mov ecx, esi
        call xml_check_label
        add esp, 8
        test al, al
        je .cancel

        push 0
        mov eax, esi
        push 0
        mov ebx, esi
        push eax
        push ebx
        mov ecx, esi
        call xml_get_contents
        add esp, 8

        pop ebx ; contents
        pop eax ; length

        push eax
        push ebx
        mov ecx, edi
        call Database_ExecuteMultiple
        add esp, 8
        test al, al
        je .continue

        const eax, db, "Mod2DLC: Failed to execute statement while processing __MOD2DLC_RAWSQL tag.", 0
        push eax
        mov ecx, edi
        call Database_LogMessage
        add esp, 4

        jmp .fail
    .continue:
        mov eax, 1
        ret
    .cancel:
        mov eax, 0
        ret
    .fail:
        mov eax, 2
        ret
XMLParserHook:
        pushad
        pushfd

        XMLParserHook_LoadVariables
        call XMLParserHookCore

        mov ebx, 2
        cmp eax, ebx
        je .failExit

        mov ebx, 1
        cmp eax, ebx
        je .continueExit

        jmp .proceedExit

    .failExit:
        popfd
        popad
        mov eax, 0
        jmp .common
    .continueExit:
        popfd
        popad
        mov eax, 1
    .common:
        push xml_parser_hook_continue
        call BuildReturnAddress
        pop XMLParserHook_FailSafeRegister
        jmp XMLParserHook_FailSafeRegister

    .proceedExit:
        popfd
        popad
        push xml_parser_hook_return
        call BuildReturnAddress
        pop XMLParserHook_SuccSafeRegister

        XMLParserHook_PatchInstructions

        jmp XMLParserHook_SuccSafeRegister
