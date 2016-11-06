if _mpPatch and _mpPatch.canEnable then
    local RefreshGameOptionsOld = RefreshGameOptions
    function RefreshGameOptions()
        RefreshGameOptionsOld()

        -- find mod dependencies
        local dlcDependencies = _mpPatch.getModDependencies(Modding.GetActivatedMods())
        local mpPatchUUID = _mpPatch.normalizeDlcName(_mpPatch.uuid)

        -- original code from mpgameoptions.lua
        g_DLCAllowedManager:ResetInstances()

        local canEditDLC = CanEditDLCGameOptions()
        for row in GameInfo.DownloadableContent() do
            local normId = _mpPatch.normalizeDlcName(row.PackageID)

            if normId == mpPatchUUID then
                PreGame.SetDLCAllowed(row.PackageID, _mpPatch.areModsEnabled)
            elseif row.IsBaseContentUpgrade == 0 then
                local dlcEntries = g_DLCAllowedManager:GetInstance()
                local dlcEntryDisabled = not canEditDLC

                local dlcButton = dlcEntries.GameOptionRoot:GetTextButton()
                dlcButton:LocalizeAndSetText(row.FriendlyNameKey)

                if dlcDependencies[normId] and #dlcDependencies[normId] > 0 then
                    dlcEntryDisabled = true
                    local tooltipTable = {Locale.Lookup("TXT_KEY_MPPATCH_CANNOT_BE_DISABLED")}
                    for _, v in ipairs(dlcDependencies[normId]) do
                        table.insert(tooltipTable, "[ICON_BULLET]"..v.Name:gsub("%[", "("):gsub("%]", ")"))
                    end
                    dlcEntries.GameOptionRoot:SetToolTipString(table.concat(tooltipTable, "[NEWLINE]"))
                    PreGame.SetDLCAllowed(row.PackageID, true)
                end

                dlcEntries.GameOptionRoot:SetCheck(PreGame.IsDLCAllowed(row.PackageID))
                dlcEntries.GameOptionRoot:SetDisabled(dlcEntryDisabled)

                dlcEntries.GameOptionRoot:RegisterCheckHandler(function(bCheck)
                    PreGame.SetDLCAllowed(row.PackageID, bCheck)
                    SendGameOptionChanged()
                end)
            else
                PreGame.SetDLCAllowed(row.PackageID, true)
            end
        end

        for _, v in ipairs({Controls.CityStateStack, Controls.DropDownOptionsStack, Controls.VictoryConditionsStack,
                            Controls.MaxTurnStack, Controls.TurnTimerStack, Controls.GameOptionsStack,
                            Controls.GameOptionsFullStack, Controls.DLCAllowedStack}) do
            v:CalculateSize()
            v:ReprocessAnchoring()
        end

        Controls.OptionsScrollPanel:CalculateInternalSize()
    end
end