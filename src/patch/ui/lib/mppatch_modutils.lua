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
