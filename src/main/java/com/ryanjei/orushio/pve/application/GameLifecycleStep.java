package com.ryanjei.orushio.pve.application;
import com.ryanjei.orushio.pve.domain.GameSession;
public interface GameLifecycleStep{
    default void prepare(GameSession session){}
    /** Compensates this step's preparation. It must only remove resources owned by the supplied session. */
    default void rollbackPreparation(GameSession session){}
    /** Idempotent recovery cleanup for resources owned by the supplied session. */
    default void cleanup(GameSession session){}
}
