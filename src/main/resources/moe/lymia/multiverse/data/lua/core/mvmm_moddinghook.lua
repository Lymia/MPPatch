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

_mvmm.loadedModules.moddinghook = true

local entryPointCache = {}
local function getEntryPoint(entryPoint)
    if not entryPointCache[entryPoint] then
        local buffer = {}

        for _, mod in pairs(_mvmm.getMods()) do
            local entryPoints = mod.entryPoints[entryPoint]
            if entryPoints then
                for _, ep in ipairs(entryPoints) do
                    table.insert(buffer, {
                        ModID         = mod.id,
                        Version       = mod.version,
                        Name          = ep.name,
                        Description   = ep.description,
                        File          = ep.path,
                        -- RowID : Skipping this, since we aren't actually in the database.
                        Type          = 1, -- TODO: Figure out if queries in Civ V take the later or earlier value.
                        Installed     = 1,
                        Enabled       = 1,
                        Activated     = 1,
                        LastRetrieved = "",
                    })
                end
            end
        end

        entryPointCache[entryPoint] = buffer
        return buffer
    else
        return entryPointCache[entryPoint]
    end
end

function _mvmm.installModdingHook()
    local oldModding = Modding

    -- TODO: Fill in all the other functions that we haven't begun to touch yet.

    local function GetActivatedModEntryPoints(entryPoint)
        _mvmm.debugPrint("Hooking GetActivatedModEntryPoints")

        -- TODO: Optimize this to not use a coroutine
        local thread = coroutine.create(function()
            for row in oldModding.GetActivatedModEntryPoints(entryPoint) do
                coroutine.yield(row)
            end
            for _, row in ipairs(getEntryPoint(entryPoint)) do
                coroutine.yield(row)
            end
            coroutine.yield(nil)
        end)

        return function()
            local success, value = coroutine.resume(thread)
            if not success then error(value) end
            return value
        end, nil, nil
    end

    local function GetEvaluatedFilePath(modId, modVersion, path)
        if _mvmm.getMods()[modId] then
            return { ModID = modId, modVersion = modVersion, Path = path, EvaluatedPath = path }
        else
            return oldModding.GetEvaluatedFilePath(modId, modVersion, path)
        end
    end

    local hooks = {
        GetActivatedModEntryPoints = GetActivatedModEntryPoints,
        GetEvaluatedFilePath = GetEvaluatedFilePath,
        __mvmm_marker = true,
    }

    Modding = setmetatable({}, {
        __index = function(_, k)
            if hooks[k] then
                return hooks[k]
            else
                return oldModding[k]
            end
        end
    })
end