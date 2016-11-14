if _mpPatch_activateFrontEnd then
    _mpPatch.net.sendPlayerData.registerHandler(function()
        if Matchmaking.IsHost() then
            Network.BroadcastPlayerInfo()
        end
    end)

    Events.SystemUpdateUI.Add(function (uiType, screen)
        if not ContextPtr:IsHidden() and not Matchmaking.IsHost() and
           uiType == SystemUpdateUIType.RestoreUI and screen == "StagingRoom" then
            _mpPatch.debugPrint("Requesting user info update.")
            _mpPatch.net.sendPlayerData()
        end
    end)
end
