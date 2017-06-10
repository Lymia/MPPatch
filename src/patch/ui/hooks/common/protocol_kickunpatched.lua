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

if _mpPatch and _mpPatch.loaded then
    local kickTimer  = {}
    local isPatched  = {}
    local isOutdated = {}

    local getPlayerName, warning1TxtKey
    function _mpPatch.hooks.protocol_kickunpached_init(pGetPlayerName, pIsInGame)
        getPlayerName = pGetPlayerName
        warning1TxtKey = pIsInGame and "TXT_KEY_MPPATCH_JOIN_WARNING_1_INGAME"
                                   or  "TXT_KEY_MPPATCH_JOIN_WARNING_1_STAGING"
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
    local function warnPlayer(playerId)
        local header = getHeader(playerId)
        local timer = math.floor(kickTimer[playerId] + 0.5)

        local joinWarning1Ending = Locale.ConvertTextKey(warning1TxtKey, timer)

        if isOutdated[playerId] then
            _mpPatch.net.skipNextChatIfVersion(_mpPatch.protocolVersion)
            Network.SendChat(header..Locale.Lookup("TXT_KEY_MPPATCH_JOIN_WARNING_1_OUTDATED").." "..
                             joinWarning1Ending)

            _mpPatch.net.skipNextChatIfVersion(_mpPatch.protocolVersion)
        else
            _mpPatch.net.skipNextChat(2)
            Network.SendChat(header..Locale.Lookup("TXT_KEY_MPPATCH_JOIN_WARNING_1_NOT_INSTALLED").." "..
                             joinWarning1Ending)
        end
        Network.SendChat(header..Locale.ConvertTextKey("TXT_KEY_MPPATCH_JOIN_WARNING_2", website))
    end

    function _mpPatch.hooks.protocol_kickunpached_installHooks()
        _mpPatch.event.reset.registerHandler(function()
            kickTimer  = {}
            isPatched  = {}
            isOutdated = {}
        end)

        _mpPatch.net.clientIsPatched.registerHandler(function(protocolVersion, playerId)
            if Matchmaking.IsHost() then
                if protocolVersion == _mpPatch.protocolVersion then
                    kickTimer[playerId] = nil
                    isPatched[playerId] = true
                else
                    isOutdated[playerId] = true
                end
            end
        end)

        local function checkPlayerId(player, reason)
            if kickTimer[player] then
                _mpPatch.debugPrint("Kicking player "..player.." for (presumably) not having MPPatch. ("..reason..")")
                Matchmaking.KickPlayer(player)
                kickTimer[player] = nil
            end
        end

        _mpPatch.event.kickIfUnpatched.registerHandler(function(player, reason)
            if Matchmaking.IsHost() then
                checkPlayerId(player, reason)
            end
        end)

        _mpPatch.event.kickAllUnpatched.registerHandler(function(reason)
            if Matchmaking.IsHost() then
                for player, _ in pairs(kickTimer) do
                    checkPlayerId(player, reason)
                end
            end
        end)
    end

    local lastKickTimer = {}
    function _mpPatch.hooks.protocol_kickunpached_onUpdate(timeDiff)
        if Matchmaking.IsHost() then
            for player, _ in pairs(kickTimer) do
                kickTimer[player] = kickTimer[player] - timeDiff

                local kickTimerIncrement = math.floor( kickTimer    [player]          / 5)
                local lastKickIncrement  = math.floor((lastKickTimer[player] or 1000) / 5)

                if kickTimer[player] <= 0 then
                    _mpPatch.debugPrint("Kicking player "..player.." for (presumably) not having MPPatch.")
                    Matchmaking.KickPlayer(player)
                    kickTimer[player] = nil
                elseif kickTimerIncrement < lastKickIncrement then
                    warnPlayer(player)
                    lastKickTimer[player] = kickTimer[player]
                end
            end
        end
    end

    function _mpPatch.hooks.protocol_kickunpached_onJoin(playerId)
        if Matchmaking.IsHost() and not kickTimer[playerId] and not isPatched[playerId] then
            local header = getHeader(playerId)
            kickTimer[playerId] = 30
            lastKickTimer[playerId] = 1000
        end
    end

    function _mpPatch.hooks.protocol_kickunpached_onDisconnect(playerId)
        if Matchmaking.IsHost() then
            kickTimer [playerId] = nil
            isPatched [playerId] = nil
            isOutdated[playerId] = nil
        end
    end
end