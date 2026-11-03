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
