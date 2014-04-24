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
function mod2dlc.registerMod(id, name, entryPoints, actions)
    mod_info[id] = {id=id, name=name, entryPoints=entryPoints, actions=actions}
end

function mod2dlc.discoverMods()
    print("Mod2Dlc: Discovering mods")
    local packageIDs = ContentManager.GetAllPackageIDs()
    for i, v in ipairs(packageIDs) do
        if not ContentManager.IsUpgrade(v) and ContentManager.IsActive(v, ContentType.GAMEPLAY) then
            local canonical = v:lower():gsub("-", "")
            -- We're putting these in UI/Mod2DLC since apparently, Civilization V is really really stupid, and
            -- only checks the first 8 characters. (note: confirm)
            include("Mod2DLC\\_mod2dlc_"..canonical.."_manifest.lua")
        end
    end
    for _, mod in pairs(mod_info) do
        print(" - Discovered mod "..mod.name)
    end
end

function mod2dlc.runAction(action)
    print("Mod2Dlc: Running action trigger "+action+".")
    for _, mod in pairs(mod_info) do
        local event = mod.actions[action]
        if act then
            for _, act in ipairs(event) do
                if act.type == "UpdateDatabase" then
                    print(" - Running database update "..act.source.." from mod "..mod.name)
                    executeSQL(act.data)
                else
                    print(" - Unknown action type "..act.type.." in mod "..mod.name)
                end
            end
        end
    end
end

function mod2dlc.checkInit()
    local init = false
    for _ in DB.Query("select * from sqlite_master where tbl_name = \"Mod2Dlc_Marker\" and type = \"table\"") do
        init = true
    end
    return init
end
function mod2dlc.initMods()
    if mod2dlc.checkInit() then
        return
    end
    print("Mod2Dlc: Initializing Mods...")
    mod2dlc.discoverMods()
    mod2dlc.runAction("OnModActivated")
    executeSQL("create table Mod2Dlc_Marker(x INTEGER)")
end