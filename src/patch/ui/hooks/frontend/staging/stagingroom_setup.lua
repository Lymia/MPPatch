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
    _mpPatch.hooks.protocol_chathandler_setupHooks()

    Events.GameMessageChat.Remove(OnChat)
    OnChat = _mpPatch.hooks.protocol_chathandler_new(OnChat, function(fromPlayer)
        return not not m_PlayerNames[fromPlayer]
    end)
    Events.GameMessageChat.Add(OnChat)

    _mpPatch.hookUpdate()
    local countdownRunning = false
    _mpPatch.addUpdateHook(function(...)
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
