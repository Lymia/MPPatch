if _mpPatch and _mpPatch.enabled then
    Modding = _mpPatch.hookTable(Modding, {ActivateAllowedDLC = function(...)
        _mpPatch.overrideModsFromPreGame()
        return Modding._super.ActivateAllowedDLC(...)
    end})

    Matchmaking = _mpPatch.hookTable(Matchmaking, {LaunchMultiplayerGame = function(...)
        _mpPatch.overrideModsFromPreGame()
        return Matchmaking._super.LaunchMultiplayerGame(...)
    end})

    if _mpPatch.isModding then
        _mpPatch.setBIsModding()
    end

    local MultiplayerGameLaunchedHook_added = true
    local MultiplayerGameLaunchedHook_remove
    local function MultiplayerGameLaunchedHook()
        if not Matchmaking.IsHost() then
            _mpPatch.overrideModsFromPreGame()
        end
        MultiplayerGameLaunchedHook_remove()
    end
    MultiplayerGameLaunchedHook_remove = function()
        if MultiplayerGameLaunchedHook_added then
            Events.MultiplayerGameLaunched.Remove(MultiplayerGameLaunchedHook)
            MultiplayerGameLaunchedHook_added = false
        end
    end
    Events.MultiplayerGameLaunched.Add(MultiplayerGameLaunchedHook)

    UIManager = _mpPatch.hookTable(UIManager, {
        DequeuePopup = function(...)
            local context = ...
            if context == ContextPtr then
                MultiplayerGameLaunchedHook_remove()
            end
            return UIManager._super.DequeuePopup(...)
        end,
        CData = UIManager.CData
    })
end
