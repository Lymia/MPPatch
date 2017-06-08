if _mpPatch_activateFrontEnd then
    _mpPatch.hooks.protocol_chathandler_setupHooks()

    OnChat = _mpPatch.hooks.protocol_chathandler_new(OnChat, function(fromPlayer)
        return not not m_PlayerNames[fromPlayer]
    end)

    _mpPatch.hookUpdate()
    local countdownRunning = false
    _mpPatch.addUpdateHook(function(...)
        if not ContextPtr:IsHidden() and countdownRunning then
            OnUpdate(...)
        end
    end)

    local OnDisconnectOld = OnDisconnect
    function OnDisconnect(...)
        if not ContextPtr:IsHidden() then
            _mpPatch.hooks.protocol_chathandler_onDisconnect(...)
        end
        return OnDisconnectOld(...)
    end
    Events.MultiplayerGamePlayerDisconnected.Remove(OnDisconnectOld)
    Events.MultiplayerGamePlayerDisconnected.Add(OnDisconnect)

    function StartCountdown()
        g_fCountdownTimer = 10
        countdownRunning = true
    end
    function StopCountdown()
        Controls.CountdownButton:SetHide(true)
        g_fCountdownTimer = -1
        countdownRunning = false
    end

    _mpPatch.addResetHook(function()
        _mpPatch.patch.NetPatch.reset()
        StopCountdown()
    end)
end
