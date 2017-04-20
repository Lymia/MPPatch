if _mpPatch_activateFrontEnd then
    local playerMap = {}
    _mpPatch.addResetHook(function()
        playerMap = {}
    end)

    _mpPatch.addUpdateHook(function(timeDiff)
        if not ContextPtr:IsHidden() and Matchmaking.IsHost() then
            for player, _ in pairs(playerMap) do
                playerMap[player] = playerMap[player] - timeDiff
                if playerMap[player] <= 0 then
                    _mpPatch.debugPrint("Kicking player "..player.." for (presumably) not having MPPatch.")
                    Matchmaking.KickPlayer(player)
                    playerMap[player] = nil
                end
            end
        end
    end)

    _mpPatch.event.kickAllUnpatched.registerHandler(function()
        for player, _ in pairs(playerMap) do
            _mpPatch.debugPrint("Kicking player "..player.." for (presumably) not having MPPatch. (Game starting)")
            Matchmaking.KickPlayer(player)
            playerMap[player] = nil
        end
    end)

    _mpPatch.net.clientIsPatched.registerHandler(function(_, playerID)
        if Matchmaking.IsHost() then
            playerMap[playerID] = nil
        end
    end)

    local OnConnectOld = OnConnect
    function OnConnect(...)
        if not ContextPtr:IsHidden() and Matchmaking.IsHost() then
            local playerId = ...
            _mpPatch.net.skipNextChat()

            local header = ""
            if m_PlayerNames[playerId] then
                header = "@"..tostring(m_PlayerNames[playerId])..": "
            end

            Network.SendChat(header..Locale.Lookup("TXT_KEY_MPPATCH_JOIN_WARNING"))
            playerMap[playerId] = 30
        end
        return OnConnectOld(...)
    end
    Events.ConnectedToNetworkHost.Remove(OnConnectOld)
    Events.ConnectedToNetworkHost.Add(OnConnect)

    local OnDisconnectOld = OnDisconnect
    function OnDisconnect(...)
        if not ContextPtr:IsHidden() and Matchmaking.IsHost() then
            local playerID = ...
            playerMap[playerID] = nil
        end
        return OnDisconnectOld(...)
    end
    Events.MultiplayerGamePlayerDisconnected.Remove(OnDisconnectOld)
    Events.MultiplayerGamePlayerDisconnected.Add(OnDisconnect)
end
