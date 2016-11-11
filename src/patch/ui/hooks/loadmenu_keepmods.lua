if _mpPatch and _mpPatch.loaded and ContextPtr:GetID() == "MPLoadGameScreen" then
    local override = {}
    for _, func in ipairs({"HostServerGame", "HostInternetGame", "HostHotSeatGame", "HostLANGame"}) do
        local func = Matchmaking[func]
        override[func] = function(...)
            local fileName = ...
            _mpPatch.overrideModsFromSaveFile(fileName)
            local ret = {func(...)}
            PreGame.SetPersistSettings(false)
            _mpPatch.enrollModsList(Modding.GetActivatedMods())
            return unpack(ret)
        end
    end
    Matchmaking = _mpPatch.hookTable(Matchmaking, override)
end
