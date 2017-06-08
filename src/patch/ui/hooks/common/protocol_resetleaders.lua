if _mpPatch then
    function _mpPatch.hooks.protocol_resetleaders()
        _mpPatch.net.sendPlayerData.registerHandler(function()
            if Matchmaking.IsHost() then
                Network.BroadcastPlayerInfo()
            end
        end)
    end
end