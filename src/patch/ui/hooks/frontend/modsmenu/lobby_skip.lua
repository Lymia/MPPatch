if _mpPatch and _mpPatch.loaded and ContextPtr:LookUpControl(".."):GetID() == "ModMultiplayerSelectScreen" then
    ContextPtr:SetShowHideHandler(function(bIsHide, _)
        if not bIsHide then
            HostButtonClick()
            UIManager:DequeuePopup(ContextPtr)
        end
    end)
end
