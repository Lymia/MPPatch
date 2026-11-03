if _mpPatch and _mpPatch.loaded and ContextPtr:GetID() == "MPLoadGameScreen" then
    local override = {}
    for _, funcName in ipairs({"HostServerGame", "HostInternetGame", "HostHotSeatGame", "HostLANGame"}) do
        local func = Matchmaking[funcName]
        override[funcName] = function(...)
            local loadFile
            if g_ShowCloudSaves then
                loadFile = Steam.GetCloudSaveFileName(g_iSelected)
            else
                loadFile = g_FileList[g_iSelected]
            end
            local header = PreGame.GetFileHeader(loadFile)
            _mpPatch.overrideWithModList(header.ActivatedMods)

            local ret = {func(...)}
            return unpack(ret)
        end
    end
    Matchmaking = _mpPatch.hookTable(Matchmaking, override)
end
