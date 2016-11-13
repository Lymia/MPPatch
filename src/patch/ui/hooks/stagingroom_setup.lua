if _mpPatch and _mpPatch.loaded and _mpPatch.isModding then
    OnChat = _mpPatch.interceptChatFunction(OnChat, function(fromPlayer)
        return not not m_PlayerNames[fromPlayer]
    end)
    _mpPatch.hookUpdate()

    local countdownRunning = false
    _mpPatch.addUpdateHook(function(...)
        if not ContextPtr:IsHidden() and countdownRunning then
            OnUpdate(...)
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

    _mpPatch.addResetHook(function()
        _mpPatch.patch.NetPatch.reset()
        StopCountdown()
    end)
end
