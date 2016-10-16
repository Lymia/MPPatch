if _mpPatch and _mpPatch.enabled then
    local _MultiplayerGameLaunchedHook
    local _MultiplayerGameLaunchedHook_added
    local _MultiplayerGameLaunchedHook_remove = function()
        if _MultiplayerGameLaunchedHook_added then
            Event.MultiplayerGameLaunched.Remove(_MultiplayerGameLaunchedHook)
            _MultiplayerGameLaunchedHook_added = false
        end
    end
    function _MultiplayerGameLaunchedHook()
        if not Matchmaking.IsHost() then
            _mpPatch.overrideModsFromPreGame()
        end
        _MultiplayerGameLaunchedHook_remove()
    end
    Event.MultiplayerGameLaunched.Add(_MultiplayerGameLaunchedHook)
    _MultiplayerGameLaunchedHook_added = true
end
