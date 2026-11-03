local success, error = pcall(function()
    local versionString
    if not _mpPatch then
        versionString = Locale.Lookup("TXT_KEY_MPPATCH_UNKNOWN_FAILURE")
    elseif not _mpPatch.loaded and _mpPatch.status.binaryLoadFailed then
        versionString = Locale.Lookup("TXT_KEY_MPPATCH_BINARY_NOT_PATCHED")
    elseif not _mpPatch.loaded then
        versionString = Locale.Lookup("TXT_KEY_MPPATCH_UNKNOWN_FAILURE")
    else
        versionString = "MpPatch v".._mpPatch.versionString
    end
    Controls.VersionNumber:SetText(Controls.VersionNumber:GetText().." -- "..versionString)
end)

if not success then
    print("Failed to add version to main menu: "..tostring(error))
end