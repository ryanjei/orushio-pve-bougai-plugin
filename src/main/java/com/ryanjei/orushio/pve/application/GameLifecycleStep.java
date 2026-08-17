package com.ryanjei.orushio.pve.application;
import com.ryanjei.orushio.pve.domain.GameSession;
import java.util.UUID;
public interface GameLifecycleStep{
    default void prepare(GameSession session){}
    /** Compensates this step's preparation. It must only remove resources owned by the supplied session. */
    default void rollbackPreparation(GameSession session){}
    /** Idempotent recovery cleanup for resources owned by the supplied session. */
    default void cleanup(GameSession session){}
    default void participantConnected(GameSession session,UUID playerId){}
    default void pendingCleanupConnected(GameSession session,UUID playerId){}
    default GameRuntimeView runtimeView(){return GameRuntimeView.idle();}
}
