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
    local function checkTable(names, key)
        if #names > 0 then
            local messageTable = {Locale.Lookup(key)}
            for _, name in ipairs(names) do
                table.insert(messageTable, "[ICON_BULLET]"..name:gsub("%[", "("):gsub("%]", ")"))
            end
            joinFailed(table.concat(messageTable, "[NEWLINE]"))
            return true
        end
        return false
    end
    function OnConnectionCompete()
        if not Matchmaking.IsHost() then
            if _mpPatch.isModding then
                _mpPatch.net.clientIsPatched(_mpPatch.protocolVersion)

                local modList = _mpPatch.decodeModsList()

                local missingMods = {}
                _mpPatch.debugPrint("Enabled mods for room:")
                for _, mod in ipairs(modList) do
                    local missingText = ""
                    if not _mpPatch.isModInstalled(mod.ID, mod.Version) then
                        table.insert(missingMods, mod.Name)
                        missingText = " (is missing)"
                    end
                    _mpPatch.debugPrint("- "..mod.Name..missingText)
                end
                if checkTable(missingMods, "TXT_KEY_MPPATCH_MOD_MISSING") then return end

                local missingDlc = {}
                local dlcDependencies = _mpPatch.getModDependencies(modList)
                _mpPatch.debugPrint("DLC dependencies for room:")
                for k, v in pairs(dlcDependencies) do
                    local dlcName = Locale.Lookup("TXT_KEY_"..k.."_DESCRIPTION")
                    if #v > 0 and not ContentManager.IsInstalled(k) then
                        table.insert(missingDlc, dlcName)
                    end
                    _mpPatch.debugPrint("- "..dlcName..", dependency count = "..#v..", "..
                                        "is installed = "..tostring(ContentManager.IsInstalled(k)))
                end
                if checkTable(missingDlc, "TXT_KEY_MPPATCH_DLC_MISSING") then return end
            end

            enterLobby()
        end
    end
end