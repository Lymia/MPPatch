if _mpPatch and _mpPatch.enabled and _mpPatch.isModding then
    local startLock = false

    local StartCountdownOld = StartCountdown
    function StartCountdown(...)
        if not Matchmaking.IsHost() then
            _mpPatch.overrideModsFromPreGame()
        end
        return StartCountdownOld(...)
    end

    local function cancelOverride(force)
        if not Matchmaking.IsHost() and (force or not startLock) then
            _mpPatch.debugPrint("Cancelling override.")
            _mpPatch.patch.NetPatch.reset()
            startLock = false
        end
    end

    local StopCountdownOld = StopCountdown
    function StopCountdown(...)
        cancelOverride()
        return StopCountdownOld(...)
    end

    local OnUpdateOld = OnUpdate
    function OnUpdate(...)
        local fDTime = ...
        local g_fCountdownTimer = g_fCountdownTimer - fDTime
        if Network.IsEveryoneConnected() and g_fCountdownTimer <= 1 then
            _mpPatch.debugPrint("Locking cancel for launch.")
            startLock = true
        end
        return OnUpdateOld(...)
    end

    local OnPreGameDirtyOld = OnPreGameDirty
    function OnPreGameDirty(...)
        if not ContextPtr:IsHidden() then
            cancelOverride(true)
        end
        return OnPreGameDirtyOld(...)
    end
    Events.PreGameDirty.Add( OnPreGameDirty );

    local DequeuePopup = UIManager.DequeuePopup
    _mpPatch.patch.globals.rawset(UIManager, "DequeuePopup", function(this, ...)
        local context = ...
        if context == ContextPtr then
            _mpPatch.debugPrint("DequeuePopup from StagingRoom")
            cancelOverride()
        end
        return DequeuePopup(UIManager, ...)
    end)

    -- Disable manual launch, helps with countdown related things.
    Controls.LaunchButton:RegisterCallback(Mouse.eLClick, function() end)
end
