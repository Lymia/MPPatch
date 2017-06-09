if _mpPatch then
    local playerMap = {}

    function _mpPatch.hooks.protocol_kickunpached_installHooks()
        _mpPatch.addResetHook(function()
            playerMap = {}
        end)

        _mpPatch.net.clientIsPatched.registerHandler(function(_, playerID)
            if Matchmaking.IsHost() then
                playerMap[playerID] = nil
            end
        end)

        _mpPatch.event.kickAllUnpatched.registerHandler(function(reason)
            if Matchmaking.IsHost() then
                for player, _ in pairs(playerMap) do
                    _mpPatch.debugPrint("Kicking player "..player.." for (presumably) not having MPPatch. ("..reason..")")
                    Matchmaking.KickPlayer(player)
                    playerMap[player] = nil
                end
            end
        end)
    end

    function _mpPatch.hooks.protocol_kickunpached_onUpdate(timeDiff)
        if Matchmaking.IsHost() then
            for player, _ in pairs(playerMap) do
                playerMap[player] = playerMap[player] - timeDiff
                if playerMap[player] <= 0 then
                    _mpPatch.debugPrint("Kicking player "..player.." for (presumably) not having MPPatch.")
                    Matchmaking.KickPlayer(player)
                    playerMap[player] = nil
                end
            end
        end
    end

    function _mpPatch.hooks.protocol_kickunpached_onJoin(playerId, getPlayerName, isInGame)
        if Matchmaking.IsHost() then
            local header = ""
            if getPlayerName then
                local name = getPlayerName(playerId)
                if name then
                    header = "@"..tostring(name)..": "
                end
            end

            local website = _mpPatch.version.info["mppatch.website"] or "<unknown>"

            _mpPatch.net.skipNextChat(2)
            Network.SendChat(header..Locale.Lookup("TXT_KEY_MPPATCH_JOIN_WARNING_1_"..(isInGame and "INGAME" or "FRONTEND")))
            Network.SendChat(header..Locale.Lookup("TXT_KEY_MPPATCH_JOIN_WARNING_2")..website)

            playerMap[playerId] = 30
        end
    end

    function _mpPatch.hooks.protocol_kickunpached_onDisconnect(playerId)
        if Matchmaking.IsHost() then
            playerMap[playerId] = nil
        end
    end
end