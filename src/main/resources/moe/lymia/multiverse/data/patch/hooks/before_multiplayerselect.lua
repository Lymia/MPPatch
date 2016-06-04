if _mpPatch and _mpPatch.enabled and ContextPtr:GetID() == "ModMultiplayerSelectScreen" then
    Modding = _mpPatch.hookTable(Modding, {ActivateDLC = function() end})
end
