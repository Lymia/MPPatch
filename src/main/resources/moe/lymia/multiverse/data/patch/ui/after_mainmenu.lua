local versionString = "MPPatch binary patch not installed!"
if _mpPatch then
    versionString = "MpPatch v".._mpPatch.versionString
end
Controls.VersionNumber:SetText(Controls.VersionNumber:GetText().." -- "..versionString)
