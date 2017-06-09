if _mpPatch then
    local skipNextLine = {}

    function _mpPatch.hooks.protocol_chathandler_setupHooks()
        _mpPatch.net.skipNextChat.registerHandler(function(data, fromPlayer)
            skipNextLine[fromPlayer] = tonumber(data)
        end)
        _mpPatch.addResetHook(function()
            skipNextLine = {}
        end)
    end

    function _mpPatch.hooks.protocol_chathandler_new(fn, condition, chatCondition, noCheckHide)
        _mpPatch.interceptChatFunction(fn, condition, function(...)
            local fromPlayer = ...
            if skipNextLine[fromPlayer] and skipNextLine[fromPlayer] > 0 then
                skipNextLine[fromPlayer] = skipNextLine[fromPlayer] - 1
                return false
            else
                if chatCondition then return chatCondition(...) end
                return true
            end
        end, noCheckHide)
    end

    function _mpPatch.hooks.protocol_chathandler_onDisconnect(id)
        skipNextLine[id] = nil
    end
end