if _mpPatch and _mpPatch.enabled then
    Controls.LaunchButton:RegisterCallback(Mouse.eLClick, function(...)
        PreGame.SetPersistSettings(false)
        _mpPatch.enrollModsList(Modding.GetActivatedMods())
        return OnStart(...)
    end)
end