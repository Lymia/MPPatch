if _mpPatch and _mpPatch.canEnable then
    Modding = _mpPatch.hookTable(Modding, {ActivateDLC = function(...)
        _mpPatch.debugPrint("Resetting NetPatch (just in case)")
        _mpPatch.patch.NetPatch.reset()
        return Modding._super.ActivateDLC(...)
    end})
end