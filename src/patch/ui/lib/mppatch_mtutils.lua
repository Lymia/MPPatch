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

-- Metatable __index hooks
local indexers = {}
function _mpPatch._mt.__index(_, k)
    for _, fn in ipairs(indexers) do
        local v = fn(k)
        if v ~= nil then return v end
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
        return v
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
        return p.read()
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
