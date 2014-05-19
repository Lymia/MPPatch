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
mod2dlc.version = {component="Mod2DLC Core", major=1, minor=0, mincompat=1}

local patch = DB.GetMemoryUsage().__mod2dlc_patch
mod2dlc.patch_api = patch
if not patch then
    mod2dlc.disabled = true
    -- We can't even reliably do a fatal error without the patch... Just disable the Lua hooks, and
    -- hope for the best.
    -- TODO: Automatically disable all Mod2DLC DLCs in this case. discoverMods + different registerMod?
    print("ERROR: Mod2DLC requires the CvGameDatabase patch to installed to run correctly!")
    return
end

local function versionString(versioninfo)
    return versioninfo.component .. " v" .. versioninfo.major .. "." .. versioninfo.minor
end
local function warnOnVersionError(versioninfo, requestSource, requested, action)
    -- Put the "Stuff might break!" line here, because the only other action we'll ever do is patch.panic.
    -- patch.panic is obviously something breaking. Stuff doesn't just "might" break..
    action = action or function(s) print("WARNING: "..s.." Stuff might break!") end
    if versioninfo.major < requested then
        action(requestSource.." requested "..versioninfo.component.." v"..requested..".x"..
                " or later, but "..versionString(versioninfo).." is installed.")
    elseif requested < versioninfo.mincompat then
        action(requestSource.." requested "..versioninfo.component.." v"..requested..".x"..
                " or later, but "..versionString(versioninfo).." is only backwards compatiable"..
                " only to v"..versioninfo.mincompat..".x or later.")
    end
end

warnOnVersionError(patch.version, "Mod2DLC Core", 1, patch.panic) -- aaaaaaa

local mod_info = {}
function mod2dlc.registerMod(minVersion, id, name, entryPoints)
    print(" - Discovered mod "..name)
    warnOnVersionError(mod2dlc.version, "Mod "..name, minVersion)
    mod_info[id] = {id=id, name=name, entryPoints=entryPoints}
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
end

function mod2dlc.callEntryPoints(entryPoint)
    print("Mod2DLC: Running entry point "..entryPoint)
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
    mod2dlc.discoverMods()
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

local function decode_u32(string, i)
    return string:byte(i+0) * 0x00000001 +
            string:byte(i+1) * 0x00000100 +
            string:byte(i+2) * 0x00010000 +
            string:byte(i+3) * 0x01000000
end
function mod2dlc.getSourcePath(fn)
    local luaFile = ("").dump(fn)
    local luaFile_expectedHeader = "\27\76\117\97\81\0\1\4\4\4\8\0"
    local luaFile_header = luaFile:sub(1, #luaFile_expectedHeader)
    assert(luaFile_header == luaFile_expectedHeader, "Unexpected Lua bytecode format!")

    local str_len = decode_u32(luaFile, 13)
    return luaFile:sub(17, 17+str_len)
end

local hookTargets = {
    InstanceManager   = {InGame=true, CityView=true},
    GameplayUtilities = {LeaderHeadRoot=true},
}
local function clean_string(str)
    -- TODO Figure out how junk is getting into my strings, and fix the root problem.
    if str:find("\0") then
        return str:sub(1, str:find("\0")-1)
    else
        return str
    end
end
function mod2dlc.tryHook(sourceModule)
    sourceModule = clean_string(sourceModule)
    if hookTargets[sourceModule] then
        local targetName = clean_string(patch.getCallerAtLevel(3):gsub(".*\\(.*)%.lua", "%1"))
        if hookTargets[sourceModule][targetName] then
            print("Mod2DLC: Hooking "..targetName.." through "..sourceModule)
            mod2dlc.installEntryPointHook()
        end
    end
end