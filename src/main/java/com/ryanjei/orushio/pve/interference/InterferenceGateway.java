package com.ryanjei.orushio.pve.interference;

import com.ryanjei.orushio.pve.map.Cuboid;
import java.util.List;
import java.util.UUID;

public interface InterferenceGateway {
    ExecutionResult apply(UUID sessionId,String runtimeWorld,Cuboid finalRegion,List<UUID> participants,InterferenceType type,InterferenceSettings settings);
    record ExecutionResult(int affectedPlayerCount,int skippedPlayerCount){public ExecutionResult{if(affectedPlayerCount<0||skippedPlayerCount<0)throw new IllegalArgumentException("妨害人数が不正です。");}}
}
