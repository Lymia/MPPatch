if _mpPatch_activateInGame then
    OnChat = _mpPatch.interceptChatFunction(OnChat)
end