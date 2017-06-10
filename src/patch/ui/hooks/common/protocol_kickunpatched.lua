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
    local playerMap = {}
    local isPatched = {}

    local chatActive = {}
    local chatQueue = {}

    local function sendChat(playerId, fn)
        if chatActive[playerId] then
            fn()
        else
            if not chatQueue[playerId] then
                chatQueue[playerId] = {}
            end
            table.insert(chatQueue[playerId], fn)
        end
    end
    local function setChatActive(playerId)
        chatActive[playerId] = true
        if chatQueue[playerId] then
            for _, fn in ipairs(chatQueue[playerId]) do
                fn()
            end
            chatQueue[playerId] = nil
        end
    end

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
        _mpPatch.event.reset.registerHandler(function()
            playerMap = {}
            isPatched = {}
            chatActive = {}
            chatQueue = {}
        end)

        _mpPatch.net.clientIsPatched.registerHandler(function(protocolVersion, playerId)
            if Matchmaking.IsHost() then
                if protocolVersion == _mpPatch.protocolVersion then
                    playerMap[playerId] = nil
                    isPatched[playerId] = true
                else
                    local header = getHeader(playerId)

                    sendChat(playerId, function()
                        _mpPatch.skipNextChatIfVersion(_mpPatch.protocolVersion)
                        Network.SendChat(header..Locale.Lookup("TXT_KEY_MPPATCH_JOIN_WARNING_1_OUTDATED").." "..
                                         joinWarning1Ending)

                        _mpPatch.skipNextChatIfVersion(_mpPatch.protocolVersion)
                        Network.SendChat(header..Locale.Lookup("TXT_KEY_MPPATCH_JOIN_WARNING_2")..website)
                    end)
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

    function _mpPatch.hooks.protocol_kickunpached_chatActive(playerId)
        setChatActive(playerId)
    end

    function _mpPatch.hooks.protocol_kickunpached_onJoin(playerId)
        if Matchmaking.IsHost() and not playerMap[playerId] and not isPatched[playerId] then
            local header = getHeader(playerId)

            sendChat(playerId, function()
                _mpPatch.net.skipNextChat(2)
                Network.SendChat(header..Locale.Lookup("TXT_KEY_MPPATCH_JOIN_WARNING_1_NOT_INSTALLED").." "..
                                 joinWarning1Ending)
                Network.SendChat(header..Locale.Lookup("TXT_KEY_MPPATCH_JOIN_WARNING_2")..website)
            end)

            playerMap[playerId] = 30
        end
    end

    function _mpPatch.hooks.protocol_kickunpached_onDisconnect(playerId)
        if Matchmaking.IsHost() then
            playerMap[playerId] = nil
            isPatched[playerId] = nil
            chatActive[playerId] = nil
            chatQueue[playerId] = nil
        end
    end
end