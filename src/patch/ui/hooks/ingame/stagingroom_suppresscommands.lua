if _mpPatch_activateInGame then
    _mpPatch.hooks.protocol_chathandler_setupHooks()
    OnChat = _mpPatch.hooks.protocol_chathandler_new(OnChat)
    Events.MultiplayerGamePlayerDisconnected.Add(function(...)
        if not ContextPtr:IsHidden() then
            _mpPatch.hooks.protocol_chathandler_onDisconnect(...)
        end
    end)
end