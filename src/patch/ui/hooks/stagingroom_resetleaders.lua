if _mpPatch and _mpPatch.loaded and _mpPatch.isModding then
    local requestUpdate = _mpPatch.registerChatCommand("19c2eec6-a87e-11e6-b86e-03d52f957d20", function()
        if Matchmaking.IsHost() then
            Network.BroadcastPlayerInfo()
        end
    end)

    Events.SystemUpdateUI.Add(function (uiType, screen)
        if not ContextPtr:IsHidden() and not Matchmaking.IsHost() and
           uiType == SystemUpdateUIType.RestoreUI and screen == "StagingRoom" then
            _mpPatch.debugPrint("Requesting user info update.")
            requestUpdate()
        end
    end)
end
