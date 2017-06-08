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
    _mpPatch.debugPrint("Overriding mods...")
    patch.NetPatch.reset()
    for _, mod in ipairs(list) do
        local id = mod.ID or mod.ModID
        _mpPatch.debugPrint("- Adding mod ".._mpPatch.getModName(id, mod.Version).."...")
        patch.NetPatch.pushMod(id, mod.Version)
    end
    patch.NetPatch.overrideModList()
    patch.NetPatch.install()
end
function _mpPatch.overrideModsFromActivatedList()
    local modList = Modding.GetActivatedMods()
    if modList and #modList > 0 then
        _mpPatch.overrideWithModList(modList)
    end
end
function _mpPatch.overrideModsFromPreGame()
    local modList = _mpPatch.decodeModsList()
    if modList and _mpPatch.isModding then
        _mpPatch.overrideWithModList(modList)
    end
end

_mpPatch._mt.registerProperty("areModsEnabled", function()
    return #Modding.GetActivatedMods() > 0
end)

function _mpPatch.getModName(uuid, version)
    local details = Modding.GetInstalledModDetails(uuid, version) or {}
    return details.Name or "<unknown mod "..uuid.." v"..version..">"
end

_mpPatch._mt.registerLazyVal("installedMods", function()
    local installed = {}
    for _, v in pairs(Modding.GetInstalledMods()) do
        installed[v.ID.."_"..v.Version] = true
    end
    return installed
end)
function _mpPatch.isModInstalled(uuid, version)
    return not not _mpPatch.installedMods[uuid.."_"..version]
end

-- Mod dependency listing
function _mpPatch.normalizeDlcName(name)
    return name:gsub("-", ""):upper()
end
function _mpPatch.getModDependencies(modList)
    local dlcDependencies = {}
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
                    if not dlcDependencies[normName] then
                        dlcDependencies[normName] = {}
                    end
                    table.insert(dlcDependencies[normName], info)
                end
            end
        end
    end
    return dlcDependencies
end

-- Encode numbers efficiently
local printableCharacters = {string.byte("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ", 0, -1) }
local printableCharacterCount = #printableCharacters
local math_floor = math.floor
local function encodeNumber(n)
    local str = ""
    while n >= printableCharacterCount do
        str = str .. printableCharacters[(n % printableCharacterCount) + 1]
        n = math_floor(n / printableCharacterCount)
    end
    return str .. printableCharacters[n]
end

-- UUID encoding/decoding for storing a mod list in PreGame's options table
local uuidRegex = ("("..("[0-9a-fA-F]"):rep(8)..")"):rep(4)
local function encodeUUID(uuidString)
    uuidString = uuidString:gsub("-", "")
    local uuids = {uuidString:match(uuidRegex)}
    if #uuids ~= 4 or not uuids[1] then error("could not parse UUID") end
    return _mpPatch.map(uuids, function(x) return tonumber(x, 16) end)
end
local function split32(v)
    return math.floor(v / 0x10000), v % 0x10000
end
local function decodeUUID(table)
    local a, b, c, d = unpack(table)
    local b0, b1 = split32(b)
    local c0, c1 = split32(c)
    return string.format("%08x-%04x-%04x-%04x-%04x%08x", a, b0, b1, c0, c1, d)
end

-- Enroll/decode mods into the PreGame option table
--
-- To save on network bandwidth, the names are written in shorthand.
-- ?       = are mods enabled
-- #       = mods count
-- $id_V   = version for mod $id
-- $id_$i  = block $i of mod $id's uuid
-- $id_N#  = the length of mod $id's name
-- $id_N$i = data block $i for mod $id's name
--
-- $id and $i are encoded using encodeNumber in each of these functions. The UUID is encoded as 4 32-bit integers,
-- representing the whole UUID as a big endian 128-bit integer, and the mod's name is encoded into ceil(len / 4)
-- data blocks.

_mpPatch._mt.registerProperty("isModding", function()
    return _mpPatch.getGameOption("?") == 1
end)

local function enrollModName(id, name)
    _mpPatch.setGameOption(encodeNumber(id).."_N#", #name)

    local j = 1
    for i=1,#name,4 do
        local a, b, c, d = name:byte(i, i+3)
        a, b, c, d = a or 0, b or 0, c or 0, d or 0
        local v = ((((a * 256) + b) * 256) + c) * 256 + d
        _mpPatch.setGameOption(encodeNumber(id).."_N"..encodeNumber(j), v)
        j = j + 1
    end
end
local function enrollMod(id, uuid, version)
    local modName = _mpPatch.getModName(uuid, version)
    _mpPatch.debugPrint("- Enrolling mod "..modName)
    _mpPatch.setGameOption(encodeNumber(id).."_V", version)
    for i, v in ipairs(encodeUUID(uuid)) do
        _mpPatch.setGameOption(encodeNumber(id).."_"..i, v)
    end
    enrollModName(id, modName)
end
function _mpPatch.enrollModsList(modList)
    _mpPatch.debugPrint("Enrolling mods...")
    if #modList > 0 then
        _mpPatch.setGameOption("?", 1)
        _mpPatch.setGameOption("#", #modList)
        for i, v in ipairs(modList) do
            enrollMod(i, v.ID, v.Version)
        end
    else
        _mpPatch.setGameOption("?", 0)
    end
end

local function decodeModName(id)
    local charTable = {}

    local i = _mpPatch.getGameOption(encodeNumber(id).."_N#")
    local j = 1
    while i > 0 do
        local current = (j - 1) * 4 + 1

        local v = string.char(_mpPatch.getGameOption(encodeNumber(id).."_N"..encodeNumber(j)))
        local a, b, c, d = math.floor(v / 0x1000000) % 0x100, math.floor(v / 0x10000) % 0x100,
                           math.floor(v / 0x100) % 0x100, v % 0x100
        if i >= 1 then charTable[current + 0] = a end
        if i >= 2 then charTable[current + 1] = b end
        if i >= 3 then charTable[current + 2] = c end
        if i >= 4 then charTable[current + 3] = d end

        i = i - 4
        j = j + 1
    end

    return table.concat(charTable)
end
local function decodeMod(id)
    local uuidTable = {}
    for i=1,4 do
        uuidTable[i] = _mpPatch.getGameOption(encodeNumber(id).."_"..i)
    end
    return {
        ID      = decodeUUID(uuidTable),
        Version = _mpPatch.getGameOption(encodeNumber(id).."_V"),
        Name    = decodeModName(id)
    }
end
function _mpPatch.decodeModsList()
    if not _mpPatch.isModding then return nil end

    local modList = {}
    for i=1,_mpPatch.getGameOption("#") do
        modList[i] = decodeMod(i)
    end
    return modList
end