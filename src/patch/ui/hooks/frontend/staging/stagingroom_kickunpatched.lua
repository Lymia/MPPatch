if _mpPatch_activateFrontEnd then
    _mpPatch.hooks.protocol_kickunpached_init(function(playerId)
        return m_PlayerNames[playerId]
    end)
    _mpPatch.hooks.protocol_kickunpached_installHooks()

    _mpPatch.addUpdateHook(function(timeDiff)
        if not ContextPtr:IsHidden() then
            _mpPatch.hooks.protocol_kickunpached_onUpdate(timeDiff)
        end
    end)

    Events.ConnectedToNetworkHost.Add(function(playerId)
        if not ContextPtr:IsHidden() then
            _mpPatch.hooks.protocol_kickunpached_onJoin(playerId)
        end
    end)

    Events.MultiplayerGamePlayerDisconnected.Add(function(playerId)
        if not ContextPtr:IsHidden() then
            _mpPatch.hooks.protocol_kickunpached_onDisconnect(playerId)
        end
    end)
end
