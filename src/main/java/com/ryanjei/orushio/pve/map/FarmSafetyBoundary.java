package com.ryanjei.orushio.pve.map;

import com.ryanjei.orushio.pve.domain.MapProfileId;
import java.util.*;

public record FarmSafetyBoundary(MapProfileId mapId,String runtimeWorldName,Cuboid region,BlockPoint spawn) {
    public FarmSafetyBoundary{Objects.requireNonNull(mapId);if(runtimeWorldName==null||runtimeWorldName.isBlank())throw new IllegalArgumentException("Runtime World名が必要です。");Objects.requireNonNull(region);Objects.requireNonNull(spawn);if(!contains(region,spawn))throw new IllegalArgumentException("farmSpawnがfarmRegion外です。");}
    public static FarmSafetyBoundary resolve(MapProfile profile,String runtimeWorldName){Cuboid region=profile.areas().getOrDefault("farmRegion",List.of()).stream().findFirst().orElseThrow(()->new IllegalArgumentException("farmRegionがありません。"));BlockPoint spawn=profile.points().getOrDefault("farmSpawn",List.of()).stream().findFirst().orElseThrow(()->new IllegalArgumentException("farmSpawnがありません。"));return new FarmSafetyBoundary(profile.mapId(),runtimeWorldName,region,spawn);}
    public boolean contains(String worldName,BlockPoint point){return runtimeWorldName.equals(worldName)&&contains(region,point);}
    public BlockPoint farmSpawn(String worldName){if(!runtimeWorldName.equals(worldName))throw new IllegalArgumentException("別Runtime WorldのFarmは参照できません。");return spawn;}
    private static boolean contains(Cuboid area,BlockPoint point){return point.x()>=area.minX()&&point.x()<=area.maxX()&&point.y()>=area.minY()&&point.y()<=area.maxY()&&point.z()>=area.minZ()&&point.z()<=area.maxZ();}
}
