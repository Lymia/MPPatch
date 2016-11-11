if _mpPatch and _mpPatch.loaded and ContextPtr:GetID() == "MPLoadGameScreen" then
    local override = {}
    for _, funcName in ipairs({"HostServerGame", "HostInternetGame", "HostHotSeatGame", "HostLANGame"}) do
        local func = Matchmaking[funcName]
        override[funcName] = function(...)
            if not g_ShowCloudSaves then
                _mpPatch.overrideModsFromSaveFile(g_FileList[g_iSelected])
            else
                _mpPatch.overrideModsFromCloudSave(g_iSelected)
            end
            local ret = {func(...)}
            PreGame.SetPersistSettings(false)
            _mpPatch.enrollModsList(Modding.GetActivatedMods())
            return unpack(ret)
        end
    end
    Matchmaking = _mpPatch.hookTable(Matchmaking, override)
end
