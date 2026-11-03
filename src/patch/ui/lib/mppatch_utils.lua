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
function _mpPatch.hookGlobalFunction(name, fn)
    _mpPatch.interceptGlobalWrite(name, function(origFn)
        return function(...)
            local v = fn(...)
            if not v then
                return origFn(...)
            end
            return v
        end
    end)
end
function _mpPatch.replaceGlobalFunction(name, fn)
    _mpPatch.interceptGlobalWrite(name, function() return fn end)
end

-- Version utils
function _mpPatch.version.get(string)
    return _mpPatch.version.info[string]
end

function _mpPatch.version.getBoolean(string)
    return _mpPatch.version.get(string) == "true"
end

-- Event utils
local eventTable = {}
local function newEvent()
    local events = {}
    local eventLevels = {}
    return setmetatable({
        registerHandler = function(fn, level)
            level = level or 0
            if not events[level] then
                events[level] = {}
                table.insert(eventLevels, level)
                table.sort(eventLevels)
            end
            table.insert(events[level], fn)
        end
    }, {
        __call = function(t, ...)
            for _, level in ipairs(eventLevels) do
                for _, fn in ipairs(events[level]) do
                    if fn(...) then
                        return true
                    end
                end
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
