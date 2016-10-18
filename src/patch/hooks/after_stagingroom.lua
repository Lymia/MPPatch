if _mpPatch and _mpPatch.enabled and _mpPatch.isModding then
    local startLock = false

    local StartCountdownOld = StartCountdown
    function StartCountdown(...)
        if not Matchmaking.IsHost() then
            _mpPatch.overrideModsFromPreGame()
        end
        return StartCountdownOld(...)
    end

    local function cancelOverride()
        if not Matchmaking.IsHost() then
            _mpPatch.debugPrint("Cancelling override.")
            _mpPatch.patch.NetPatch.reset()
        end
    end

    local function OnPreGameDirtyHook(...)
        if not ContextPtr:IsHidden() then
            _mpPatch.debugPrint("cancelOverride from OnPreGameDirty")
            cancelOverride()
        end
        return OnPreGameDirtyOld(...)
    end
    Events.PreGameDirty.Add(OnPreGameDirtyHook)

    local DequeuePopup = UIManager.DequeuePopup
    _mpPatch.patch.globals.rawset(UIManager, "DequeuePopup", function(this, ...)
        local context = ...
        if context == ContextPtr then
            _mpPatch.debugPrint("cancelOverride from DequeuePopup")
            cancelOverride()
        end
        return DequeuePopup(UIManager, ...)
    end)

    -- Disable manual launch, helps with countdown related things.
    Controls.LaunchButton:RegisterCallback(Mouse.eLClick, function() end)
end
