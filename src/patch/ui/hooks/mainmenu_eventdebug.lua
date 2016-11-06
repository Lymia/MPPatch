if _mpPatch and _mpPatch.canEnable and _mpPatch.debug then
    pcall(function()
        _mpPatch.eventList = {
            "ActivePlayerTurnEnd", "ActivePlayerTurnStart", "AddPopupTextEvent", "AddUnitMoveHexRangeHex",
            "AdvisorDisplayHide", "AdvisorDisplayShow", "AfterModsActivate", "AfterModsDeactivate", "AILeaderMessage",
            "AudioDebugChangeMusic", "AudioPlay2DSound", "BuildingLibrarySwap", "CameraStartPitchingDown",
            "CameraStartPitchingUp", "CameraStartRotatingCCW", "CameraStartRotatingCW", "CameraStopPitchingDown",
            "CameraStopPitchingUp", "CameraStopRotatingCCW", "CameraStopRotatingCW", "CameraViewChanged",
            "ClearDiplomacyTradeTable", "ClearHexHighlights", "ClearHexHighlightStyle", "ClearUnitMoveHexRange",
            "ConnectedToNetworkHost", "EndCombatSim", "EndGameShow", "EndTurnBlockingChanged", "EndTurnTimerUpdate",
            "EventOpenOptionsScreen", "EventPoliciesDirty", "ExitToMainMenu", "FrontEndPopup", "GameMessageChat",
            "GameOptionsChanged", "GameplayAlertMessage", "GameplayFX", "GameplaySetActivePlayer",
            "GenericWorldAnchor", "GoToPediaHomePage", "GraphicsOptionsChanged", "HexFOWStateChanged",
            "HexYieldMightHaveChanged", "InitCityRangeStrike", "InstalledModsUpdated", "InterfaceModeChanged",
            "KeyUpEvent", "LandmarkLibrarySwap", "LeavingLeaderViewMode", "LoadScreenClose", "LocalMachineAppUpdate",
            "LocalMachineUnitPositionChanged", "MinimapClickedEvent", "MinimapTextureBroadcastEvent",
            "MultiplayerConnectionFailed", "MultiplayerGameAbandoned", "MultiplayerGameInvite",
            "MultiplayerGameLaunched", "MultiplayerGameListClear", "MultiplayerGameListComplete",
            "MultiplayerGamePlayerDisconnected", "MultiplayerGamePlayerUpdated", "MultiplayerJoinRoomComplete",
            "MultiplayerJoinRoomFailed", "MultiplayerPingTimesChanged", "NaturalWonderRevealed", "NotificationAdded",
            "NotificationRemoved", "OpenInfoCorner", "OpenPlayerDealScreenEvent", "ParticleEffectReloadRequested",
            "ParticleEffectStatsRequested", "ParticleEffectStatsResponse", "PlayerChoseToLoadGame",
            "PlayerVersionMismatchEvent", "PreGameDirty", "RemotePlayerTurnEnd", "RemoveAllArrowsEvent",
            "RequestYieldDisplay", "RunCombatSim", "SearchForPediaEntry", "SequenceGameInitComplete",
            "SerialEventBuildingSizeChanged", "SerialEventCameraIn", "SerialEventCameraOut",
            "SerialEventCameraStartMovingBack", "SerialEventCameraStartMovingForward",
            "SerialEventCameraStartMovingLeft", "SerialEventCameraStartMovingRight", "SerialEventCameraStopMovingBack",
            "SerialEventCameraStopMovingForward", "SerialEventCameraStopMovingLeft",
            "SerialEventCameraStopMovingRight", "SerialEventCityCaptured", "SerialEventCityContinentChanged",
            "SerialEventCityCreated", "SerialEventCityCultureChanged", "SerialEventCityDestroyed",
            "SerialEventCityHexHighlightDirty", "SerialEventCityInfoDirty", "SerialEventCityPopulationChanged",
            "SerialEventCityScreenDirty", "SerialEventCitySetDamage", "SerialEventDawnOfManHide",
            "SerialEventDawnOfManShow", "SerialEventEndTurnDirty", "SerialEventEnterCityScreen",
            "SerialEventEraChanged", "SerialEventEspionageScreenDirty", "SerialEventExitCityScreen",
            "SerialEventGameDataDirty", "SerialEventGameMessagePopup", "SerialEventGameMessagePopupProcessed",
            "SerialEventGameMessagePopupShown", "SerialEventHexCultureChanged", "SerialEventHexDeSelected",
            "SerialEventHexGridOff", "SerialEventHexGridOn", "SerialEventHexHighlight", "SerialEventHexSelected",
            "SerialEventImprovementCreated", "SerialEventImprovementDestroyed", "SerialEventMouseOverHex",
            "SerialEventRawResourceCreated", "SerialEventRawResourceIconCreated", "SerialEventResearchDirty",
            "SerialEventRoadCreated", "SerialEventRoadDestroyed", "SerialEventScoreDirty", "SerialEventStartGame",
            "SerialEventTurnTimerDirty", "SerialEventUnitCreated", "SerialEventUnitDestroyed",
            "SerialEventUnitFlagSelected", "SerialEventUnitInfoDirty", "SerialEventUnitMove",
            "SerialEventUnitMoveToHexes", "SerialEventUnitSetDamage", "SerialEventUnitTeleportedToHex",
            "ShowAttackTargets", "ShowHexYield", "ShowMovementRange", "SpawnArrowEvent", "SpecificCityInfoDirty",
            "StartUnitMoveHexRange", "StrategicViewStateChanged", "SystemUpdateUI", "TaskListUpdate", "TechAcquired",
            "UIPathFinderUpdate", "UnitActionChanged", "UnitDataEdited", "UnitDataRequested", "UnitDebugFSM",
            "UnitEmbark", "UnitFlagUpdated", "UnitGarrison", "UnitHexHighlight", "UnitLibrarySwap",
            "UnitMarkThreatening", "UnitMemberCombatStateChanged", "UnitMemberCombatTargetChanged",
            "UnitMemberOverlayAdd", "UnitMemberOverlayRemove", "UnitMemberOverlayShowHide",
            "UnitMemberPositionChanged", "UnitMoveQueueChanged", "UnitSelectionChanged", "UnitSelectionCleared",
            "UnitShouldDimFlag", "UnitStateChangeDetected", "UnitTypeChanged", "UnitVisibilityChanged",
            "UserRequestClose", "WarStateChanged", "WorldMouseOver",
        }
        local excludeList = { LocalMachineAppUpdate = true, KeyUpEvent = true, GameMessageChat = true }
        if not _mpPatch.patch.shared.eventHookInstalled then
            _mpPatch.patch.shared.eventHookInstalled = true

            local _mpPatch, unpack = _mpPatch, unpack
            _mpPatch.debugPrint("Enabling event logging...")
            for _, event in ipairs(_mpPatch.eventList) do
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