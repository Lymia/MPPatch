-- Copyright (c) 2015-2016 Lymia Alusyia <lymia@lymiahugs.com>
--
-- Permission is hereby granted, free of charge, to any person obtaining a copy
-- of this software and associated documentation files (the "Software"), to deal
-- in the Software without restriction, including without limitation the rights
-- to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
-- copies of the Software, and to permit persons to whom the Software is
-- furnished to do so, subject to the following conditions:
--
-- The above copyright notice and this permission notice shall be included in
-- all copies or substantial portions of the Software.
--
-- THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
-- IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
-- FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
-- AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
-- LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
-- OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
-- THE SOFTWARE.

_mpPatch.eventList = {
    "ActivePlayerTurnEnd", "ActivePlayerTurnStart", "AddPopupTextEvent", "AddUnitMoveHexRangeHex",
    "AdvisorDisplayHide", "AdvisorDisplayShow", "AfterModsActivate", "AfterModsDeactivate", "AILeaderMessage",
    "AudioDebugChangeMusic", "AudioPlay2DSound", "BuildingLibrarySwap", "CameraStartPitchingDown",
    "CameraStartPitchingUp", "CameraStartRotatingCCW", "CameraStartRotatingCW", "CameraStopPitchingDown",
    "CameraStopPitchingUp", "CameraStopRotatingCCW", "CameraStopRotatingCW", "CameraViewChanged",
    "ClearDiplomacyTradeTable", "ClearHexHighlights", "ClearHexHighlightStyle", "ClearUnitMoveHexRange",
    "ConnectedToNetworkHost", "EndCombatSim", "EndGameShow", "EndTurnBlockingChanged", "EndTurnTimerUpdate",
    "EventOpenOptionsScreen", "EventPoliciesDirty", "ExitToMainMenu", "FrontEndPopup", "GameMessageChat",
    "GameOptionsChanged", "GameplayAlertMessage", "GameplayFX", "GameplaySetActivePlayer", "GenericWorldAnchor",
    "GoToPediaHomePage", "GraphicsOptionsChanged", "HexFOWStateChanged", "HexYieldMightHaveChanged",
    "InitCityRangeStrike", "InstalledModsUpdated", "InterfaceModeChanged", "KeyUpEvent", "LandmarkLibrarySwap",
    "LeavingLeaderViewMode", "LoadScreenClose", "LocalMachineAppUpdate", "LocalMachineUnitPositionChanged",
    "MinimapClickedEvent", "MinimapTextureBroadcastEvent", "MultiplayerConnectionFailed", "MultiplayerGameAbandoned",
    "MultiplayerGameInvite", "MultiplayerGameLaunched", "MultiplayerGameListClear", "MultiplayerGameListComplete",
    "MultiplayerGamePlayerDisconnected", "MultiplayerGamePlayerUpdated", "MultiplayerJoinRoomComplete",
    "MultiplayerJoinRoomFailed", "MultiplayerPingTimesChanged", "NaturalWonderRevealed", "NotificationAdded",
    "NotificationRemoved", "OpenInfoCorner", "OpenPlayerDealScreenEvent", "ParticleEffectReloadRequested",
    "ParticleEffectStatsRequested", "ParticleEffectStatsResponse", "PlayerChoseToLoadGame",
    "PlayerVersionMismatchEvent", "PreGameDirty", "RemotePlayerTurnEnd", "RemoveAllArrowsEvent", "RequestYieldDisplay",
    "RunCombatSim", "SearchForPediaEntry", "SequenceGameInitComplete", "SerialEventBuildingSizeChanged",
    "SerialEventCameraIn", "SerialEventCameraOut", "SerialEventCameraStartMovingBack",
    "SerialEventCameraStartMovingForward", "SerialEventCameraStartMovingLeft", "SerialEventCameraStartMovingRight",
    "SerialEventCameraStopMovingBack", "SerialEventCameraStopMovingForward", "SerialEventCameraStopMovingLeft",
    "SerialEventCameraStopMovingRight", "SerialEventCityCaptured", "SerialEventCityContinentChanged",
    "SerialEventCityCreated", "SerialEventCityCultureChanged", "SerialEventCityDestroyed",
    "SerialEventCityHexHighlightDirty", "SerialEventCityInfoDirty", "SerialEventCityPopulationChanged",
    "SerialEventCityScreenDirty", "SerialEventCitySetDamage", "SerialEventDawnOfManHide", "SerialEventDawnOfManShow",
    "SerialEventEndTurnDirty", "SerialEventEnterCityScreen", "SerialEventEraChanged", "SerialEventEspionageScreenDirty",
    "SerialEventExitCityScreen", "SerialEventGameDataDirty", "SerialEventGameMessagePopup",
    "SerialEventGameMessagePopupProcessed", "SerialEventGameMessagePopupShown", "SerialEventHexCultureChanged",
    "SerialEventHexDeSelected", "SerialEventHexGridOff", "SerialEventHexGridOn", "SerialEventHexHighlight",
    "SerialEventHexSelected", "SerialEventImprovementCreated", "SerialEventImprovementDestroyed",
    "SerialEventMouseOverHex", "SerialEventRawResourceCreated", "SerialEventRawResourceIconCreated",
    "SerialEventResearchDirty", "SerialEventRoadCreated", "SerialEventRoadDestroyed", "SerialEventScoreDirty",
    "SerialEventStartGame", "SerialEventTurnTimerDirty", "SerialEventUnitCreated", "SerialEventUnitDestroyed",
    "SerialEventUnitFlagSelected", "SerialEventUnitInfoDirty", "SerialEventUnitMove", "SerialEventUnitMoveToHexes",
    "SerialEventUnitSetDamage", "SerialEventUnitTeleportedToHex", "ShowAttackTargets", "ShowHexYield",
    "ShowMovementRange", "SpawnArrowEvent", "SpecificCityInfoDirty", "StartUnitMoveHexRange",
    "StrategicViewStateChanged", "SystemUpdateUI", "TaskListUpdate", "TechAcquired", "UIPathFinderUpdate",
    "UnitActionChanged", "UnitDataEdited", "UnitDataRequested", "UnitDebugFSM", "UnitEmbark", "UnitFlagUpdated",
    "UnitGarrison", "UnitHexHighlight", "UnitLibrarySwap", "UnitMarkThreatening", "UnitMemberCombatStateChanged",
    "UnitMemberCombatTargetChanged", "UnitMemberOverlayAdd", "UnitMemberOverlayRemove", "UnitMemberOverlayShowHide",
    "UnitMemberPositionChanged", "UnitMoveQueueChanged", "UnitSelectionChanged", "UnitSelectionCleared",
    "UnitShouldDimFlag", "UnitStateChangeDetected", "UnitTypeChanged", "UnitVisibilityChanged", "UserRequestClose",
    "WarStateChanged", "WorldMouseOver",
}
local excludeList = {LocalMachineAppUpdate = true}
if not _mpPatch.patch.shared.eventHookInstalled then
    _mpPatch.patch.shared.eventHookInstalled = true

    local print, unpack = print, unpack
    for _, event in ipairs(_mpPatch.eventList) do
        local eventObj = Events[event]
        if excludeList[event] then
            print("Event "..event.." ignored for logging.")
        elseif not eventObj then
            print("Could not log event "..event.." (does not exist)")
        else
            print("Logging event "..event)
            eventObj.Add(function(...)
                local args = {...}
                print("Got event: "..event.."\n",unpack(args))
            end)
        end
    end
end