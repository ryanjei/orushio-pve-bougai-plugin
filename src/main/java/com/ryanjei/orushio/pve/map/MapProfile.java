package com.ryanjei.orushio.pve.map;

import com.ryanjei.orushio.pve.domain.MapProfileId;
import java.time.Instant;
import java.util.*;

public record MapProfile(MapProfileId mapId, String displayName, boolean enabled, String templateDirectory,
                         Map<String,List<BlockPoint>> points, Map<String,List<Cuboid>> areas, Instant createdAt,
                         List<SpawnMarker> spawnMarkers,List<SetupMarker> setupMarkers) {
    public static final Set<String> RETIRED_POINT_FIELDS=Set.of("farmSpawn","combatEntry","normalCoreCandidates","finalCore","finalEntryTrigger","shopPoints");
    /** 旧テスト・旧profile読込用。新規Runtime/Readinessでは使用しない。 */
    @Deprecated public static final Set<String> REQUIRED_SINGLE_POINTS=Set.of("farmSpawn","combatEntry","finalCore","finalEntryTrigger");
    public static final Set<String> RETIRED_AREA_FIELDS=Set.of("gateRegions","checkpoints");
    public static final Map<SetupMarkerType,Integer> REQUIRED_MARKERS=Map.of(
        SetupMarkerType.FARM_RESPAWN,1,SetupMarkerType.GAME_START,1,SetupMarkerType.NORMAL_CORE,3,
        SetupMarkerType.FINAL_CORE,1,SetupMarkerType.FINAL_AREA_ENTRY,1,SetupMarkerType.FINAL_AREA_DESTINATION,1,SetupMarkerType.FINAL_GATE,1);
    /** 旧fixture互換。Runtimeの必須領域はREQUIRED_RUNTIME_AREA_COUNTSを使用する。 */
    @Deprecated public static final Map<String,Integer> REQUIRED_AREA_COUNTS=Map.of("farmRegion",1,"enemyZones",1,"finalRegion",1,"gateRegions",1);
    private static final Map<String,Integer>REQUIRED_RUNTIME_AREA_COUNTS=Map.of("farmRegion",1,"enemyZones",1,"finalRegion",1);
    public MapProfile {
        Objects.requireNonNull(mapId); if(displayName==null||displayName.isBlank())throw new IllegalArgumentException("表示名が必要です。");
        if(templateDirectory==null||templateDirectory.isBlank())throw new IllegalArgumentException("原本識別子が必要です。");
        points=immutable(points);areas=immutable(areas);Objects.requireNonNull(createdAt);spawnMarkers=List.copyOf(spawnMarkers==null?List.of():spawnMarkers);setupMarkers=migrateLegacy(points,areas,setupMarkers==null?List.of():setupMarkers);
        List<LogicalArea> logicalAreas=logicalAreas(areas);Set<UUID> markerIds=new HashSet<>();for(SpawnMarker marker:spawnMarkers){if(!markerIds.add(marker.markerId()))throw new IllegalArgumentException("markerIdが重複しています。");LogicalArea area=logicalAreas.stream().filter(value->value.areaId().equals(marker.areaId())).findFirst().orElseThrow(()->new IllegalArgumentException("Spawn MarkerのAreaが存在しません。"));if(!contains(area.region(),marker.position()))throw new IllegalArgumentException("Spawn Markerが所属Areaの範囲外です。");}
        Set<UUID> setupMarkerIds=new HashSet<>();for(SetupMarker marker:setupMarkers)if(!setupMarkerIds.add(marker.markerId())||markerIds.contains(marker.markerId()))throw new IllegalArgumentException("markerIdが重複しています。");if(enabled&&!missing(areas,setupMarkers).isEmpty())throw new IllegalArgumentException("必須設定が不足しているため有効化できません。");
    }
    public MapProfile(MapProfileId mapId,String displayName,boolean enabled,String templateDirectory,Map<String,List<BlockPoint>> points,Map<String,List<Cuboid>> areas,Instant createdAt){this(mapId,displayName,enabled,templateDirectory,points,areas,createdAt,List.of(),List.of());}
    public MapProfile(MapProfileId mapId,String displayName,boolean enabled,String templateDirectory,Map<String,List<BlockPoint>> points,Map<String,List<Cuboid>> areas,Instant createdAt,List<SpawnMarker> markers){this(mapId,displayName,enabled,templateDirectory,points,areas,createdAt,markers,List.of());}
    private static <T> Map<String,List<T>> immutable(Map<String,List<T>> source){Map<String,List<T>> copy=new LinkedHashMap<>();source.forEach((k,v)->copy.put(k,List.copyOf(v)));return Collections.unmodifiableMap(copy);}
    public List<String> missingRequirements(){return missing(areas,setupMarkers);}private static List<String> missing(Map<String,List<Cuboid>> areas,List<SetupMarker>markers){List<String> missing=new ArrayList<>();REQUIRED_RUNTIME_AREA_COUNTS.forEach((k,n)->{if(areas.getOrDefault(k,List.of()).size()<n)missing.add(k);});REQUIRED_MARKERS.forEach((type,n)->{long count=markers.stream().filter(SetupMarker::enabled).filter(marker->marker.markerType()==type).count();if(type==SetupMarkerType.FINAL_AREA_DESTINATION?count!=n:count<n)missing.add("marker:"+type.name());});return List.copyOf(missing);}
    public boolean setupComplete(){return missingRequirements().isEmpty();}
    public MapProfile withEnabled(boolean value){return new MapProfile(mapId,displayName,value,templateDirectory,points,areas,createdAt,spawnMarkers,setupMarkers);}
    public MapProfile withDraft(Map<String,List<BlockPoint>> newPoints,Map<String,List<Cuboid>> newAreas){return withDraft(newPoints,newAreas,spawnMarkers);}
    public MapProfile withDraft(Map<String,List<BlockPoint>> newPoints,Map<String,List<Cuboid>> newAreas,List<SpawnMarker> markers){return withDraft(newPoints,newAreas,markers,setupMarkers);}
    public MapProfile withDraft(Map<String,List<BlockPoint>> newPoints,Map<String,List<Cuboid>> newAreas,List<SpawnMarker> markers,List<SetupMarker> setup){return new MapProfile(mapId,displayName,false,templateDirectory,newPoints,newAreas,createdAt,markers,setup);}
    public List<LogicalArea> logicalAreas(){return logicalAreas(areas);}
    public List<SetupMarker> enabledSetupMarkers(){return setupMarkers.stream().filter(SetupMarker::enabled).toList();}
    public List<SetupMarker> enabledSetupMarkers(SetupMarkerType type){return setupMarkers.stream().filter(SetupMarker::enabled).filter(marker->marker.markerType()==type).toList();}
    public BlockPoint requireSingleEnabledMarker(SetupMarkerType type){List<SetupMarker>values=enabledSetupMarkers(type);if(values.size()!=1)throw new IllegalArgumentException(type.label()+"が1件だけ有効になっていません。");return values.getFirst().position();}
    private static List<LogicalArea> logicalAreas(Map<String,List<Cuboid>> values){List<LogicalArea> result=new ArrayList<>();values.forEach((field,regions)->{if(RETIRED_AREA_FIELDS.contains(field))return;MapFieldCatalog.Field definition;try{definition=MapFieldCatalog.require(field);}catch(IllegalArgumentException ignored){return;}for(int i=0;i<regions.size();i++)result.add(new LogicalArea(field+":"+i,field,definition.label(),i,regions.get(i)));});return List.copyOf(result);}
    private static boolean contains(Cuboid area,BlockPoint point){return point.x()>=area.minX()&&point.x()<=area.maxX()&&point.y()>=area.minY()&&point.y()<=area.maxY()&&point.z()>=area.minZ()&&point.z()<=area.maxZ();}
    private static List<SetupMarker>migrateLegacy(Map<String,List<BlockPoint>>points,Map<String,List<Cuboid>>areas,List<SetupMarker>source){List<SetupMarker>result=new ArrayList<>(source);legacy(points,"farmSpawn",SetupMarkerType.FARM_RESPAWN,result);legacy(points,"combatEntry",SetupMarkerType.GAME_START,result);legacy(points,"normalCoreCandidates",SetupMarkerType.NORMAL_CORE,result);legacy(points,"finalCore",SetupMarkerType.FINAL_CORE,result);legacy(points,"finalEntryTrigger",SetupMarkerType.FINAL_AREA_ENTRY,result);for(Cuboid gate:areas.getOrDefault("gateRegions",List.of())){if(blockCount(gate)>100_000L)continue;for(int x=gate.minX();x<=gate.maxX();x++)for(int y=gate.minY();y<=gate.maxY();y++)for(int z=gate.minZ();z<=gate.maxZ();z++)addLegacy(result,SetupMarkerType.FINAL_GATE,new BlockPoint(x,y,z,0,0));}return List.copyOf(result);}
    private static void legacy(Map<String,List<BlockPoint>>points,String key,SetupMarkerType type,List<SetupMarker>result){if(result.stream().anyMatch(marker->marker.markerType()==type))return;List<BlockPoint>values=points.getOrDefault(key,List.of());for(int i=0;i<values.size();i++){BlockPoint point=values.get(i);result.add(new SetupMarker(UUID.nameUUIDFromBytes(("legacy|"+type+"|"+i+"|"+point.x()+","+point.y()+","+point.z()).getBytes(java.nio.charset.StandardCharsets.UTF_8)),type,point,true));}}
    private static void addLegacy(List<SetupMarker>result,SetupMarkerType type,BlockPoint point){if(result.stream().noneMatch(marker->marker.markerType()==type&&sameBlock(marker.position(),point)))result.add(new SetupMarker(UUID.nameUUIDFromBytes(("legacy|"+type+"|"+point.x()+","+point.y()+","+point.z()).getBytes(java.nio.charset.StandardCharsets.UTF_8)),type,point,true));}
    private static boolean sameBlock(BlockPoint a,BlockPoint b){return a.x()==b.x()&&a.y()==b.y()&&a.z()==b.z();}
    private static long blockCount(Cuboid value){return ((long)value.maxX()-value.minX()+1L)*((long)value.maxY()-value.minY()+1L)*((long)value.maxZ()-value.minZ()+1L);}
}
