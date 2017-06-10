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

local marker = "mppatch_command:8f671fc2-cd03-11e6-9c65-00e09c101bf5:"

local function sendChatCommand(id, data)
    _mpPatch.debugPrint("Sending MPPatch chat command: "..id..", data = "..(data or "<no data>"))
    Network.SendChat(marker..id..":"..(data or ""))
end

local chatProtocolCommands = {}
local function newChatCommand(id)
    local event = _mpPatch.event["command_"..id]
    chatProtocolCommands[id] = event
    return setmetatable({
        send = function(data)
            sendChatCommand(id, data)
        end,
        registerHandler = event.registerHandler
    }, {
      __call = function(t, ...) return t.send(...) end
    })
end

function _mpPatch.interceptChatFunction(fn, condition, chatCondition, noCheckHide)
    condition     = condition     or function() return true end
    chatCondition = chatCondition or function() return true end
    local function chatFn(...)
        local fromPlayer, _, text = ...
        if (noCheckHide or not ContextPtr:IsHidden()) and condition(...) then
            local textHead, textTail = text:sub(1, marker:len()), text:sub(marker:len() + 1)
            if textHead == marker then
                local split = textTail:find(":")
                local command, data = textTail:sub(1, split - 1), textTail:sub(split + 1)
                if data == "" then data = nil end
                _mpPatch.debugPrint("Got MPPatch chat command: "..command..", "..
                                    "player = "..fromPlayer.." data = "..(data or "<no data>"))
                local fn = chatProtocolCommands[command]
                if not fn then
                    return
                else
                    return fn(data, ...)
                end
            end
        end
        if fn and chatCondition(...) then return fn(...) end
    end
    return chatFn
end

-- protocol information
_mpPatch.protocolVersion = "0"

-- commands
local chatCommands = {}
_mpPatch.net = setmetatable({}, {
    __index = function(_, k)
        if not chatCommands[k] then
            chatCommands[k] = newChatCommand(k)
        end
        return chatCommands[k]
    end
})