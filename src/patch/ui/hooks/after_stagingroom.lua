if _mpPatch and _mpPatch.enabled and _mpPatch.isModding then
    local confirmChat = "mppatch:gamelaunchcountdown:WgUUz7wWuxWFmjY02WFS0mka53nFDzJvD00zmuQHKyJT2wNHuWZvrDcejHv5rTyl"

    local gameLaunchSet = false
    local gameLaunchCountdown = 3

    local StartCountdownOld = StartCountdown
    function StartCountdown(...)
        if gameLaunchSet then return end
        return StartCountdownOld(...)
    end

    local StopCountdownOld = StopCountdown
    function StopCountdown(...)
        if gameLaunchSet then return end
        return StopCountdownOld(...)
    end

    local HandleExitRequestOld = HandleExitRequest
    function HandleExitRequest(...)
        if gameLaunchSet then return end
        return HandleExitRequestOld(...)
    end

    local LaunchGameOld = LaunchGame
    local function LaunchGameCountdown(timeDiff)
        gameLaunchCountdown = gameLaunchCountdown - timeDiff
        if gameLaunchCountdown <= 0 then
            LaunchGameOld()
            ContextPtr:ClearUpdate()
        end
    end
    function LaunchGame(...)
        if PreGame.IsHotSeatGame() then
            return LaunchGameOld(...)
        else
            SendChat(confirmChat)
            ContextPtr:ClearUpdate()
            gameLaunchSet = true
            ContextPtr:SetUpdate(LaunchGameCountdown)
        end
    end
    Controls.LaunchButton:RegisterCallback(Mouse.eLClick, LaunchGame)

    local OnChatOld = OnChat
    function OnChat(...)
        local fromPlayer, toPlayer, text = ...
        if not Matchmaking.IsHost() and fromPlayer == m_HostID and text == confirmChat then
            gameLaunchSet = true
            _mpPatch.overrideModsFromPreGame()
            return
        end
        return OnChatOld(...)
    end

    local DequeuePopup = UIManager.DequeuePopup
    _mpPatch.patch.globals.rawset(UIManager, "DequeuePopup", function(this, ...)
        local context = ...
        if context == ContextPtr and not Matchmaking.IsHost() then
            _mpPatch.debugPrint("Cancelling override.")
            _mpPatch.patch.NetPatch.reset()
        end
        return DequeuePopup(UIManager, ...)
    end)
end
