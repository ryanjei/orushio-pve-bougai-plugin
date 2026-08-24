package com.ryanjei.orushio.pve.pve;

import com.ryanjei.orushio.pve.map.Cuboid;
import java.util.*;
public record EnemySpawnRequest(UUID sessionId,String runtimeWorld,UUID enemyZoneId,Cuboid region,Cuboid farmRegion,Cuboid participantRegion,EnemyType type,double minParticipantDistance,double maxParticipantDistance){public EnemySpawnRequest{Objects.requireNonNull(sessionId);Objects.requireNonNull(enemyZoneId);Objects.requireNonNull(region);Objects.requireNonNull(farmRegion);Objects.requireNonNull(type);if(runtimeWorld==null||runtimeWorld.isBlank())throw new IllegalArgumentException("Runtime Worldが必要です。");}public EnemySpawnRequest(UUID sessionId,String runtimeWorld,UUID enemyZoneId,Cuboid region,Cuboid farmRegion,EnemyType type,double minParticipantDistance,double maxParticipantDistance){this(sessionId,runtimeWorld,enemyZoneId,region,farmRegion,null,type,minParticipantDistance,maxParticipantDistance);}}
