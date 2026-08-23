package com.ryanjei.orushio.pve.economy;
import com.ryanjei.orushio.pve.map.BlockPoint;import java.util.UUID;
public interface FarmEconomyGateway{boolean respawn(UUID sessionId,String runtimeWorldName,BlockPoint position,String material);boolean giveItem(UUID sessionId,UUID playerId,ItemSpec item);}
