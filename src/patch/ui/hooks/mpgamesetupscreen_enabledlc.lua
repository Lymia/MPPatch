if _mpPatch and _mpPatch.canEnable then
    local OnStartOld = OnStart
    function OnStart(...)
        if _mpPatch.areModsEnabled then
            if not ContentManager.IsActive(_mpPatch.uuid, ContentType.GAMEPLAY) then
                ContentManager.SetActive({{_mpPatch.uuid, ContentType.GAMEPLAY, true}})
            end
            PreGame.SetDLCAllowed(_mpPatch.normalizeDlcName(_mpPatch.uuid), true)
        end
        return OnStartOld(...)
    end
    Controls.LaunchButton:RegisterCallback(Mouse.eLClick, OnStart)
end