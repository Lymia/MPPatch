if _mpPatch and _mpPatch.loaded then
    local IsHotLoad = ContextPtr.IsHotLoad
    _mpPatch.patch.globals.rawset(ContextPtr, "IsHotLoad", function(...)
        _mpPatch.debugPrint("Resetting NetPatch (just in case)")
        _mpPatch.patch.NetPatch.reset()
        return IsHotLoad(...)
    end)
end