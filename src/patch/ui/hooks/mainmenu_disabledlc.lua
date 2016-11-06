if _mpPatch and _mpPatch.canEnable then
    local IsHotLoad = ContextPtr.IsHotLoad
    _mpPatch.patch.globals.rawset(ContextPtr, "IsHotLoad", function(this, ...)
        _mpPatch.debugPrint("Resetting NetPatch (just in case)")
        _mpPatch.patch.NetPatch.reset()
        return IsHotLoad(this, ...) and not ContentManager.IsActive(_mpPatch.uuid, ContentType.GAMEPLAY)
    end)

    Modding = _mpPatch.hookTable(Modding, {ActivateDLC = function(...)
        if ContentManager.IsActive(_mpPatch.uuid, ContentType.GAMEPLAY) then
            ContentManager.SetActive({{_mpPatch.uuid, ContentType.GAMEPLAY, false}})
        end
        return Modding._super.ActivateDLC(...)
    end})
end