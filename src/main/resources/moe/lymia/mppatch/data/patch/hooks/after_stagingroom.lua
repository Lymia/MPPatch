if _mpPatch and _mpPatch.enabled then
    local StartCountdownOld = StartCountdown
    function StartCountdown(...)
        if not Matchmaking.IsHost() then
            _mpPatch.overrideModsFromPreGame()
        end
        return StartCountdownOld(...)
    end

    local function cancelOverride()
        if not Matchmaking.IsHost() then
            print("Cancelling override.")
            patch.NetPatch.reset()
        end
    end

    local StopCountdownOld = StopCountdown
    function StopCountdown(...)
        cancelOverride()
        return StopCountdownOld(...)
    end

    local DequeuePopup = UIManager.DequeuePopup
    _mpPatch.patch.globals.rawset(UIManager, "DequeuePopup", function(this, ...)
        local context = ...
        if context == ContextPtr then cancelOverride() end
        return DequeuePopup(UIManager, ...)
    end)
end
