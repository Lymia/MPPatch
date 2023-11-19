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

if _mpPatch and _mpPatch.loaded then
    local skipNextLine = {}

    function _mpPatch.hooks.protocol_chathandler_setupHooks()
        _mpPatch.net.skipNextChat.registerHandler(function(data, fromPlayer)
            skipNextLine[fromPlayer] = tonumber(data)
        end)
        _mpPatch.net.skipNextChatIfVersion.registerHandler(function(data, fromPlayer)
            if data == _mpPatch.protocolVersion then
                skipNextLine[fromPlayer] = 1
            end
        end)
        _mpPatch.event.reset.registerHandler(function()
            skipNextLine = {}
        end)
    end

    function _mpPatch.hooks.protocol_chathandler_new(fn, condition, chatCondition, noCheckHide)
        return _mpPatch.interceptChatFunction(fn, condition, function(...)
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