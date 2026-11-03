-- globals from patch
local rawset = _mpPatch.patch.globals.rawset

-- Metatable __index hooks
local indexers = {}
function _mpPatch._mt.__index(_, k)
    for _, fn in ipairs(indexers) do
        local hasValue, v = fn(k)
        if hasValue then return v end
    end
    error("Access to unknown field "..k.." in MpPatch runtime.")
end
function _mpPatch._mt.registerIndexer(fn)
    table.insert(indexers, fn)
end

-- Metatable __newindex hooks
local newIndexers = {}
function _mpPatch._mt.__newindex(_, k, v)
    for _, fn in ipairs(newIndexers) do
        if fn(k, v) then return end
    end
    rawset(_mpPatch, k, v)
end
function _mpPatch._mt.registerNewIndexer(fn)
    table.insert(newIndexers, fn)
end

-- Lazy variables
local lazyVals = {}
_mpPatch._mt.registerIndexer(function(k)
    local fn = lazyVals[k]
    if fn then
        local v = fn()
        _mpPatch[k] = v
        return true, v
    end
end)
function _mpPatch._mt.registerLazyVal(k, fn)
    lazyVals[k] = fn
end

-- Properties
local properties = {}
_mpPatch._mt.registerIndexer(function(k)
    local p = properties[k]
    if p then
        return true, p.read()
    end
end)
_mpPatch._mt.registerNewIndexer(function(k, v)
    local p = properties[k]
    if p then
        if not p.write then error("write to immutable property "..k) end
        p.write(v)
        return true
    end
end)
function _mpPatch._mt.registerProperty(k, read, write)
    properties[k] = {read = read, write = write}
end
