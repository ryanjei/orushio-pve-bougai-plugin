package com.ryanjei.orushio.pve.paper;

import com.ryanjei.orushio.pve.persistence.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

final class SetupAdministratorOwnership {
    private static final Pattern UUID_FIELD=Pattern.compile("\\\"uuid\\\"\\s*:\\s*\\\"([0-9a-fA-F-]{36})\\\"");
    private final AtomicYamlStore store;private final Set<UUID> owned=new LinkedHashSet<>();
    SetupAdministratorOwnership(Path dataDirectory,Path serverRoot){store=new AtomicYamlStore(dataDirectory.resolve("setup-administrators.yml"));Optional<Map<String,String>> existing=store.read();if(existing.isPresent())decode(existing.get().getOrDefault("owned",""));else{migrateLegacy(dataDirectory.resolve("logs"),serverRoot.resolve("ops.json"));save();}}
    synchronized boolean contains(UUID id){return owned.contains(id);}
    synchronized void add(UUID id){if(owned.add(id))save();}
    synchronized void remove(UUID id){if(owned.remove(id))save();}
    private void decode(String value){for(String part:value.split(","))if(!part.isBlank())owned.add(UUID.fromString(part.trim()));}
    private void save(){store.write(Map.of("schemaVersion","1","owned",owned.stream().map(UUID::toString).sorted().reduce((a,b)->a+","+b).orElse("")));}
    private void migrateLegacy(Path logs,Path ops){if(legacyGrantCount(logs)!=1)return;Set<UUID> operators=readOperators(ops);if(operators.size()!=1)return;owned.add(operators.iterator().next());}
    static long legacyGrantCount(Path logs){if(!Files.isDirectory(logs))return 0;long count=0;try(var files=Files.list(logs)){for(Path path:files.filter(file->file.getFileName().toString().startsWith("audit-")).toList())for(String line:Files.readAllLines(path,StandardCharsets.UTF_8))if(line.contains("\"code\":\"SETUP_ADMINISTRATOR_GRANTED\""))count++;return count;}catch(IOException e){return 0;}}
    static Set<UUID> readOperators(Path ops){if(!Files.isRegularFile(ops))return Set.of();try{var matcher=UUID_FIELD.matcher(Files.readString(ops,StandardCharsets.UTF_8));Set<UUID> result=new LinkedHashSet<>();while(matcher.find())result.add(UUID.fromString(matcher.group(1)));return result;}catch(Exception e){return Set.of();}}
}
