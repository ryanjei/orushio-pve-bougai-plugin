package com.ryanjei.orushio.pve.progression;

import com.ryanjei.orushio.pve.map.BlockPoint;
import java.util.UUID;

public interface FinalAreaGateway{
 void teleport(UUID sessionId,String runtimeWorld,UUID playerId,BlockPoint destination);
 void notifyLocked(UUID sessionId,String runtimeWorld,UUID playerId);
}
