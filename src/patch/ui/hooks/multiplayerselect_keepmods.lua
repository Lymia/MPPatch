if _mpPatch and _mpPatch.canEnable and ContextPtr:GetID() == "ModMultiplayerSelectScreen" then
    Modding = _mpPatch.hookTable(Modding, {
        ActivateDLC = function(...)
            _mpPatch.overrideModsFromActivatedList()
            Modding._super.ActivateDLC(...)
        end
    })
    PreGame = _mpPatch.hookTable(PreGame, {
        ResetGameOptions = function(...)
            PreGame._super.ResetGameOptions(...)
            PreGame.SetPersistSettings(false)
            _mpPatch.enrollModsList(Modding.GetActivatedMods())
        end
    })
end
