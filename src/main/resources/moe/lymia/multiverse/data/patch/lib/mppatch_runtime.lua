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

_mpPatch = nil

local patch = DB.GetMemoryUsage("216f0090-85dd-4061-8371-3d8ba2099a70")
local hasPatch = false
if patch.__mvmm_marker then hasPatch = true end

if not hasPatch then return end

_mpPatch = {}
_mpPatch.patch = patch
_mpPatch.versionString = patch.version.versionString

local function getFullPath()
    local accum = ContextPtr:GetID()
    local path = ".."
    local seen = {}
    while true do
        local currentContext = ContextPtr:LookUpControl(path)
        if not currentContext then break end
        if seen[currentContext:GetID()] then
            return "{...}/"..accum
        end
        seen[currentContext:GetID()] = true
        accum = currentContext:GetID().."/"..accum
        path = path.."/.."
    end
    return accum
end

function _mpPatch.isModding()
    local path = _mpPatch.fullPath
    return not not (path:find("ModMultiplayerSelectScreen") or path:find("ModdingMultiplayer"))
end

function _mpPatch.overrideWithModList(list)
    patch.NetPatch.reset()
    for _, mod in ipairs(list) do
        patch.NetPatch.pushMod(mod.ID, mod.Version)
    end
    patch.NetPatch.overrideModList()
end
function _mpPatch.overrideWithLoadedMods()
    _mpPatch.overrideWithModList(Modding.GetActivatedMods())
end
function _mpPatch.overrideModsFromSaveFile(file)
    local _, requiredMods = Modding.GetSavedGameRequirements(file)
    if type(requiredMods) == "table" then
        _mpPatch.overrideWithModList(requiredMods)
    end
end

function _mpPatch.hookTable(table, hooks)
    return setmetatable({}, {__index = function(_, k)
        if hooks[k] then return hooks[k] end
        if k == "_super" then return table end
        return table[k]
    end})
end

_mpPatch.fullPath = getFullPath()
print("MPPatch runtime loaded in ".._mpPatch.fullPath)