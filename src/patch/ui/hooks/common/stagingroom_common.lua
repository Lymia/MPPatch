local _mpPatch_isModding = _mpPatch and _mpPatch.loaded and _mpPatch.isModding
local _mpPatch_isInGame  = ContextPtr:LookUpControl(".."):GetID() == "InGame"

local _mpPatch_activateFrontEnd = _mpPatch_isModding and not _mpPatch_isInGame
local _mpPatch_activateInGame   = _mpPatch_isModding and     _mpPatch_isInGame

if _mpPatch_isModding then
    Modding = _mpPatch.hookTable(Modding, {ActivateAllowedDLC = function(...)
        _mpPatch.overrideModsFromPreGame()
        return Modding._super.ActivateAllowedDLC(...)
    end})
end