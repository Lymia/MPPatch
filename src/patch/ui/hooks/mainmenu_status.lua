pcall(function()
    local versionString
    if not _mpPatch then
        versionString = Locale.Lookup("TXT_KEY_MPPATCH_UNKNOWN_FAILURE")
    elseif not _mpPatch.canEnable then
        versionString = Locale.Lookup("TXT_KEY_MPPATCH_BINARY_NOT_PATCHED")
    else
        versionString = "MpPatch v".._mpPatch.versionString
    end
    Controls.VersionNumber:SetText(Controls.VersionNumber:GetText().." -- "..versionString)
end)