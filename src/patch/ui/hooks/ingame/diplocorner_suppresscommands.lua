if _mpPatch and _mpPatch.loaded and _mpPatch.isModding then
    _mpPatch.interceptGlobalWrite("OnChat", _mpPatch.interceptChatFunction)
end