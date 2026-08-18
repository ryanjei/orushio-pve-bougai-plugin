package com.ryanjei.orushio.pve.map;

import com.ryanjei.orushio.pve.persistence.AtomicYamlStore;
import com.ryanjei.orushio.pve.persistence.RepositoryException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class MapSetupEvidenceRepository {
    public record Evidence(String sessionId,String mapId,UUID administrator,String worldName,String ownershipId) { }
    private final AtomicYamlStore store;
    public MapSetupEvidenceRepository(Path path){store=new AtomicYamlStore(path);}
    public Optional<Evidence> load(){return store.read().flatMap(value->{if(!Boolean.parseBoolean(value.getOrDefault("active","false")))return Optional.empty();try{return Optional.of(new Evidence(required(value,"sessionId"),required(value,"mapId"),UUID.fromString(required(value,"administrator")),required(value,"worldName"),required(value,"ownershipId")));}catch(RuntimeException failure){throw new RepositoryException("Map Setup証跡が不正です。",failure);}});}
    public void save(Evidence evidence){Map<String,String> value=new LinkedHashMap<>();value.put("schemaVersion","1");value.put("active","true");value.put("sessionId",evidence.sessionId());value.put("mapId",evidence.mapId());value.put("administrator",evidence.administrator().toString());value.put("worldName",evidence.worldName());value.put("ownershipId",evidence.ownershipId());store.write(value);}
    public void markInactive(){store.write(Map.of("schemaVersion","1","active","false"));}
    private static String required(Map<String,String> value,String key){String result=value.get(key);if(result==null||result.isBlank())throw new IllegalArgumentException(key+"がありません。");return result;}
}
