if _mpPatch and _mpPatch.loaded then
    _mpPatch.loadElementFromProxy("mppatch_multiplayerproxy", "ModMultiplayerSelectScreen")

    Controls.MultiPlayerButton:RegisterCallback(Mouse.eLClick, function()
        UIManager:QueuePopup(Controls.ModMultiplayerSelectScreen, PopupPriority.ModMultiplayerSelectScreen)
    end)
    Controls.MultiPlayerButton:SetHide(false)

    local function setIncompatible(control, name, title)
        local mods = {}
        for _, v in ipairs(Modding.GetActivatedMods()) do
            if Modding.GetModProperty(v.ID, v.Version, name) ~= 1 then
                table.insert(mods, "[BULLET]".._mpPatch.getModName(v.ID, v.Version))
            end
        end
        if #mods == 0 then
            control:SetToolTipString(nil)
        else
            control:SetToolTipString(Locale.Lookup(title).."[NEWLINE]"..table.concat(mods, "[NEWLINE]"))
        end
    end
    local function onShowHideHook()
        setIncompatible(Controls.SinglePlayerButton, "SupportsSinglePlayer", "TXT_KEY_MPPATCH_NO_SINGLEPLAYER_SUPPORT")
        setIncompatible(Controls.MultiPlayerButton , "SupportsMultiplayer" , "TXT_KEY_MPPATCH_NO_MULTIPLAYER_SUPPORT" )
    end
    Modding = _mpPatch.hookTable(Modding, {
        AllEnabledModsContainPropertyValue = function(...)
            local name = ...
            if name == "SupportsSinglePlayer" then
                onShowHideHook()
                return true
            elseif name == "SupportsMultiplayer" then
                return true
            end
            return Modding._super.AllEnabledModsContainPropertyValue(...)
        end
    })
end