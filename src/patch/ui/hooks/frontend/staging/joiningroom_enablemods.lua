-- Copyright (c) 2015-2023 Lymia Kanokawa <lymia@lymia.moe>
--
-- Permission is hereby granted, free of charge, to any person obtaining a copy
-- of this software and associated documentation files (the "Software"), to deal
-- in the Software without restriction, including without limitation the rights
-- to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
-- copies of the Software, and to permit persons to whom the Software is
-- furnished to do so, subject to the following conditions:
--
-- The above copyright notice and this permission notice shall be included in
-- all copies or substantial portions of the Software.
--
-- THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
-- IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
-- FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
-- AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
-- LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
-- OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
-- THE SOFTWARE.

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

    local areEventsActive, startTimer = false, nil
    _mpPatch.hookUpdate()
    _mpPatch.event.update.registerHandler(function(timeDiff)
        if areEventsActive and startTimer then
            startTimer = startTimer - timeDiff
            if startTimer <= 0 then
                startTimer = nil
                enterLobby()
            end
        end
    end)

    local RegisterEventsOld = RegisterEvents
    function RegisterEvents(...)
        areEventsActive = true
        return RegisterEventsOld(...)
    end

    local UnregisterEventsOld = UnregisterEvents
    function UnregisterEvents(...)
        areEventsActive = false
        return UnregisterEventsOld(...)
    end

    function OnConnectionCompete()
        if not Matchmaking.IsHost() then
            if _mpPatch.isModding then
                if not _mpPatch.isSupportedVersion then
                    joinFailed(Locale.Lookup("TXT_KEY_MPPATCH_SERIALIZATION_VERSION_ERROR_ROOM"))
                    return
                end

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

                startTimer = 2 -- Delay enterLobby() call to try and give the client time to send clientIsPatched
            else
                enterLobby()
            end
        end
    end
end