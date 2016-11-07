if _mpPatch and _mpPatch.loaded then
    Modding = _mpPatch.hookTable(Modding, {ActivateAllowedDLC = function(...)
        _mpPatch.overrideModsFromPreGame()
        return Modding._super.ActivateAllowedDLC(...)
    end})

    local function enterLobby()
        UIManager:QueuePopup(Controls.StagingRoomScreen, PopupPriority.StagingScreen)
        UIManager:DequeuePopup(ContextPtr)
    end
    local function joinFailed(message)
        Events.FrontEndPopup.CallImmediate(message)
        g_joinFailed = true
        Matchmaking.LeaveMultiplayerGame()
        UIManager:DequeuePopup(ContextPtr)
    end
    function OnConnectionCompete()
        if not Matchmaking.IsHost() then
            if _mpPatch.isModding then
                local missingModList = {}
                _mpPatch.debugPrint("Enabled mods for room:")
                for _, mod in ipairs(_mpPatch.decodeModsList()) do
                    local missingText = ""
                    if not Modding.IsModInstalled(mod.ID, mod.Version) then
                        table.insert(missingModList, mod.Name)
                        missingText = " (is missing)"
                    end
                    _mpPatch.debugPrint("- "..mod.Name..missingText)
                end
                -- TODO: Check for DLCs/mod compatibility
                if #missingModList > 0 then
                    local messageTable = {Locale.Lookup("TXT_KEY_MPPATCH_MOD_MISSING")}
                    for _, name in ipairs(missingModList) do
                        table.insert(messageTable, "[ICON_BULLET]"..v.Name:gsub("%[", "("):gsub("%]", ")"))
                    end
                    joinFailed(table.concat(messageTable, "[NEWLINE]"))
                    return
                end

                _mpPatch.debugPrint("Activating mods and DLC...")
                Modding.ActivateAllowedDLC()
            end

            enterLobby()
        end
    end
end