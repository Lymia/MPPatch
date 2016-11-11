if _mpPatch and _mpPatch.loaded and _mpPatch.isModding then
    Events.SystemUpdateUI.Add(function (uiType, screen)
        if not ContextPtr:IsHidden() and uiType == SystemUpdateUIType.RestoreUI and screen == "StagingRoom" then
            _mpPatch.debugPrint("Executing OnPreGameDirty.")
            OnPreGameDirty()
        end
    end)

    function ValidateCivSelections()
        -- do nothing
    end

    PreGame = _mpPatch.hookTable(PreGame, {GetCivilization = function(...)
        local civ = PreGame._super.GetCivilization(...)
        if civ ~= -1 and not GameInfo.Civilizations[civ] then
            return -1
        end
        return civ
    end})
end
