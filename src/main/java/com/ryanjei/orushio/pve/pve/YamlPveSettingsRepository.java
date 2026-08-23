package com.ryanjei.orushio.pve.pve;

import com.ryanjei.orushio.pve.map.Cuboid;
import com.ryanjei.orushio.pve.persistence.*;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

public final class YamlPveSettingsRepository implements PveSettingsRepository {
    private final Path mapsRoot;
    public YamlPveSettingsRepository(Path mapsRoot){this.mapsRoot=mapsRoot;}
    public PveSettings load(String mapId){return new AtomicYamlStore(path(mapId)).read().map(this::decode).orElseGet(PveSettings::empty);}
    public void save(String mapId,PveSettings settings){new AtomicYamlStore(path(mapId)).write(encode(settings));}
    private Path path(String mapId){if(mapId==null||!mapId.matches("[a-z0-9][a-z0-9_-]{0,63}"))throw new IllegalArgumentException("mapIdが不正です。");Path value=mapsRoot.resolve(mapId).resolve("pve-settings.yml").normalize();if(!value.startsWith(mapsRoot.normalize()))throw new IllegalArgumentException("設定Pathが不正です。");return value;}
    private Map<String,String> encode(PveSettings settings){Map<String,String>v=new LinkedHashMap<>();v.put("schemaVersion","1");v.put("enemyZoneCount",Integer.toString(settings.enemyZones().size()));for(int i=0;i<settings.enemyZones().size();i++){var z=settings.enemyZones().get(i);String p="enemyZone."+i+".";v.put(p+"id",z.enemyZoneId().toString());v.put(p+"region",region(z.region()));v.put(p+"spawnIntervalSeconds",Long.toString(z.spawnInterval().toSeconds()));v.put(p+"baseSpawnCount",Integer.toString(z.baseSpawnCount()));v.put(p+"minParticipantDistance",Double.toString(z.minParticipantDistance()));v.put(p+"maxParticipantDistance",Double.toString(z.maxParticipantDistance()));for(EnemyType type:EnemyType.values())v.put(p+"weight."+type.name(),Integer.toString(z.mobWeights().getOrDefault(type,0)));}return v;}
    private PveSettings decode(Map<String,String>v){try{if(!"1".equals(req(v,"schemaVersion")))throw new RepositoryException("未対応のpve-settings schemaVersionです。");Set<String>used=new HashSet<>(Set.of("schemaVersion","enemyZoneCount"));int count=integer(v,"enemyZoneCount",0,1000);List<PveSettings.EnemyZone>zones=new ArrayList<>();for(int i=0;i<count;i++){String p="enemyZone."+i+".";Collections.addAll(used,p+"id",p+"region",p+"spawnIntervalSeconds",p+"baseSpawnCount",p+"minParticipantDistance",p+"maxParticipantDistance");EnumMap<EnemyType,Integer>weights=new EnumMap<>(EnemyType.class);for(EnemyType type:EnemyType.values()){String key=p+"weight."+type.name();used.add(key);weights.put(type,integer(v,key,0,1_000_000));}zones.add(new PveSettings.EnemyZone(UUID.fromString(req(v,p+"id")),region(req(v,p+"region")),Duration.ofSeconds(integer(v,p+"spawnIntervalSeconds",1,86400)),integer(v,p+"baseSpawnCount",1,64),weights,decimal(v,p+"minParticipantDistance"),decimal(v,p+"maxParticipantDistance")));}if(!used.equals(v.keySet()))throw new RepositoryException("pve-settingsに未知の項目があります。");return new PveSettings(zones);}catch(RepositoryException e){throw e;}catch(Exception e){throw new RepositoryException("pve-settingsの値が不正です。",e);}}
    private static String req(Map<String,String>v,String key){String value=v.get(key);if(value==null||value.isBlank())throw new RepositoryException(key+"がありません。");return value;}
    private static int integer(Map<String,String>v,String key,int min,int max){int value=Integer.parseInt(req(v,key));if(value<min||value>max)throw new RepositoryException(key+"が範囲外です。");return value;}
    private static double decimal(Map<String,String>v,String key){double value=Double.parseDouble(req(v,key));if(!Double.isFinite(value))throw new RepositoryException(key+"が不正です。");return value;}
    private static String region(Cuboid c){return c.minX()+","+c.minY()+","+c.minZ()+","+c.maxX()+","+c.maxY()+","+c.maxZ();}
    private static Cuboid region(String value){String[]p=value.split(",");if(p.length!=6)throw new RepositoryException("Enemy Zone範囲が不正です。");return new Cuboid(Integer.parseInt(p[0]),Integer.parseInt(p[1]),Integer.parseInt(p[2]),Integer.parseInt(p[3]),Integer.parseInt(p[4]),Integer.parseInt(p[5]));}
}
