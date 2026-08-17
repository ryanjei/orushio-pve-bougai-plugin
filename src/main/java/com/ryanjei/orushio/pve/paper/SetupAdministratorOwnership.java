package com.ryanjei.orushio.pve.paper;

import com.ryanjei.orushio.pve.persistence.*;
import java.nio.file.*;
import java.util.*;

final class SetupAdministratorOwnership {
    private static final String EVIDENCE_VERSION="explicit-v2";
    private final AtomicYamlStore store;private final Set<UUID> owned=new LinkedHashSet<>();
    SetupAdministratorOwnership(Path dataDirectory){store=new AtomicYamlStore(dataDirectory.resolve("setup-administrators.yml"));Optional<Map<String,String>> existing=store.read();if(existing.isPresent()&&EVIDENCE_VERSION.equals(existing.get().get("ownershipEvidence")))decode(existing.get().getOrDefault("owned",""));else save();}
    synchronized boolean contains(UUID id){return owned.contains(id);}
    synchronized void add(UUID id){if(owned.add(id))save();}
    synchronized void remove(UUID id){if(owned.remove(id))save();}
    private void decode(String value){for(String part:value.split(","))if(!part.isBlank())owned.add(UUID.fromString(part.trim()));}
    private void save(){store.write(Map.of("schemaVersion","1","ownershipEvidence",EVIDENCE_VERSION,"owned",owned.stream().map(UUID::toString).sorted().reduce((a,b)->a+","+b).orElse("")));}
}
