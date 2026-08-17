package com.ryanjei.orushio.pve.map;
import java.util.*;
public record RuntimeMap(MapProfile profile,List<LogicalArea> areas,List<SpawnMarker> enabledMarkers){
 public RuntimeMap{areas=List.copyOf(areas);enabledMarkers=List.copyOf(enabledMarkers);for(SpawnMarker m:enabledMarkers){LogicalArea area=areas.stream().filter(a->a.areaId().equals(m.areaId())).findFirst().orElseThrow(()->new IllegalArgumentException("スポーン地点の所属Areaがありません。"));if(!contains(area.region(),m.position()))throw new IllegalArgumentException("スポーン地点が所属Areaの範囲外です。");}}
 public static RuntimeMap resolve(MapProfile profile){return new RuntimeMap(profile,profile.logicalAreas(),profile.spawnMarkers().stream().filter(SpawnMarker::enabled).toList());}
 public List<SpawnMarker> enabledMarkers(String areaId){return enabledMarkers.stream().filter(marker->marker.areaId().equals(areaId)).toList();}
 private static boolean contains(Cuboid a,BlockPoint p){return p.x()>=a.minX()&&p.x()<=a.maxX()&&p.y()>=a.minY()&&p.y()<=a.maxY()&&p.z()>=a.minZ()&&p.z()<=a.maxZ();}
}
