package com.ryanjei.orushio.pve.application;
import com.ryanjei.orushio.pve.domain.GameSession;
import java.util.*;
public interface GameLifecycleStep{
    default void prepare(GameSession session){}
    /** Drops preparation-only rollback evidence after the session has safely become ACTIVE. */
    default void preparationCommitted(GameSession session){}
    /** Compensates this step's preparation. It must only remove resources owned by the supplied session. */
    default void rollbackPreparation(GameSession session){}
    /** Idempotent recovery cleanup for resources owned by the supplied session. */
    default void cleanup(GameSession session){}
    /** Lower values clean first. World-owning steps must run after entity/player cleanup. */
    default int cleanupOrder(){return 0;}
    default void participantConnected(GameSession session,UUID playerId){}
    default void rollbackParticipantConnection(GameSession session,UUID playerId){}
    default void participantConnectionCommitted(GameSession session,UUID playerId){}
    default void participantDisconnected(GameSession session,UUID playerId){}
    default void participantRespawned(GameSession session,UUID playerId){}
    /** Returns true when this step selected and applied the session-owned respawn destination. */
    default boolean participantRespawnedHandled(GameSession session,UUID playerId){return false;}
    /** Non-critical presentation hook. Cleanup must continue if this hook fails. */
    default void clearStarted(GameSession session){}
    default boolean ownsRuntime(GameSession session,String worldName){return false;}
    /** Returns true only when session-owned pending cleanup completed successfully. */
    default boolean pendingCleanupConnected(GameSession session,UUID playerId){return false;}
    default Set<UUID> pendingCleanupPlayers(GameSession session){return Set.of();}
    default Set<UUID> completedCleanupPlayers(GameSession session){return Set.of();}
    default boolean hasPendingCleanup(UUID playerId){return false;}
    default GameRuntimeView runtimeView(){return GameRuntimeView.idle();}
    default com.ryanjei.orushio.pve.pve.PveRuntimeView pveRuntimeView(){return com.ryanjei.orushio.pve.pve.PveRuntimeView.idle();}
    default com.ryanjei.orushio.pve.progression.FinalAreaProgressionView progressionView(){return com.ryanjei.orushio.pve.progression.FinalAreaProgressionView.idle();}
}
