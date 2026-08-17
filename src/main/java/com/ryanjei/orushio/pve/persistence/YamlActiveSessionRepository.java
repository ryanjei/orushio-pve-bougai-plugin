package com.ryanjei.orushio.pve.persistence;

import com.ryanjei.orushio.pve.domain.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

public final class YamlActiveSessionRepository implements ActiveSessionRepository {
    private final AtomicYamlStore store;
    public YamlActiveSessionRepository(Path path){store=new AtomicYamlStore(path);}
    public Optional<GameSession> load(){return store.read().map(this::decode);}
    public void save(GameSession session){Map<String,String> values=new LinkedHashMap<>();values.put("schemaVersion","1");values.put("sessionId",session.sessionId().toString());values.put("state",session.state().name());values.put("createdAt",session.createdAt().toString());values.put("mapId",session.mapId());values.put("participants",encodeParticipants(session.participants()));values.put("participantCountAtStart",Integer.toString(session.participantCountAtStart()));values.put("timeLimitMinutes",Integer.toString(session.timeLimitMinutes()));values.put("requiredNormalCores",Integer.toString(session.requiredNormalCores()));values.put("enemyMultiplier",Double.toString(session.enemyMultiplier()));values.put("activeAt",session.activeAt().map(Instant::toString).orElse(""));values.put("endsAt",session.endsAt().map(Instant::toString).orElse(""));values.put("pendingCleanup",session.pendingCleanup().stream().map(UUID::toString).sorted().reduce((a,b)->a+","+b).orElse(""));store.write(values);}
    private GameSession decode(Map<String,String> value){try{return new GameSession(UUID.fromString(required(value,"sessionId")),GameState.valueOf(required(value,"state")),decodeParticipants(value.getOrDefault("participants","")),Instant.parse(required(value,"createdAt")),value.getOrDefault("mapId",""),integer(value,"participantCountAtStart",0,0,1000),integer(value,"timeLimitMinutes",60,1,1440),integer(value,"requiredNormalCores",2,1,1000),decimal(value,"enemyMultiplier",1.0,0.01,100),instant(value.get("activeAt")),instant(value.get("endsAt")),decodeUuids(value.getOrDefault("pendingCleanup","")));}catch(RepositoryException exception){throw exception;}catch(Exception exception){throw new RepositoryException("active-sessionの値が不正です。",exception);}}
    private static String encodeParticipants(List<Participant> values){Base64.Encoder encoder=Base64.getUrlEncoder().withoutPadding();return values.stream().map(p->p.playerUuid()+","+encoder.encodeToString(p.lastKnownName().getBytes(StandardCharsets.UTF_8))+","+p.connected()).reduce((a,b)->a+";"+b).orElse("");}
    private static List<Participant> decodeParticipants(String raw){if(raw.isBlank())return List.of();Base64.Decoder decoder=Base64.getUrlDecoder();List<Participant> values=new ArrayList<>();for(String item:raw.split(";")){String[] fields=item.split(",",-1);if(fields.length!=3||(!fields[2].equals("true")&&!fields[2].equals("false")))throw new RepositoryException("参加者データが不正です。");values.add(new Participant(UUID.fromString(fields[0]),new String(decoder.decode(fields[1]),StandardCharsets.UTF_8),Boolean.parseBoolean(fields[2])));}return List.copyOf(values);}
    private static Set<UUID> decodeUuids(String raw){if(raw.isBlank())return Set.of();Set<UUID> values=new LinkedHashSet<>();for(String item:raw.split(","))values.add(UUID.fromString(item));return Set.copyOf(values);}
    private static String required(Map<String,String> value,String key){String result=value.get(key);if(result==null||result.isBlank())throw new RepositoryException(key+"がありません。");return result;}
    private static int integer(Map<String,String> value,String key,int defaultValue,int minimum,int maximum){int result=Integer.parseInt(value.getOrDefault(key,Integer.toString(defaultValue)));if(result<minimum||result>maximum)throw new RepositoryException(key+"が範囲外です。");return result;}
    private static double decimal(Map<String,String> value,String key,double defaultValue,double minimum,double maximum){double result=Double.parseDouble(value.getOrDefault(key,Double.toString(defaultValue)));if(!Double.isFinite(result)||result<minimum||result>maximum)throw new RepositoryException(key+"が範囲外です。");return result;}
    private static Instant instant(String value){return value==null||value.isBlank()?null:Instant.parse(value);}
}
