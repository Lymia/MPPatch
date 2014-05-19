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
mod2dlc.version = {major=1, minor=0, mincompat=1}
local versionString = mod2dlc.version.major .. "." .. mod2dlc.version.minor

local function executeSQL(sql)
    for _ in DB.Query(sql) do end
end

local databasePatch              = DB.GetMemoryUsage().__mod2dlc_patch
local databasePatchVersion       = databasePatch and databasePatch.versioninfo
local databasePatchVersionString = databasePatch and
        (databasePatchVersion.major .. "." .. databasePatchVersion.minor)
mod2dlc.patch_api = databasePatch

if databasePatch then
    local minimumPatchVersion = 1
    local luaVersionString = mod2dlc.lua_version.major .. "." .. mod2dlc.lua_version.minor
    local databasePatchVersionString = databasePatch and
            (databasePatch.versioninfo.major .. "." .. databasePatch.versioninfo.minor)

    if mod2dlc.lua_version.major < databasePatchCoreMinCompat or minimumPatchVersion < databasePatchVersion then
        print("WARNING: CvGameDatabase patch v"..databasePatchVersionString.." is not compatible with"
            .." core v"..luaVersionString.."; things might break VERY badly!!")
    end
end

local mod_info = {}
function mod2dlc.registerMod(mod2DlcCoreVersion, mod2DlcCoreReqVersion,
                             id, name, entryPoints, usesCvGameDatabasePatch)
    if mod2DlcCoreVersion > mod2dlc.version.mincompat then
        print(" - WARNING: Mod2Dlc core v"..versionString.." is only backwards compatible back to"..
                " v"..mod2dlc.version.mincompat..".x; Mod may not function correctly. (Mod requires:"..
                " v"..mod2DlcCoreReqVersion..".x)")
    elseif mod2DlcCoreReqVersion > mod2dlc.version.major then
        print(" - WARNING: Mod requires Mod2Dlc core v"..mod2DlcCoreReqVersion".x or later to function"..
                " correctly. (Installed: v"..versionString..")")
    end

    mod_info[id] = {id=id, name=name, entryPoints=entryPoints, usesCvGameDatabasePatch=usesCvGameDatabasePatch}
end

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
        if mod.usesCvGameDatabasePatch then
            if not databasePatch then
                print("   - WARNING: Mod requires CvGameDatabase patch to function correctly,"..
                        " but patch is not installed.")
            elseif mod.usesCvGameDatabasePatch < databasePatchVersion.major then
                print("   - WARNING: Mod requires CvGameDatabase patch v"..mod.usesCvGameDatabasePatch..".x"..
                        " or later to function correctly (Installed: v"..databasePatchVersionString..")")
            elseif databasePatchVersion.mincompat < mod.usesCvGameDatabasePatch then
                print("   - WARNING: CvGameDatabase patch v"..usesCvGameDatabasePatch.." is only backwards"..
                        " compatiable back to v"..databasePatchVersion.mincompat..".x; Mod may not"..
                        " function correctly. (Mod requires: v"..mod.databasePatchMinCompat..".x)")
            end
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