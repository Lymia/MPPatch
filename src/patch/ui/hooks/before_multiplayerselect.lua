if _mpPatch and _mpPatch.enabled and ContextPtr:GetID() == "ModMultiplayerSelectScreen" then
    Modding = _mpPatch.hookTable(Modding, {
        ActivateDLC = function() end
    })
    PreGame = _mpPatch.hookTable(PreGame, {
        ResetGameOptions = function(...)
            PreGame._super.ResetGameOptions(...)
            PreGame.SetPersistSettings(false)
            _mpPatch.enrollModsList(Modding.GetActivatedMods())
        end
    })
end
