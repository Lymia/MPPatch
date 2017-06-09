if _mpPatch and _mpPatch.loaded and _mpPatch.debug then
    pcall(function()
        local eventList = {
            "AIProcessingEndedForPlayer", "AIProcessingStepChanged", "ActivePlayerTurnEnd", "AddPopupTextEvent",
            "AddUnitMoveHexRangeHex", "AdvisorDisplayShow", "AfterModsActivate", "AfterModsDeactivate",
            "AudioDebugChangeMusic", "AudioRewindCurrentPlaylistTrack", "BeforeModsDeactivate", "BuildingLibrarySwap",
            "CameraProjectionChanged", "CameraStopPitchingDown", "CameraStopPitchingUp", "CameraViewChanged",
            "ClearDiplomacyTradeTable", "ClearUnitMoveHexRange", "ConnectedToNetworkHost", "DisplayMovementIndicator",
            "DontRecordCommandStreams", "DragCamera", "EndCombatSim", "EndGameShow", "EndTurnBlockingChanged",
            "EndTurnTimerUpdate", "EndUnitMoveHexRange", "EventPoliciesDirty", "Event_ToggleTradeRouteDisplay",
            "ExitToMainMenu", "FrontEndPopup", "GoldenAgeEnded", "GoldenAgeStarted", "GraphicsOptionsChanged",
            "HexYieldMightHaveChanged", "InitCityRangeStrike", "InterfaceModeChanged", "Leaderboard_ScoresDownloaded",
            "LeavingLeaderViewMode", "LoadScreenClose", "LocalMachineAppUpdate", "LocalMachineUnitPositionChanged",
            "MinimapClickedEvent", "MultiplayerConnectionComplete", "MultiplayerGameAbandoned",
            "MultiplayerGameLastPlayer", "MultiplayerGameListClear", "MultiplayerGameListComplete",
            "MultiplayerGameLobbyInvite", "MultiplayerGamePlayerDisconnected", "MultiplayerHotJoinStarted",
            "MultiplayerJoinRoomAttempt", "MultiplayerJoinRoomComplete", "MultiplayerJoinRoomFailed",
            "MultiplayerNetRegistered", "MultiplayerPingTimesChanged", "MultiplayerProfileFailed",
            "NaturalWonderRevealed", "NewGameTurn", "NotificationActivated", "NotificationAdded",
            "NotificationRemoved", "NotifyAILeaderInGame", "OpenPlayerDealScreenEvent", "ParticleEffectStatsRequested",
            "ParticleEffectStatsResponse", "PlayerVersionMismatchEvent", "RemotePlayerTurnEnd",
            "RemotePlayerTurnStart", "RemoveAllArrowsEvent", "RequestYieldDisplay", "RunCombatSim",
            "SearchForPediaEntry", "SerialEventCameraBack", "SerialEventCameraForward", "SerialEventCameraOut",
            "SerialEventCameraSetCenterAndZoom", "SerialEventCameraStartMovingLeft",
            "SerialEventCameraStartMovingRight", "SerialEventCameraStopMovingBack", "SerialEventCameraStopMovingLeft",
            "SerialEventCameraStopMovingRight", "SerialEventCityContinentChanged", "SerialEventCityCultureChanged",
            "SerialEventCityDestroyed", "SerialEventCityHexHighlightDirty", "SerialEventCityInfoDirty",
            "SerialEventCityScreenDirty", "SerialEventCitySetDamage", "SerialEventDawnOfManShow",
            "SerialEventEndTurnDirty", "SerialEventEnterCityScreen", "SerialEventEraChanged",
            "SerialEventExitCityScreen", "SerialEventFeatureCreated", "SerialEventFeatureDestroyed",
            "SerialEventForestRemoved", "SerialEventGameMessagePopup", "SerialEventGameMessagePopupProcessed",
            "SerialEventGameMessagePopupShown", "SerialEventHexCultureChanged", "SerialEventHexDeSelected",
            "SerialEventHexGridOff", "SerialEventHexHighlight", "SerialEventImprovementDestroyed",
            "SerialEventImprovementIconCreated", "SerialEventImprovementIconDestroyed", "SerialEventInfoPaneDirty",
            "SerialEventJungleCreated", "SerialEventLeaderToggleDebugCam", "SerialEventMouseOverHex",
            "SerialEventRawResourceDestroyed", "SerialEventResearchDirty", "SerialEventRoadDestroyed",
            "SerialEventScreenShot", "SerialEventStartGame", "SerialEventTest", "SerialEventTurnTimerDirty",
            "SerialEventUnitCreated", "SerialEventUnitFacingChanged", "SerialEventUnitFlagSelected",
            "SerialEventUnitInfoDirty", "SerialEventUnitMove", "SerialEventUnitSetDamage",
            "SerialEventUnitTeleportedToHex", "ShowHexYield", "ShowMovementRange", "ShowPlayerChangeUI",
            "SpawnArrowEvent", "StateMachineDumpStates", "StrategicViewStateChanged", "SystemUpdateUI",
            "TaskListUpdate", "TeamMet", "TechAcquired", "ToggleDisplayUnits", "ToolTipEvent", "UIPathFinderUpdate",
            "UnitActionChanged", "UnitDataEdited", "UnitEmbark", "UnitFlagUpdated", "UnitHandleCreated",
            "UnitHexHighlight", "UnitMemberOverlayRemove", "UnitMemberPositionChanged", "UnitMoveQueueChanged",
            "UnitNameChanged", "UnitSelectionCleared", "UnitShouldDimFlag", "UnitStateChangeDetected",
            "UnitTypeChanged", "UnitVisibilityChanged", "WarStateChanged", "WonderTypeChanged", "WorldMouseOver"
        }
        local excludeList = { LocalMachineAppUpdate = true, KeyUpEvent = true, GameMessageChat = true }
        if not _mpPatch.patch.shared.eventHookInstalled then
            _mpPatch.patch.shared.eventHookInstalled = true

            local _mpPatch, unpack = _mpPatch, unpack
            _mpPatch.debugPrint("Enabling event logging...")
            for _, event in ipairs(eventList) do
                local eventObj = Events[event]
                if not eventObj then
                    _mpPatch.debugPrint("Could not log event "..event.." (does not exist)")
                elseif not excludeList[event] then
                    eventObj.Add(function(...)
                        local args = {...}
                        _mpPatch.debugPrint("Got event: "..event.."\n",unpack(args))
                    end)
                end
            end
        end
    end)
end