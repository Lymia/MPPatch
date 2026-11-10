if _mpPatch_activateFrontEnd then
    _mpPatch.hooks.protocol_chathandler_setupHooks()

    Events.GameMessageChat.Remove(OnChat)
    OnChat = _mpPatch.hooks.protocol_chathandler_new(OnChat, function(fromPlayer)
        return not not m_PlayerNames[fromPlayer]
    end)
    Events.GameMessageChat.Add(OnChat)

    _mpPatch.net.clientIsPatched(_mpPatch.protocolVersion)

    _mpPatch.hookUpdate()
    local countdownRunning = false
    _mpPatch.event.update.registerHandler(function(...)
        if not ContextPtr:IsHidden() and countdownRunning then
            OnUpdate(...)
        end
    end)

    Events.MultiplayerGamePlayerDisconnected.Add(function(...)
        if not ContextPtr:IsHidden() then
            _mpPatch.hooks.protocol_chathandler_onDisconnect(...)
        end
    end)

    function StartCountdown()
        g_fCountdownTimer = 10
        countdownRunning = true
    end

    function StopCountdown()
        Controls.CountdownButton:SetHide(true)
        g_fCountdownTimer = -1
        countdownRunning = false
    end

    _mpPatch.event.reset.registerHandler(function()
        _mpPatch.patch.NetPatch.reset()
        StopCountdown()
    end)
end
