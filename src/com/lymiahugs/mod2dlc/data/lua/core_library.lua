-- Copyright (C) 2014 Lymia Aluysia <lymiahugs@gmail.com>
--
-- Permission is hereby granted, free of charge, to any person obtaining a copy of
-- this software and associated documentation files (the "Software"), to deal in
-- the Software without restriction, including without limitation the rights to
-- use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
-- of the Software, and to permit persons to whom the Software is furnished to do
-- so, subject to the following conditions:
--
-- The above copyright notice and this permission notice shall be included in all
-- copies or substantial portions of the Software.
--
-- THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
-- IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
-- FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
-- AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
-- LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
-- OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
-- SOFTWARE.

mod2dlc = {}

local function executeSQL(sql)
    for _ in DB.Query(sql) do end
end

local mod_info = {}
function mod2dlc.registerMod(id, name, entryPoints, usesCvGameDatabasePatch)
    mod_info[id] = {id=id, name=name, entryPoints=entryPoints, usesCvGameDatabasePatch=usesCvGameDatabasePatch}
end

DB.CollectMemoryUsage()
local databasePatchInstalled = DB.GetMemoryUsage()._mod2dlc_marker
function mod2dlc.discoverMods()
    print("Mod2DLC: Discovering mods")
    local packageIDs = ContentManager.GetAllPackageIDs()
    for i, v in ipairs(packageIDs) do
        if not ContentManager.IsUpgrade(v) and ContentManager.IsActive(v, ContentType.GAMEPLAY) then
            local canonical = v:lower():gsub("-", "")
            -- We're putting these in UI/Mod2DLC since apparently, Civilization V is really really stupid, and
            -- only checks the first 8 characters.
            include("Mod2DLC\\_mod2dlc_"..canonical.."_manifest.lua")
        end
    end
    for _, mod in pairs(mod_info) do
        print(" - Discovered mod "..mod.name)
        if mod.usesCvGameDatabasePatch and not databasePatchInstalled then
            print(" - WARNING: Mod requires CvGameDatabase patch which is not installed!!")
        end
    end
end

function mod2dlc.callEntryPoints(entryPoint)
    print("Mod2DLC: Running entry point")
    g_uiAddins = g_uiAddins or {}
    for _, mod in pairs(mod_info) do
        local epList = mod.entryPoints[entryPoint]
        if epList then
            for _, ep in ipairs(epList) do
                print(" - Executing entry point "..ep.name.." from "..mod.name)
                local addinPath = ep.path
                local extension = Path.GetExtension(addinPath)
                local path = string.sub(addinPath, 1, #addinPath - #extension)
                table.insert(g_uiAddins, ContextPtr:LoadNewContext(path));
            end
        end
    end
end


function mod2dlc.checkInit()
    local init = false
    for _ in DB.Query("select * from sqlite_master where tbl_name = \"Mod2DLC_Marker\" and type = \"table\"") do
        init = true
    end
    return init
end
function mod2dlc.initMods()
    if mod2dlc.checkInit() then
        return
    end
    print("Mod2DLC: Discovering Mods")
    mod2dlc.discoverMods()
    executeSQL("create table Mod2DLC_Marker(x INTEGER)")
end

function mod2dlc.installEntryPointHook()
    mod2dlc.initMods()
    if not Modding.__mod2dlc_marker then
        local oldModding = Modding
        Modding = setmetatable({}, {
            __index = function(_, k)
                if k == "GetActivatedModEntryPoints" then
                    return function(entryPoint, ...)
                        local iter = oldModding.GetActivatedModEntryPoints(entryPoint, ...)
                        return function(...)
                            local rv = {iter(...)}
                            if rv[1] == nil then
                                mod2dlc.callEntryPoints(entryPoint)
                            end
                            return unpack(rv)
                        end
                    end
                elseif k == "__mod2dlc_marker" then
                    return true
                else
                    return oldModding[k]
                end
            end
        })
    end
end