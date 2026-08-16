package com.ryanjei.orushio.pve.map;

import com.ryanjei.orushio.pve.domain.MapProfileId;
import java.time.Instant;
import java.util.*;

public record MapProfile(MapProfileId mapId, String displayName, boolean enabled, String templateDirectory,
                         Map<String,List<BlockPoint>> points, Map<String,List<Cuboid>> areas, Instant createdAt) {
    public static final Set<String> REQUIRED_SINGLE_POINTS=Set.of("farmSpawn","combatEntry","finalCore","finalEntryTrigger");
    public static final Map<String,Integer> REQUIRED_POINT_COUNTS=Map.of("normalCoreCandidates",3,"shopPoints",1);
    public static final Map<String,Integer> REQUIRED_AREA_COUNTS=Map.of("farmRegion",1,"enemyZones",1,"finalRegion",1,"gateRegions",1);
    public MapProfile {
        Objects.requireNonNull(mapId); if(displayName==null||displayName.isBlank())throw new IllegalArgumentException("表示名が必要です。");
        if(templateDirectory==null||templateDirectory.isBlank())throw new IllegalArgumentException("原本識別子が必要です。");
        points=immutable(points);areas=immutable(areas);Objects.requireNonNull(createdAt);
        if(enabled&&!missing(points,areas).isEmpty())throw new IllegalArgumentException("必須設定が不足しているため有効化できません。");
    }
    private static <T> Map<String,List<T>> immutable(Map<String,List<T>> source){Map<String,List<T>> copy=new LinkedHashMap<>();source.forEach((k,v)->copy.put(k,List.copyOf(v)));return Collections.unmodifiableMap(copy);}
    public List<String> missingRequirements(){return missing(points,areas);}private static List<String> missing(Map<String,List<BlockPoint>> points,Map<String,List<Cuboid>> areas){List<String> missing=new ArrayList<>();for(String key:REQUIRED_SINGLE_POINTS)if(points.getOrDefault(key,List.of()).isEmpty())missing.add(key);REQUIRED_POINT_COUNTS.forEach((k,n)->{if(points.getOrDefault(k,List.of()).size()<n)missing.add(k);});REQUIRED_AREA_COUNTS.forEach((k,n)->{if(areas.getOrDefault(k,List.of()).size()<n)missing.add(k);});return List.copyOf(missing);}
    public boolean setupComplete(){return missingRequirements().isEmpty();}
    public MapProfile withEnabled(boolean value){return new MapProfile(mapId,displayName,value,templateDirectory,points,areas,createdAt);}
    public MapProfile withDraft(Map<String,List<BlockPoint>> newPoints,Map<String,List<Cuboid>> newAreas){return new MapProfile(mapId,displayName,false,templateDirectory,newPoints,newAreas,createdAt);}
}
