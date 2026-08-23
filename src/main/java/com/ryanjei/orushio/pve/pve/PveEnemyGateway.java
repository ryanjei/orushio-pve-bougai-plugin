package com.ryanjei.orushio.pve.pve;

import com.ryanjei.orushio.pve.map.Cuboid;
import java.util.*;
public interface PveEnemyGateway {
    Optional<UUID> spawn(EnemySpawnRequest request,int maxCandidateAttempts);
    Set<UUID> cleanupOwned(UUID sessionId,String runtimeWorld);
    Set<UUID> removeOwnedInside(UUID sessionId,String runtimeWorld,Cuboid region);
    Optional<EnemyOwnership> ownership(UUID entityId);
}
