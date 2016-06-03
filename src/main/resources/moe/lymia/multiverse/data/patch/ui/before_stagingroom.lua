if _mpPatch --[[ and _mpPatch.isModding() ]] then
    Modding = _mpPatch.hookTable(Modding, {ActivateAllowedDLC = function(...)
        _mpPatch.overrideWithLoadedMods()
        return Modding._super.ActivateAllowedDLC(...)
    end})

    Matchmaking = _mpPatch.hookTable(Matchmaking, {LaunchMultiplayerGame = function(...)
        _mpPatch.overrideWithLoadedMods()
        return Matchmaking._super.LaunchMultiplayerGame(...)
    end})
end
