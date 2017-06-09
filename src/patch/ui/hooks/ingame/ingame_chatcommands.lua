-- Copyright (c) 2015-2016 Lymia Alusyia <lymia@lymiahugs.com>
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

if _mpPatch and _mpPatch.loaded and _mpPatch.isModding then
    _mpPatch.interceptGlobalWrite("OnUpdate", function(OnUpdate)
        _mpPatch.addUpdateHook(OnUpdate)
        return _mpPatch.onUpdate
    end)

    _mpPatch.hooks.protocol_resetleaders()
    _mpPatch.interceptChatFunction()

    _mpPatch.hooks.protocol_kickunpached_init(function(id)
        local player = Players[id]
        if player then
            return Players[id]:GetName()
        end
    end, true)
    _mpPatch.hooks.protocol_kickunpached_installHooks()

    _mpPatch.addUpdateHook(function(timeDiff)
        _mpPatch.hooks.protocol_kickunpached_onUpdate(timeDiff)
    end)

    local lastNotificationPlayer
    Events.NotificationAdded.Add(function(id, type, toolTip, strSummary, iGameValue, iExtraGameData, ePlayer)
        if type == NotificationTypes.NOTIFICATION_PLAYER_CONNECTING or
           type == NotificationTypes.NOTIFICATION_PLAYER_RECONNECTED then
            lastNotificationPlayer = ePlayer
        else
            lastNotificationPlayer = nil
        end
    end)
    local function checkLastNotificationPlayer(fn)
        return function()
            if lastNotificationPlayer == nil then
                _mpPatch.debugPrint("No last notification player ID available! Maybe using an unofficial game .dll?")
                return
            end
            fn(lastNotificationPlayer)
            lastNotificationPlayer = nil
        end
    end

    Events.MultiplayerHotJoinStarted.Add(checkLastNotificationPlayer(function(player)
        _mpPatch.hooks.protocol_kickunpached_onJoin(player)
    end))
    Events.MultiplayerGamePlayerDisconnected.Add(function(player)
        _mpPatch.hooks.protocol_kickunpached_onDisconnect(player)
    end)
    Events.MultiplayerHotJoinCompleted.Add(checkLastNotificationPlayer(function(player)
        _mpPatch.event.kickIfUnpatched(player, "Hotjoin completed")
    end))

end
