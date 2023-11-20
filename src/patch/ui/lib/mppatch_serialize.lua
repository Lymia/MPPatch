-- Copyright (c) 2015-2023 Lymia Kanokawa <lymia@lymia.moe>
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

-----------------------------------------------------------------------------------------------------------------------
-- Game option wrapper
--
-- There's a bug in CvPreGame.cpp (available in the SDK) where it passes the result of strlen to strncmp, in a way
-- where it doesn't check for the C string terminator in some cases. This works around it.
--
-- In particular, if you already have an option "_foo", the option "_foooooooooo" will match it, as it will only check
-- the first four characters, and not the terminator.
--
-- We use nonprinting characters to try and try and ensure this bug doesn't cause us any trouble. The choice of two
-- nonprinting characters used should minimize the chance of a option collision between MPPatch and something else
-- while minimizing network transmission overhead from the mod list.
--
-- In addition, the values of options are encoded as signed 32-bit integers. To make handling in Lua code easier,
-- we encode/decode them into unsigned 32-bit integers.
--
-- Even if Civ V is updated to fix the bugs this works around (unlikely, as I don't expect any new versions of Civ V
-- to be released), this encoding must stay the same to retain compatibility with older save games and clients.

local function mungeName(name)
    return "\19"..name.."\8"
end
local function decode32(v)
    if v == nil then return nil end
    if v < 0 then
        return 0x100000000 + v
    else
        return v
    end
end
local function encode32(v)
    if v > 0x7FFFFFFF then
        return v - 0x100000000
    else
        return v
    end
end
local function getGameOption(name)
    return decode32(PreGame.GetGameOption(mungeName(name)))
end
local function setGameOption(name, value)
    return PreGame.SetGameOption(mungeName(name), encode32(value))
end

-----------------------------------------------------------------------------------------------------------------------
-- Utility function for encoding numbers in base 62

local printableCharacters = {string.byte("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ", 0, -1)}
local printableCharacterCount = #printableCharacters
local function encodeNumber(n)
    local str = ""
    while n >= printableCharacterCount do
        str = str .. string.char(printableCharacters[(n % printableCharacterCount) + 1])
        n = math.floor(n / printableCharacterCount)
    end
    return str .. string.char(printableCharacters[n])
end

-----------------------------------------------------------------------------------------------------------------------
-- Utility function for encoding uuids as 4 32-bit integers

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

-----------------------------------------------------------------------------------------------------------------------
-- Enroll/decode mods into the PreGame option table
--
-- To save on network bandwidth, option names are written in shorthand. If at all possible, avoid changing these fields
-- unless necessary, and take efforts to retain backwards compatibility.
--
-- The following fields should never be changed if at all possible, and are used for checking compatibility:
-- V0      = major encoding version (increment this when the protocol is completely changed)
-- V1      = minor encoding version (increment this when new features are added)
-- ?       = are mods enabled
--
-- The following fields can be changed if needed:
-- #       = mods count
-- !$id    = version for mod $id
-- $i$id   = block $i of mod $id's uuid
-- #$id    = the length of mod $id's name
-- $id~$i  = data block $i for mod $id's name
--
-- $id and $i are encoded using encodeNumber in each of these functions. The UUID is encoded as 4 32-bit integers,
-- representing the whole UUID as a big endian 128-bit integer, and the mod's name is encoded into ceil(len / 4)
-- data blocks.

local encodingVersionMajor = 1
local encodingVersionMinor = 0

local isModdingField    = "?"
local majorVersionField = "V0"
local minorVersionField = "V1"

local modCountField     = "#"
local function modVersionField(id)
    return "!"..encodeNumber(id)
end
local function modUUIDBlockField(id, i)
    return tostring(i)..encodeNumber(id)
end
local function modNameLengthField(id)
    return "#"..encodeNumber(id)
end
local function modNameDataBlockField(id, i)
    return encodeNumber(id).."~"..encodeNumber(i)
end

_mpPatch._mt.registerProperty("isModding", function()
    return getGameOption(isModdingField) == 1
end)
_mpPatch._mt.registerProperty("isSupportedVersion", function()
    return getGameOption(majorVersionField) == encodingVersionMajor
end)

local function enrollModName(id, name)
    setGameOption(modNameLengthField(id), #name)

    local j = 1
    for i=1,#name,4 do
        local a, b, c, d = name:byte(i, i+3)
        a, b, c, d = a or 0, b or 0, c or 0, d or 0
        local v = ((((a * 256) + b) * 256) + c) * 256 + d
        setGameOption(modNameDataBlockField(id, j), v)
        j = j + 1
    end
end
local function enrollMod(id, uuid, version)
    local modName = _mpPatch.getModName(uuid, version)
    _mpPatch.debugPrint("- Enrolling mod "..modName)
    setGameOption(modVersionField(id), version)
    for i, v in ipairs(encodeUUID(uuid)) do
        setGameOption(modUUIDBlockField(id, i), v)
    end
    enrollModName(id, modName)
end
function _mpPatch.enrollModsList(modList)
    _mpPatch.debugPrint("Enrolling mods...")
    setGameOption(majorVersionField, encodingVersionMajor)
    setGameOption(minorVersionField, encodingVersionMinor)
    if #modList > 0 then
        setGameOption(isModdingField, 1)
        setGameOption(modCountField, #modList)
        for i, v in ipairs(modList) do
            enrollMod(i, v.ID, v.Version)
        end
    else
        setGameOption(isModdingField, 0)
    end
end

local function decodeModName(id)
    local charTable = {}

    local i = getGameOption(modNameLengthField(id))
    local j = 1
    while i > 0 do
        local current = (j - 1) * 4 + 1

        local v = getGameOption(modNameDataBlockField(id, j))
        local a, b, c, d = math.floor(v / 0x1000000) % 0x100, math.floor(v / 0x10000) % 0x100,
                           math.floor(v / 0x100) % 0x100, v % 0x100
        if i >= 1 then charTable[current + 0] = string.char(a) end
        if i >= 2 then charTable[current + 1] = string.char(b) end
        if i >= 3 then charTable[current + 2] = string.char(c) end
        if i >= 4 then charTable[current + 3] = string.char(d) end

        i = i - 4
        j = j + 1
    end

    return table.concat(charTable)
end
local function decodeMod(id)
    local uuidTable = {}
    for i=1,4 do
        uuidTable[i] = getGameOption(modUUIDBlockField(id, i))
    end
    return {
        ID      = decodeUUID(uuidTable),
        Version = getGameOption(modVersionField(id)),
        Name    = decodeModName(id)
    }
end
function _mpPatch.decodeModsList()
    if not _mpPatch.isModding then return nil end

    local modList = {}
    for i=1,getGameOption(modCountField) do
        modList[i] = decodeMod(i)
    end
    return modList
end