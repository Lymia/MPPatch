do
    local versionString
    if not _mpPatch then
        versionString = Locale.Lookup("TXT_KEY_MPPATCH_UNKNOWN_FAILURE")
    elseif not _mpPatch.canEnable then
        versionString = Locale.Lookup("TXT_KEY_MPPATCH_BINARY_NOT_PATCHED")
    elseif not _mpPatch.enabled then
        versionString = Locale.Lookup("TXT_KEY_MPPATCH_DISABLED")
    else
        versionString = "MpPatch v".._mpPatch.versionString
    end
    Controls.VersionNumber:SetText(Controls.VersionNumber:GetText().." -- "..versionString)
end

if _mpPatch and _mpPatch.canEnable then
    Modding = _mpPatch.hookTable(Modding, {ActivateDLC = function(...)
        print("Resetting NetPatch (just in case)")
        _mpPatch.patch.NetPatch.reset()
        return Modding._super.ActivateDLC(...)
    end})
end