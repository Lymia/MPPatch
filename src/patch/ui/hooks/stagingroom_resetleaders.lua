if _mpPatch and _mpPatch.loaded and _mpPatch.isModding then
    local function OnSystemUpdateUI(uiType, screen)
        if not ContextPtr:IsHidden() and uiType == SystemUpdateUIType.RestoreUI and screen == "StagingRoom" then
            _mpPatch.debugPrint("Executing OnPreGameDirty.")
            OnPreGameDirty()
        end
    end
    Events.GameMessageChat.Add(OnSystemUpdateUI)
end

