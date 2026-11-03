local patch = _mpPatch.patch

function _mpPatch.forceReloadMods()
    patch.NetPatch.reset()
    patch.NetPatch.overrideReloadMods(true)
end
function _mpPatch.overrideWithModList(list)
    _mpPatch.debugPrint("Overriding mods...")
    patch.NetPatch.reset()
    for _, mod in ipairs(list) do
        local id = mod.ID or mod.ModID
        _mpPatch.debugPrint("- Adding mod ".._mpPatch.getModName(id, mod.Version).."...")
        patch.NetPatch.pushMod(id, mod.Version)
    end
    patch.NetPatch.overrideModList()
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
