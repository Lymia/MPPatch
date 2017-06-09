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

-- globals from patch
local rawset = _mpPatch.patch.globals.rawset

-- Misc utils
function _mpPatch.hookTable(table, hooks)
    return setmetatable({}, {__index = function(_, k)
        if hooks[k] then return hooks[k] end
        if k == "_super" then return table end
        return table[k]
    end})
end

function _mpPatch.map(list, fn)
    local newList = {}
    for k, v in pairs(list) do
        newList[k] = fn(v)
    end
    return newList
end

function _mpPatch.strStarts(str, prefix)
   return str:sub(1, prefix:len()) == prefix
end


-- Soft hook utils
local globalMetatableSetupComplete = false
local interceptGlobalWriteHooks = {}
local function setupGlobalMetatable()
    if not globalMetatableSetupComplete then
        local _G = _mpPatch.patch.getGlobals()
        local mt = getmetatable(_G)
        if not mt then
            mt = {}
            setmetatable(_G, mt)

            mt.__newindex = function(t, k, v)
                local hook = interceptGlobalWriteHooks[k]
                if hook then
                    _mpPatch.debugPrint("Intercepting write to _G."..k)
                    v = hook(v)
                end
                return rawset(t, k, v)
            end
        end
        globalMetatableSetupComplete = true
    end
end
function _mpPatch.interceptGlobalWrite(name, fn)
    setupGlobalMetatable()
    interceptGlobalWriteHooks[name] = fn
end

-- Version utils
function _mpPatch.version.get(string)
    return _mpPatch.version.info[string]
end

function _mpPatch.version.getBoolean(string)
    return get(string) == "true"
end

-- Event utils
local eventTable = {}
local function newEvent()
    local events = {}
    return setmetatable({
        registerHandler = function(fn)
            table.insert(events, fn)
        end
    }, {
        __call = function(t, ...)
            for _, fn in ipairs(events) do
                fn(...)
            end
        end
    })
end
_mpPatch.event = setmetatable({}, {
    __index = function(_, k)
        if not eventTable[k] then
            eventTable[k] = newEvent()
        end
        return eventTable[k]
    end
})

-- Pregame utils
--
-- There's a bug in CvPreGame.cpp (available in the SDK) where it passes the result of strlen to strncmp, in a way
-- where it doesn't check for the C string terminator in some cases. This works around it.
--
-- In particlar, if you already have an option "_foo", the option "_foooooooooo" will match it, as it will only check
-- the first four characters, and not the terminator.
--
-- We use nonprinting characters to try and try and ensure this bug doesn't cause us any trouble.
--
-- In addition, the values of options are encoded as signed 32-bit integers. To make handling in Lua code easier,
-- we encode/decode them into unsigned 32-bit integers.

local function mungeName(name)
    return "MPP\8"..name.."\8"
end
local function decode32(v)
    if v == nil then return nil end
    if v < 0 then
        return 0x100000000 + v
    else
        return v
    end
end
local function encode32(v)
    if v > 0x7FFFFFFF then
        return v - 0x100000000
    else
        return v
    end
end
function _mpPatch.getGameOption(name)
    return decode32(PreGame.GetGameOption(mungeName(name)))
end
function _mpPatch.setGameOption(name, value)
    return PreGame.SetGameOption(mungeName(name), encode32(value))
end
