if _mpPatch_activateFrontEnd then
    _mpPatch.hooks.protocol_resetleaders()

    Events.SystemUpdateUI.Add(function (uiType, screen)
        if not ContextPtr:IsHidden() and not Matchmaking.IsHost() and
           uiType == SystemUpdateUIType.RestoreUI and screen == "StagingRoom" then
            _mpPatch.debugPrint("Requesting user info update.")
            _mpPatch.net.sendPlayerData()
        end
    end)
end
