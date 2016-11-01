if _mpPatch and _mpPatch.enabled then
    _mpPatch.loadElementFromProxy("mppatch_multiplayerproxy", "ModMultiplayerSelectScreen")

    Controls.MultiPlayerButton:RegisterCallback(Mouse.eLClick, function()
        UIManager:QueuePopup(Controls.ModMultiplayerSelectScreen, PopupPriority.ModMultiplayerSelectScreen)
    end)
    Controls.MultiPlayerButton:SetHide(false)
end