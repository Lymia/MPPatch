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

local patch = _mpPatch.patch

function _mpPatch.overrideWithModList(list)
    patch.NetPatch.reset()
    for _, mod in ipairs(list) do
        patch.NetPatch.pushMod(mod.ID, mod.Version)
    end
    patch.NetPatch.overrideModList()
end
function _mpPatch.overrideModsFromPreGame()
    local modList = _mpPatch.decodeModsList()
    if modList then
        _mpPatch.overrideWithModList(modList)
    end
end
function _mpPatch.overrideModsFromSaveFile(file)
    local _, requiredMods = Modding.GetSavedGameRequirements(file)
    if type(requiredMods) == "table" then
        _mpPatch.overrideWithModList(requiredMods)
    end
end

_mpPatch._mt.registerProperty("areModsEnabled", function()
    return #Modding.GetActivatedMods() > 0
end)

function _mpPatch.getModName(uuid, version)
    local details = Modding.GetInstalledModDetails(uuid, version) or {}
    return details.Name or "<unknown mod "..uuid.." v"..version..">"
end

-- Mod dependency listing
function _mpPatch.normalizeDlcName(name)
    return name:gsub("-", ""):upper()
end
function _mpPatch.getModDependencies(modList)
    local dlcDependencies = {}
    for row in GameInfo.DownloadableContent() do
        dlcDependencies[_mpPatch.normalizeDlcName(row.PackageID)] = {}
    end

    for _, mod in ipairs(modList) do
        local info = { ID = mod.ID, Version = mod.Version, Name = _mpPatch.getModName(mod.ID, mod.Version) }
        for _, assoc in ipairs(Modding.GetDlcAssociations(mod.ID, mod.Version)) do
            if assoc.Type == 2 then
                if assoc.PackageID == "*" then
                    for row in GameInfo.DownloadableContent() do
                        table.insert(dlcDependencies[_mpPatch.normalizeDlcName(row.PackageID)], info)
                    end
                else
                    local normName = _mpPatch.normalizeDlcName(assoc.PackageID)
                    if dlcDependencies[normName] then
                        table.insert(dlcDependencies[normName], info)
                    end
                end
            end
        end
    end

    return dlcDependencies
end

-- UUID encoding/decoding for storing a mod list in PreGame's options table
local uuidRegex = ("("..("[0-9a-zA-Z]"):rep(4)..")"):rep(8)
local function encodeUUID(uuidString)
    uuidString = uuidString:gsub("-", "")
    local uuids = {uuidString:match(uuidRegex)}
    if #uuids ~= 8 or not uuids[1] then error("could not parse UUID") end
    return _mpPatch.map(uuids, function(x) return tonumber(x, 16) end)
end
local function decodeUUID(table)
    return string.format("%04x%04x-%04x-%04x-%04x-%04x%04x%04x", unpack(table))
end

-- Enroll/decode mods into the PreGame option table
_mpPatch._mt.registerProperty("isModding", function()
    return PreGame.GetGameOption("_MPPATCH_HAS_MODS") == 1
end)

local function enrollModName(id, name)
    PreGame.SetGameOption("_MPPATCH_MOD_"..id.."_NAME_LENGTH", #name)
    for i=1,#name do
        PreGame.SetGameOption("_MPPATCH_MOD_"..id.."_NAME_"..i, name:byte(i))
    end
end
local function enrollMod(id, uuid, version)
    PreGame.SetGameOption("_MPPATCH_MOD_"..id.."_VERSION", version)
    for i, v in ipairs(encodeUUID(uuid)) do
        PreGame.SetGameOption("_MPPATCH_MOD_"..id.."_"..i, v)
    end
    enrollModName(id, _mpPatch.getModName(uuid, version))
end
function _mpPatch.enrollModsList(modList)
    if #modList > 0 then
        PreGame.SetGameOption("_MPPATCH_HAS_MODS", 1)
        PreGame.SetGameOption("_MPPATCH_MOD_COUNT", #modList)
        for i, v in ipairs(modList) do
            enrollMod(i, v.ID, v.Version)
        end
    end
end

local function decodeModName(id)
    local charTable = {}
    for i=1,PreGame.GetGameOption("_MPPATCH_MOD_"..id.."_NAME_LENGTH") do
        charTable[i] = string.char(PreGame.GetGameOption("_MPPATCH_MOD_"..id.."_NAME_"..i))
    end
    return table.concat(charTable)
end
local function decodeMod(id)
    local uuidTable = {}
    for i=1,8 do
        uuidTable[i] = PreGame.GetGameOption("_MPPATCH_MOD_"..id.."_"..i)
    end
    return {
        ID      = decodeUUID(uuidTable),
        Version = PreGame.GetGameOption("_MPPATCH_MOD_"..id.."_VERSION"),
        Name    = decodeModName(id)
    }
end
function _mpPatch.decodeModsList()
    if not _mpPatch.isModding then return nil end

    local modList = {}
    for i=1,PreGame.GetGameOption("_MPPATCH_MOD_COUNT") do
        modList[i] = decodeMod(i)
    end
    return modList
end