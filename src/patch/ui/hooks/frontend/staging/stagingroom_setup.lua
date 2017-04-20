if _mpPatch_activateFrontEnd then
    local skipNextLine = {}

    OnChat = _mpPatch.interceptChatFunction(OnChat, function(fromPlayer)
        return not not m_PlayerNames[fromPlayer]
    end, function(fromPlayer)
        if skipNextLine[fromPlayer] then
            skipNextLine[fromPlayer] = nil
            return false
        else
            return true
        end
    end)
    _mpPatch.hookUpdate()

    local countdownRunning = false
    _mpPatch.addUpdateHook(function(...)
        if not ContextPtr:IsHidden() and countdownRunning then
            OnUpdate(...)
        end
    end)

    _mpPatch.net.skipNextChat.registerHandler(function(_, fromPlayer)
        skipNextLine[fromPlayer] = true
    end)

    local OnDisconnectOld = OnDisconnect
    function OnDisconnect(...)
        if not ContextPtr:IsHidden() and Matchmaking.IsHost() then
            local playerID = ...
            skipNextLine[playerID] = nil
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
