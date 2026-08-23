package com.ryanjei.orushio.pve.core;
import com.ryanjei.orushio.pve.persistence.*;import java.nio.file.Path;import java.util.*;
public final class YamlCoreSettingsRepository implements CoreSettingsRepository{
 private final Path mapsRoot;public YamlCoreSettingsRepository(Path mapsRoot){this.mapsRoot=mapsRoot;}
 public CoreSettings load(String mapId){return new AtomicYamlStore(path(mapId)).read().map(this::decode).orElseThrow(()->new RepositoryException("core-settings.ymlがありません。"));}
 public void save(String mapId,CoreSettings settings){new AtomicYamlStore(path(mapId)).write(Map.of("schemaVersion","1","normalCoreHp",Double.toString(settings.normalCoreHp()),"finalCoreHp",Double.toString(settings.finalCoreHp())));}
 private Path path(String mapId){if(mapId==null||!mapId.matches("[a-z0-9][a-z0-9_-]{0,63}"))throw new IllegalArgumentException("mapIdが不正です。");Path value=mapsRoot.resolve(mapId).resolve("core-settings.yml").normalize();if(!value.startsWith(mapsRoot.normalize()))throw new IllegalArgumentException("設定Pathが不正です。");return value;}
 private CoreSettings decode(Map<String,String>v){if(!v.keySet().equals(Set.of("schemaVersion","normalCoreHp","finalCoreHp")))throw new RepositoryException("core-settingsに不足または未知の項目があります。");if(!"1".equals(v.get("schemaVersion")))throw new RepositoryException("未対応のcore-settings schemaVersionです。");try{return new CoreSettings(Double.parseDouble(v.get("normalCoreHp")),Double.parseDouble(v.get("finalCoreHp")));}catch(IllegalArgumentException e){throw new RepositoryException("core-settingsの値が不正です。",e);}}
}
