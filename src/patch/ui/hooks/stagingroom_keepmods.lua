if _mpPatch and _mpPatch.loaded and _mpPatch.isModding then
    _mpPatch.setBIsModding()

    -- Hook simple modding functions.
    Modding = _mpPatch.hookTable(Modding, {ActivateAllowedDLC = function(...)
        _mpPatch.overrideModsFromPreGame()
        return Modding._super.ActivateAllowedDLC(...)
    end})

    Matchmaking = _mpPatch.hookTable(Matchmaking, {LaunchMultiplayerGame = function(...)
        _mpPatch.overrideModsFromPreGame()
        return Matchmaking._super.LaunchMultiplayerGame(...)
    end})

    -- Protocol for ensuring non-host players will override the mod list.
    local gameLaunchSet = false
    local gameLaunchCountdown = -1

    local function setGameLaunch()
        gameLaunchSet = true
        gameLaunchCountdown = 3
    end
    _mpPatch.addResetHook(function()
        gameLaunchSet = false
    end)

    _mpPatch.net.startLaunchCountdown.registerHandler(function(_, id)
        if id == m_HostID and not Matchmaking.IsHost() then
            setGameLaunch()
            _mpPatch.overrideModsFromPreGame()
        end
    end)

    local function doReset()
        _mpPatch.resetUI()
        _mpPatch.patch.NetPatch.reset()
    end

    local HandleExitRequestOld = HandleExitRequest
    function HandleExitRequest(...)
        if gameLaunchSet then return end
        doReset()
        return HandleExitRequestOld(...)
    end

    local LaunchGameOld = LaunchGame
    _mpPatch.addUpdateHook(function(timeDiff)
        if not ContextPtr:IsHidden() and gameLaunchSet then
            gameLaunchCountdown = gameLaunchCountdown - timeDiff
            if gameLaunchCountdown <= 0 then
                LaunchGameOld()
                _mpPatch.resetUI()
            end
            return true
        end
    end, -1)
    function LaunchGame(...)
        if PreGame.IsHotSeatGame() then
            return LaunchGameOld(...)
        else
            _mpPatch.net.startLaunchCountdown()
            setGameLaunch()
        end
    end
    Controls.LaunchButton:RegisterCallback(Mouse.eLClick, LaunchGame)

    -- Ensure the NetPatch hook doesn't end up escaping the UI.
    local DequeuePopup = UIManager.DequeuePopup
    _mpPatch.patch.globals.rawset(UIManager, "DequeuePopup", function(this, ...)
        local context = ...
        if context == ContextPtr then
            doReset()
        end
        return DequeuePopup(this, ...)
    end)
end

