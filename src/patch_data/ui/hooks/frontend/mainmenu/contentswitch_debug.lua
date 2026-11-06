if _mpPatch and _mpPatch.loaded then
    ContextPtr:SetShowHideHandler(function(isHide, _)
        if not isHide then
            _mpPatch.debugPrint("Content switch.")
        end
    end)
end