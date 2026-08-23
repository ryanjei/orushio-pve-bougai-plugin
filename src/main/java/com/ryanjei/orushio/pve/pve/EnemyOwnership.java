package com.ryanjei.orushio.pve.pve;

import java.util.*;
public record EnemyOwnership(UUID sessionId, UUID enemyZoneId, String runtimeWorld) {
    public EnemyOwnership { Objects.requireNonNull(sessionId); Objects.requireNonNull(enemyZoneId); if(runtimeWorld==null||runtimeWorld.isBlank())throw new IllegalArgumentException("Runtime Worldが必要です。"); }
}
