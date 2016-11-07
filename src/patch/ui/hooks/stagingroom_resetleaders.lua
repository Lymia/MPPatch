if _mpPatch and _mpPatch.loaded and _mpPatch.isModding then
    local function OnSystemUpdateUI(uiType, screen)
        if not ContextPtr:IsHidden() and uiType == SystemUpdateUIType.RestoreUI and screen == "StagingRoom" then
            RefreshPlayerList()
        end
    end
    Events.GameMessageChat.Add(OnSystemUpdateUI)
end

