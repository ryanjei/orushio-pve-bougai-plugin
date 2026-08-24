package com.ryanjei.orushio.pve.application;

import com.ryanjei.orushio.pve.domain.GameSession;
import com.ryanjei.orushio.pve.domain.GameLaunchSettings;
import java.time.Instant;
import java.util.UUID;

public interface GameApplicationService {
    GameSession current();
    OperationResult startRecruiting(String expectedState);
    OperationResult closeRecruiting(String expectedSessionId);
    OperationResult startMapSetup(String expectedState);
    void validateMapSetup(String expectedSessionId);
    OperationResult closeMapSetup(String expectedSessionId);
    default GameSession addParticipant(UUID playerId){throw new UnsupportedOperationException();}
    default GameSession removeParticipant(UUID playerId){throw new UnsupportedOperationException();}
    default GameStartView startView(String mapId){throw new UnsupportedOperationException();}
    default GameLaunchSettings saveLaunchSettings(String mapId,GameLaunchSettings settings){throw new UnsupportedOperationException();}
    default OperationResult prepareGame(String expectedState,String mapId){throw new UnsupportedOperationException();}
    default OperationResult activateGame(String expectedSessionId,Instant now){throw new UnsupportedOperationException();}
    default OperationResult abortGame(String expectedSessionId,String reason){throw new UnsupportedOperationException();}
    default boolean expireIfNeeded(Instant now){return false;}
    default void playerConnected(UUID playerId,String name){}
    default void playerDisconnected(UUID playerId){}
    default boolean playerDied(UUID playerId,String worldName){return false;}
    default boolean playerRespawned(UUID playerId){return false;}
    default OperationResult completeGame(UUID sessionId){throw new UnsupportedOperationException();}
    default OperationResult recoverClearGame(UUID sessionId){throw new UnsupportedOperationException();}
    default int participantLimit(){return ParticipantPolicy.standard().maxParticipants();}
    default GameRuntimeView runtimeView(){return GameRuntimeView.idle();}
    default com.ryanjei.orushio.pve.pve.PveRuntimeView pveRuntimeView(){return com.ryanjei.orushio.pve.pve.PveRuntimeView.idle();}
    default com.ryanjei.orushio.pve.progression.FinalAreaProgressionView progressionView(){return com.ryanjei.orushio.pve.progression.FinalAreaProgressionView.idle();}
}
