if _mpPatch and _mpPatch.loaded then
    _mpPatch.loadElementFromProxy("mppatch_multiplayerproxy", "ModMultiplayerSelectScreen")

    local function getModMessage(name, title)
        local mods = {}
        for _, v in ipairs(Modding.GetActivatedMods()) do
            if Modding.GetModProperty(v.ID, v.Version, name) ~= "1" then
                table.insert(mods, "[ICON_BULLET]".._mpPatch.getModName(v.ID, v.Version))
            end
        end
        if #mods == 0 then
            return nil
        else
            return Locale.Lookup(title).."[NEWLINE]"..table.concat(mods, "[NEWLINE]")
        end
    end
    local function setTooltip(control, name, title)
        control:SetToolTipString(getModMessage(name, title))
    end
    local function showMessage(name, title)
        local result = getModMessage(name, title)
        if result then
            Events.FrontEndPopup.CallImmediate(Locale.Lookup("TXT_KEY_MPPATCH_WARNING_PREFIX")..result)
        end
    end

    _mpPatch.hookGlobalFunction("OnSinglePlayerClick", function()
        showMessage("SupportsSinglePlayer", "TXT_KEY_MPPATCH_NO_SINGLEPLAYER_SUPPORT")
    end)
    _mpPatch.replaceGlobalFunction("OnMultiPlayerClick", function()
        showMessage("SupportsMultiplayer" , "TXT_KEY_MPPATCH_NO_MULTIPLAYER_SUPPORT" )
        UIManager:QueuePopup(Controls.ModMultiplayerSelectScreen, PopupPriority.ModMultiplayerSelectScreen)
    end)

    local function onShowHide()
        setTooltip(Controls.SinglePlayerButton, "SupportsSinglePlayer", "TXT_KEY_MPPATCH_NO_SINGLEPLAYER_SUPPORT")
        setTooltip(Controls.MultiPlayerButton , "SupportsMultiplayer" , "TXT_KEY_MPPATCH_NO_MULTIPLAYER_SUPPORT" )
        Controls.MultiPlayerButton:SetHide(false)
    end
    Modding = _mpPatch.hookTable(Modding, {
        AllEnabledModsContainPropertyValue = function(...)
            local name = ...
            if name == "SupportsSinglePlayer" then
                onShowHide()
                return true
            elseif name == "SupportsMultiplayer" then
                return true
            end
            return Modding._super.AllEnabledModsContainPropertyValue(...)
        end
    })
end