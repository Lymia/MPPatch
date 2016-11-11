if _mpPatch and _mpPatch.loaded and ContextPtr:GetID() == "MPLoadGameScreen" then
    local override = {}
    for _, funcName in ipairs({"HostServerGame", "HostInternetGame", "HostHotSeatGame", "HostLANGame"}) do
        local func = Matchmaking[funcName]
        override[funcName] = function(...)
            if g_ShowCloudSaves then
                _mpPatch.overrideModsFromSaveFile(PreGame.GetLoadFileName())
            else
                _mpPatch.overrideModsFromCloudSave(PreGame.GetLoadFileName())
            end
            local ret = {func(...)}
            PreGame.SetPersistSettings(false)
            _mpPatch.enrollModsList(Modding.GetActivatedMods())
            return unpack(ret)
        end
    end
    Matchmaking = _mpPatch.hookTable(Matchmaking, override)
end
