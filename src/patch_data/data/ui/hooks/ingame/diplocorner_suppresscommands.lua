if _mpPatch and _mpPatch.loaded and _mpPatch.isModding then
    _mpPatch.hooks.protocol_chathandler_setupHooks()
    _mpPatch.interceptGlobalWrite("OnChat", _mpPatch.hooks.protocol_chathandler_new)
    Events.MultiplayerGamePlayerDisconnected.Add(function(...)
        if not ContextPtr:IsHidden() then
            _mpPatch.hooks.protocol_chathandler_onDisconnect(...)
        end
    end)
end