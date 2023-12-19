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
    local RefreshGameOptionsOld = RefreshGameOptions
    function RefreshGameOptions()
        RefreshGameOptionsOld()

        -- find mod dependencies
        local dlcDependencies = _mpPatch.getModDependencies(Modding.GetActivatedMods())

        -- original code from mpgameoptions.lua
        g_DLCAllowedManager:ResetInstances()

        local canEditDLC = CanEditDLCGameOptions()
        for row in GameInfo.DownloadableContent() do
            local normId = _mpPatch.normalizeDlcName(row.PackageID)

            if row.IsBaseContentUpgrade == 0 then
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