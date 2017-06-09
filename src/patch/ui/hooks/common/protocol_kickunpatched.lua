if _mpPatch and _mpPatch.loaded then
    local playerMap = {}

    local getPlayerName, joinWarning1Ending
    function _mpPatch.hooks.protocol_kickunpached_init(pGetPlayerName, pIsInGame)
        getPlayerName = pGetPlayerName
        joinWarning1Ending = Locale.Lookup(isInGame and "TXT_KEY_MPPATCH_JOIN_WARNING_1_INGAME"
                                                    or  "TXT_KEY_MPPATCH_JOIN_WARNING_1_STAGING")
    end

    local website = _mpPatch.version.info["mppatch.website"] or "<unknown>"
    local function getHeader(playerId)
        local header = ""
        if getPlayerName then
            local name = getPlayerName(playerId)
            if name then
                header = "@"..tostring(name)..": "
            end
        end
        return header
    end

    function _mpPatch.hooks.protocol_kickunpached_installHooks()
        _mpPatch.addResetHook(function()
            playerMap = {}
        end)

        _mpPatch.net.clientIsPatched.registerHandler(function(protocolVersion, playerID)
            if Matchmaking.IsHost() then
                if protocolVersion == _mpPatch.protocolVersion then
                    playerMap[playerID] = nil
                else
                    local header = getHeader(playerId)

                    _mpPatch.skipNextChatIfVersion(_mpPatch.protocolVersion)
                    Network.SendChat(header..Locale.Lookup("TXT_KEY_MPPATCH_JOIN_WARNING_1_OUTDATED").." "..
                                     joinWarning1Ending)

                    _mpPatch.skipNextChatIfVersion(_mpPatch.protocolVersion)
                    Network.SendChat(header..Locale.Lookup("TXT_KEY_MPPATCH_JOIN_WARNING_2")..website)
                end
            end
        end)

        local function checkPlayerId(player, reason)
            if playerMap[player] then
                _mpPatch.debugPrint("Kicking player "..player.." for (presumably) not having MPPatch. ("..reason..")")
                Matchmaking.KickPlayer(player)
                playerMap[player] = nil
            end
        end

        _mpPatch.event.kickIfUnpatched.registerHandler(function(player, reason)
            if Matchmaking.IsHost() then
                checkPlayerId(player, reason)
            end
        end)

        _mpPatch.event.kickAllUnpatched.registerHandler(function(reason)
            if Matchmaking.IsHost() then
                for player, _ in pairs(playerMap) do
                    checkPlayerId(player, reason)
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

    function _mpPatch.hooks.protocol_kickunpached_onJoin(playerId)
        if Matchmaking.IsHost() then
            local header = getHeader(playerId)

            _mpPatch.net.skipNextChat(2)
            Network.SendChat(header..Locale.Lookup("TXT_KEY_MPPATCH_JOIN_WARNING_1_NOT_INSTALLED").." "..
                             joinWarning1Ending)
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