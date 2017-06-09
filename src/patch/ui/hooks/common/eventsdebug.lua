if _mpPatch and _mpPatch.loaded and _mpPatch.debug then
    pcall(function()
        local eventList = {
            "AILeaderMessage", "AIProcessingEndedForPlayer", "AIProcessingStartedForPlayer", "AIProcessingStepChanged",
            "ActivePlayerTurnEnd", "ActivePlayerTurnStart", "AddPopupTextEvent", "AddUnitMoveHexRangeHex",
            "AdvisorDisplayHide", "AdvisorDisplayShow", "AfterModsActivate", "AfterModsDeactivate",
            "AnimationSamplingChanged", "AppInitComplete", "AudioAdvanceCurrentPlaylistTrack", "AudioDebugChangeMusic",
            "AudioPlay2DSound", "AudioRewindCurrentPlaylistTrack", "AudioVolumeChanged", "BeforeModsActivate",
            "BeforeModsDeactivate", "BuildingLibrarySwap", "CameraProjectionChanged", "CameraStartPitchingDown",
            "CameraStartPitchingUp", "CameraStartRotatingCCW", "CameraStartRotatingCW", "CameraStopPitchingDown",
            "CameraStopPitchingUp", "CameraStopRotatingCCW", "CameraStopRotatingCW", "CameraViewChanged",
            "CityHandleCreated", "CityRuinsCreated", "ClearDiplomacyTradeTable", "ClearHexHighlightStyle",
            "ClearHexHighlights", "ClearUnitMoveHexRange", "ConnectedToNetworkHost", "DisplayMovementIndicator",
            "DontRecordCommandStreams", "DragCamera", "EndCombatSim", "EndGameShow", "EndTurnBlockingChanged",
            "EndTurnTimerUpdate", "EndUnitMoveHexRange", "EventOpenOptionsScreen", "EventPoliciesDirty",
            "Event_ToggleTradeRouteDisplay", "ExitToMainMenu", "FrontEndPopup", "GameMessageChat",
            "GameOptionsChanged", "GameViewTypeChanged", "GameplayAlertMessage", "GameplayFX",
            "GameplaySetActivePlayer", "GenericWorldAnchor", "GlobalUnitScale", "GoToPediaHomePage", "GoldenAgeEnded",
            "GoldenAgeStarted", "GraphicsOptionsChanged", "HexFOWStateChanged", "HexYieldMightHaveChanged",
            "InitCityRangeStrike", "InterfaceModeChanged", "KeyUpEvent", "LandmarkLibrarySwap", "LanguageChanging",
            "Leaderboard_ScoresDownloaded", "LeavingLeaderViewMode", "LoadScreenClose", "LocalMachineAppUpdate",
            "LocalMachineUnitPositionChanged", "LuaEvent", "MinimapClickedEvent", "MinimapTextureBroadcastEvent",
            "MultiplayerConnectionComplete", "MultiplayerConnectionFailed", "MultiplayerGameAbandoned",
            "MultiplayerGameHostMigration", "MultiplayerGameLastPlayer", "MultiplayerGameLaunched",
            "MultiplayerGameListClear", "MultiplayerGameListComplete", "MultiplayerGameListUpdated",
            "MultiplayerGameLobbyInvite", "MultiplayerGamePlayerDisconnected", "MultiplayerGamePlayerUpdated",
            "MultiplayerGameServerInvite", "MultiplayerHotJoinCompleted", "MultiplayerHotJoinStarted",
            "MultiplayerJoinRoomAttempt", "MultiplayerJoinRoomComplete", "MultiplayerJoinRoomFailed",
            "MultiplayerNetRegistered", "MultiplayerPingTimesChanged", "MultiplayerProfileDisconnected",
            "MultiplayerProfileFailed", "NaturalWonderRevealed", "NewGameTurn", "NotificationActivated",
            "NotificationAdded", "NotificationRemoved", "NotifyAILeaderInGame", "OpenInfoCorner",
            "OpenPlayerDealScreenEvent", "ParticleEffectReloadRequested", "ParticleEffectStatsRequested",
            "ParticleEffectStatsResponse", "PlayerChoseToLoadGame", "PlayerChoseToLoadMap",
            "PlayerVersionMismatchEvent", "PreGameDirty", "RecordCommandStreams", "RemotePlayerTurnEnd",
            "RemotePlayerTurnStart", "RemoveAllArrowsEvent", "RequestYieldDisplay", "RunCombatSim",
            "SearchForPediaEntry", "SequenceGameInitComplete", "SerialEventBuildingSizeChanged",
            "SerialEventCameraBack", "SerialEventCameraForward", "SerialEventCameraIn", "SerialEventCameraLeft",
            "SerialEventCameraOut", "SerialEventCameraRight", "SerialEventCameraSetCenterAndZoom",
            "SerialEventCameraStartMovingBack", "SerialEventCameraStartMovingForward",
            "SerialEventCameraStartMovingLeft", "SerialEventCameraStartMovingRight", "SerialEventCameraStopMovingBack",
            "SerialEventCameraStopMovingForward", "SerialEventCameraStopMovingLeft",
            "SerialEventCameraStopMovingRight", "SerialEventCityCaptured", "SerialEventCityContinentChanged",
            "SerialEventCityCreated", "SerialEventCityCultureChanged", "SerialEventCityDestroyed",
            "SerialEventCityHexHighlightDirty", "SerialEventCityInfoDirty", "SerialEventCityPopulationChanged",
            "SerialEventCityScreenDirty", "SerialEventCitySetDamage", "SerialEventDawnOfManHide",
            "SerialEventDawnOfManShow", "SerialEventEndTurnDirty", "SerialEventEnterCityScreen",
            "SerialEventEraChanged", "SerialEventEspionageScreenDirty", "SerialEventExitCityScreen",
            "SerialEventFeatureCreated", "SerialEventFeatureDestroyed", "SerialEventForestCreated",
            "SerialEventForestRemoved", "SerialEventGameDataDirty", "SerialEventGameMessagePopup",
            "SerialEventGameMessagePopupProcessed", "SerialEventGameMessagePopupShown",
            "SerialEventGreatWorksScreenDirty", "SerialEventHexCultureChanged", "SerialEventHexDeSelected",
            "SerialEventHexGridOff", "SerialEventHexGridOn", "SerialEventHexHighlight", "SerialEventHexSelected",
            "SerialEventImprovementCreated", "SerialEventImprovementDestroyed", "SerialEventImprovementIconCreated",
            "SerialEventImprovementIconDestroyed", "SerialEventInfoPaneDirty", "SerialEventJungleCreated",
            "SerialEventJungleRemoved", "SerialEventLeaderToggleDebugCam", "SerialEventLeagueScreenDirty",
            "SerialEventMouseOverHex", "SerialEventRawResourceCreated", "SerialEventRawResourceDestroyed",
            "SerialEventRawResourceIconCreated", "SerialEventRawResourceIconDestroyed", "SerialEventResearchDirty",
            "SerialEventRoadCreated", "SerialEventRoadDestroyed", "SerialEventScoreDirty", "SerialEventScreenShot",
            "SerialEventStartGame", "SerialEventTerrainDecalCreated", "SerialEventTerrainOverlayMod",
            "SerialEventTest", "SerialEventTestAnimations", "SerialEventTurnTimerDirty", "SerialEventUnitCreated",
            "SerialEventUnitDestroyed", "SerialEventUnitFacingChanged", "SerialEventUnitFlagSelected",
            "SerialEventUnitInfoDirty", "SerialEventUnitMove", "SerialEventUnitMoveToHexes",
            "SerialEventUnitSetDamage", "SerialEventUnitTeleportedToHex", "ShowAttackTargets", "ShowHexYield",
            "ShowMovementRange", "ShowPlayerChangeUI", "SpawnArrowEvent", "SpecificCityInfoDirty",
            "StartUnitMoveHexRange", "StateMachineDumpStates", "StateMachineRequestStates",
            "StrategicViewStateChanged", "SystemUpdateUI", "TaskListUpdate", "TeamMet", "TechAcquired",
            "ToggleDisplayUnits", "ToolTipEvent", "UIPathFinderUpdate", "UnitActionChanged", "UnitDataEdited",
            "UnitDataRequested", "UnitDebugFSM", "UnitEmbark", "UnitFlagUpdated", "UnitGarrison", "UnitHandleCreated",
            "UnitHexHighlight", "UnitLibrarySwap", "UnitMarkThreatening", "UnitMemberCombatStateChanged",
            "UnitMemberCombatTargetChanged", "UnitMemberOverlayAdd", "UnitMemberOverlayMessage",
            "UnitMemberOverlayRemove", "UnitMemberOverlayShowHide", "UnitMemberOverlayTargetColor",
            "UnitMemberPositionChanged", "UnitMoveQueueChanged", "UnitNameChanged", "UnitSelectionChanged",
            "UnitSelectionCleared", "UnitShouldDimFlag", "UnitStateChangeDetected", "UnitTypeChanged",
            "UnitVisibilityChanged", "UserRequestClose", "VisibilityUpdated", "WarStateChanged", "WonderStateChanged",
            "WonderTogglePlacement", "WonderTypeChanged", "WorldMouseOver"
        }
        local excludeList = { LocalMachineAppUpdate = true, KeyUpEvent = true, SerialEventMouseOverHex = true,
                              WorldMouseOver = true, UnitVisibilityChanged = true, VisibilityUpdated = true,
                              MultiplayerPingTimesChanged = true, }
        local excludePrefixes = { "SerialEventCamera", "Camera", "Show" }

        local testEventHandlerPing = "7f176dfe-4d49-11e7-b2f0-62ba1ac45905"
        local testEventHandlerPong = "9eb4260c-4d49-11e7-bc4d-62ba1ac45905"

        local eventHookIsActive = false
        local function LuaEventHandler(event)
            if event == testEventHandlerPong then
                eventHookIsActive = true
            end
        end
        Events.LuaEvent.Add(LuaEventHandler)
        Events.LuaEvent(testEventHandlerPing)
        Events.LuaEvent.Remove(LuaEventHandler)

        if not eventHookIsActive then
            local _mpPatch, unpack = _mpPatch, unpack
            _mpPatch.debugPrint("Enabling event logging...")

            for _, event in ipairs(eventList) do
                local eventObj = Events[event]
                if not eventObj then
                    _mpPatch.debugPrint("Could not log event "..event.." (does not exist)")
                elseif not excludeList[event] then
                    local isExcluded = false
                    for _, prefix in ipairs(excludePrefixes) do
                        isExcluded = isExcluded or _mpPatch.strStarts(event, prefix)
                    end
                    if not isExcluded then
                        eventObj.Add(function(...)
                            local args = {...}
                            _mpPatch.debugPrint("Got event: "..event.."\n",unpack(args))
                        end)
                    end
                end
            end
            Event.LuaEvent.Add(function(event)
                if event == testEventHandlerPing then
                    Event.LuaEvent(testEventHandlerPong)
                end
            end)
        else
            _mpPatch.debugPrint("Event logging already enabled.")
        end
    end)
end