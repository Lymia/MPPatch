-- Copyright (c) 2015-2016 Lymia Alusyia <lymia@lymiahugs.com>
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

local LookUpControl = ContextPtr.LookUpControl -- cached because we override this in some of our hooks

_mpPatch._mt.registerLazyVal("fullPath", function()
    local accum = ContextPtr:GetID()
    local path = ".."
    local seen = {}
    while true do
        local currentContext = LookUpControl(ContextPtr, path)
        if not currentContext then break end
        if seen[currentContext:GetID()] then
            return "{...}/"..accum
        end
        seen[currentContext:GetID()] = true
        accum = currentContext:GetID().."/"..accum
        path = path.."/.."
    end
    return accum
end)

function _mpPatch.loadElementFromProxy(proxyName, controlName)
    local proxy = ContextPtr:LoadNewContext(proxyName)
    Controls[controlName] = LookUpControl(proxy, controlName)
end

function _mpPatch.setBIsModding()
    function ContextPtr.LookUpControl()
        return {
            GetID = function() return "ModMultiplayerSelectScreen" end
        }
    end
end

-- Update function hooking
local hooks = {}
local hookLevels = {}
function _mpPatch.onUpdate(...)
    for _, level in ipairs(hookLevels) do
        for _, hook in ipairs(hooks[level]) do
            if hook(...) then
                return true
            end
        end
    end
end
function _mpPatch.hookUpdate()
    ContextPtr:SetUpdate(_mpPatch.onUpdate)
end
function _mpPatch.unhookUpdate()
    ContextPtr:ClearUpdate()
end
function _mpPatch.addUpdateHook(hook, level)
    level = level or 0
    if not hooks[level] then
        hooks[level] = {}
        table.insert(hookLevels, level)
        table.sort(hookLevels)
    end
    table.insert(hooks[level], hook)
end

-- Reset UI
local resetHooks = {}
function _mpPatch.addResetHook(fn)
    table.insert(resetHooks, fn)
end
function _mpPatch.resetUI()
    _mpPatch.debugPrint("Resetting UI")
    for _, fn in ipairs(resetHooks) do
        fn()
    end
end