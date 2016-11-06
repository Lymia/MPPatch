if _mpPatch and _mpPatch.canEnable then
    local IsHotLoad = ContextPtr.IsHotLoad
    _mpPatch.patch.globals.rawset(ContextPtr, "IsHotLoad", function(this, ...)
        return IsHotLoad(this, ...) and not _mpPatch.isModding
    end)

    Modding = _mpPatch.hookTable(Modding, {ActivateAllowedDLC = function(...)
        for _,v in ipairs(ContentManager.GetAllPackageIDs()) do
            _mpPatch.debugPrint("Allowed DLC:")
            _mpPatch.debugPrint(" - "..v..": "..tostring(PreGame.IsDLCAllowed(v)))
        end

        _mpPatch.overrideModsFromPreGame()
        return Modding._super.ActivateAllowedDLC(...)
    end})
end