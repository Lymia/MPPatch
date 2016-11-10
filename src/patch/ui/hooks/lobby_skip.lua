if _mpPatch and _mpPatch.loaded then
    if ContextPtr:LookUpControl(".."):GetID() == "ModMultiplayerSelectScreen" then
        ContextPtr:SetShowHideHandler(function(bIsHide, _)
            if not bIsHide then
                HostButtonClick()
                UIManager:DequeuePopup(ContextPtr)
            end
        end)
    end
end
