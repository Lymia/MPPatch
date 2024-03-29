-- Copyright (c) 2015-2023 Lymia Kanokawa <lymia@lymia.moe>
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
        _mpPatch.event.update.registerHandler(OnUpdate)
        local event = _mpPatch.event.update
        return function(...) return event(...) end
    end)

    _mpPatch.hooks.protocol_resetleaders()
    Events.GameMessageChat.Add(_mpPatch.interceptChatFunction())

    _mpPatch.hooks.protocol_kickunpached_init(function(id)
        local player = Players[id]
        if player then
            return Players[id]:GetName()
        end
    end, true)
    _mpPatch.hooks.protocol_kickunpached_installHooks()

    _mpPatch.event.update.registerHandler(function(timeDiff)
        _mpPatch.hooks.protocol_kickunpached_onUpdate(timeDiff)
    end)

    Events.ConnectedToNetworkHost.Add(function(player)
        _mpPatch.hooks.protocol_kickunpached_onJoin(player)
    end)
    Events.MultiplayerGamePlayerDisconnected.Add(function(player)
        _mpPatch.hooks.protocol_kickunpached_onDisconnect(player)
    end)

    Events.MultiplayerHotJoinStarted.Add(function(player)
        _mpPatch.event.kickAllUnpatched("Hotjoin started")
    end)
    Events.MultiplayerHotJoinCompleted.Add(function(player)
        _mpPatch.event.kickAllUnpatched("Hotjoin completed")
    end)
end
