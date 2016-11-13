if _mpPatch and _mpPatch.loaded and _mpPatch.isModding then
    OnChat = _mpPatch.interceptChatFunction(OnChat, function(fromPlayer)
        return not not m_PlayerNames[fromPlayer]
    end)
end
