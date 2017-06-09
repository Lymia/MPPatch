-- Copyright (c) 2015-2017 Lymia Alusyia <lymia@lymiahugs.com>
--
-- Permission is hereby granted, free of charge, to any person obtaining a copy
-- of this software and associated documentation files (the "Software"), to deal
-- in the Software without restriction, including without limitation the rights
-- to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
-- copies of the Software, and to permit persons to whom the Software is
-- furnished to do so, subject to the following conditions:
--
-- The above copyright notice and this permission notice shall be included in
-- all copies or substantial portions of the Software.
--
-- THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
-- IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
-- FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
-- AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
-- LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
-- OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
-- THE SOFTWARE.

if _mpPatch_activateFrontEnd then
    _mpPatch.setBIsModding()

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
            _mpPatch.overrideModsFromPreGame()
        end
    end)

    local HandleExitRequestOld = HandleExitRequest
    function HandleExitRequest(...)
        if gameLaunchSet then return end
        _mpPatch.resetUI()
        return HandleExitRequestOld(...)
    end

    local LaunchGameOld = LaunchGame
    _mpPatch.addUpdateHook(function(timeDiff)
        if not ContextPtr:IsHidden() and gameLaunchSet then
            gameLaunchCountdown = gameLaunchCountdown - timeDiff
            if gameLaunchCountdown <= 0 then
                _mpPatch.event.kickAllUnpatched("Game starting")
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
            _mpPatch.resetUI()
        end
        return DequeuePopup(this, ...)
    end)
end

