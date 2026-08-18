package com.ryanjei.orushio.pve.map;

import com.ryanjei.orushio.pve.domain.MapProfileId;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class TemporaryWorldManager {
    public record OwnedWorld(String worldName, MapProfileId mapId, String purpose, String ownershipId, String sessionId, Path directory) {public OwnedWorld(String worldName,MapProfileId mapId,String purpose,String ownershipId,Path directory){this(worldName,mapId,purpose,ownershipId,"",directory);}}
    private static final String MARKER = ".orushio-world-owner";
    private final Path mapsRoot, worldContainer;
    private boolean startupRecoveryPrepared;
    private boolean startupRecoveryComplete = true;
    private MapIoException startupRecoveryFailure;
    private OwnedWorld recoveryTarget;
    private String protectedRunSessionId = "";
    private boolean protectAllRunWorlds;
    private boolean protectAllSetupWorlds;
    public TemporaryWorldManager(Path mapsRoot, Path worldContainer) { this.mapsRoot=mapsRoot.toAbsolutePath().normalize(); this.worldContainer=worldContainer.toAbsolutePath().normalize(); }

    public synchronized OwnedWorld create(MapProfile profile, String purpose) {return create(profile,purpose,"");}
    public synchronized OwnedWorld create(MapProfile profile, String purpose,String sessionId) {
        if (startupRecoveryFailure != null) throw new MapIoException("WORLD_RECOVERY", "起動時の一時ワールド回収に失敗したため新規作成できません。", startupRecoveryFailure);
        if (!startupRecoveryComplete) throw new MapIoException("WORLD_RECOVERY_IN_PROGRESS", "起動時の一時ワールド回収中です。しばらく待ってください。");
        if(!Set.of("run","setup").contains(purpose)) throw new IllegalArgumentException("一時ワールド用途が不正です。");
        String ownership=UUID.randomUUID().toString(), name="orushio_"+purpose+"_"+ownership.replace("-","");
        Path source=mapsRoot.resolve(profile.mapId().value()).resolve(profile.templateDirectory()).normalize(), target=worldContainer.resolve(name).normalize();
        if(!source.startsWith(mapsRoot)||!target.getParent().equals(worldContainer)) throw new MapIoException("WORLD_PATH","一時ワールドパスが不正です。");
        try {
            if(!Files.isRegularFile(source.resolve("level.dat"))) throw new MapIoException("TEMPLATE_INVALID","原本ワールドが不正です。");
            SafeFiles.copyTree(source,target);
            Files.writeString(target.resolve(MARKER),"mapId="+profile.mapId().value()+"\npurpose="+purpose+"\nownershipId="+ownership+"\nsessionId="+(sessionId==null?"":sessionId)+"\n",StandardOpenOption.CREATE_NEW);
            return new OwnedWorld(name,profile.mapId(),purpose,ownership,sessionId==null?"":sessionId,target);
        } catch(Exception e) {
            try { if(Files.exists(target)) SafeFiles.deleteTree(target); } catch(IOException ignored) {}
            if(e instanceof MapIoException mapError) throw mapError;
            throw new MapIoException("WORLD_COPY","一時ワールドを作成できません。",e);
        }
    }

    public synchronized void delete(OwnedWorld world) {
        Path target=worldContainer.resolve(world.worldName()).normalize();
        if(!target.equals(world.directory().toAbsolutePath().normalize())||!target.getParent().equals(worldContainer)) throw ownershipFailure("削除対象が不正です。");
        OwnedWorld actual=readOwned(target).orElseThrow(()->ownershipFailure("所有情報を確認できません。"));
        if(!actual.mapId().equals(world.mapId())||!actual.purpose().equals(world.purpose())||!actual.ownershipId().equals(world.ownershipId())||!actual.sessionId().equals(world.sessionId())) throw ownershipFailure("所有情報が一致しません。");
        try { SafeFiles.deleteTree(target); } catch(IOException e) { throw new MapIoException("WORLD_DELETE","一時ワールドを削除できません。",e); }
    }

    public synchronized boolean hasOwnedWorld(MapProfileId mapId) { return ownedWorlds().stream().anyMatch(world->world.mapId().equals(mapId)); }
    public synchronized void prepareStartupRecovery() {
        prepareStartupRecovery("");
    }
    public synchronized void prepareStartupRecovery(String protectedRunSessionId) {
        prepareStartupRecovery(protectedRunSessionId,false);
    }
    public synchronized void prepareStartupRecovery(String protectedRunSessionId,boolean protectAllRunWorlds) {
        prepareStartupRecovery(protectedRunSessionId,protectAllRunWorlds,false);
    }
    public synchronized void prepareStartupRecovery(String protectedRunSessionId,boolean protectAllRunWorlds,boolean protectAllSetupWorlds) {
        if (startupRecoveryPrepared) throw new IllegalStateException("起動時回収はすでに準備されています。");
        startupRecoveryPrepared = true;
        startupRecoveryComplete = false;
        this.protectedRunSessionId=protectedRunSessionId==null?"":protectedRunSessionId;
        this.protectAllRunWorlds=protectAllRunWorlds;
        this.protectAllSetupWorlds=protectAllSetupWorlds;
    }

    public List<Path> recoverPreparedWorlds() { return recoverPreparedWorlds(() -> {}); }

    List<Path> recoverPreparedWorlds(Runnable afterSnapshot) {
        try {
            List<OwnedWorld> stale;
            synchronized (this) {
                if (!startupRecoveryPrepared || startupRecoveryComplete) throw new IllegalStateException("起動時回収が準備されていません。");
                stale = ownedWorlds().stream().filter(world->{
                    if(world.purpose().equals("setup"))return !protectAllSetupWorlds;
                    return !protectAllRunWorlds&&!world.sessionId().equals(protectedRunSessionId);
                }).toList();
            }
            afterSnapshot.run();
            List<Path> removed = new ArrayList<>();
            for (OwnedWorld world : stale) { delete(world); removed.add(world.directory()); }
            synchronized (this) { startupRecoveryComplete = true; }
            return List.copyOf(removed);
        } catch (Exception exception) {
            MapIoException failure = exception instanceof MapIoException mapError ? mapError : new MapIoException("WORLD_RECOVERY", "起動時の一時ワールド回収に失敗しました。", exception);
            synchronized (this) { startupRecoveryFailure = failure; startupRecoveryComplete = true; }
            throw failure;
        }
    }

    public synchronized List<Path> recoverOwnedWorlds() { List<Path> removed=new ArrayList<>(); for(OwnedWorld world:ownedWorlds()){delete(world);removed.add(world.directory());} return List.copyOf(removed); }
    public synchronized boolean startupRecoveryComplete() { return startupRecoveryComplete; }
    public synchronized void requireRecovery(OwnedWorld world,String reason) { recoveryTarget=Objects.requireNonNull(world); startupRecoveryFailure=new MapIoException("WORLD_RECOVERY",reason); }
    public synchronized void requireRecovery(String reason) { recoveryTarget=null; startupRecoveryFailure=new MapIoException("WORLD_RECOVERY",reason); }
    public synchronized boolean recoveryRequired() { return startupRecoveryFailure!=null; }
    public synchronized Optional<OwnedWorld> recoveryTarget() { return Optional.ofNullable(recoveryTarget); }
    public synchronized Optional<OwnedWorld> findRunWorld(String sessionId,MapProfileId mapId) {
        List<OwnedWorld> sameSession=ownedWorlds().stream().filter(world->world.purpose().equals("run")&&world.sessionId().equals(sessionId)).toList();
        if(sameSession.stream().anyMatch(world->!world.mapId().equals(mapId)))throw ownershipFailure("ゲームセッションとマップの所有情報が一致しません。");
        List<OwnedWorld> matches=sameSession.stream().filter(world->world.mapId().equals(mapId)).toList();
        if(matches.size()>1)throw ownershipFailure("同じゲームセッションの一時ワールドが複数存在します。");
        return matches.stream().findFirst();
    }
    public synchronized List<OwnedWorld> setupWorlds() { return ownedWorlds().stream().filter(world->world.purpose().equals("setup")).toList(); }
    public synchronized Optional<String> startupRecoveryWarning() { if(startupRecoveryFailure==null)return Optional.empty();if(recoveryTarget!=null)return Optional.of("一時ワールド「"+recoveryTarget.worldName()+"」を安全に回収できません。再起動または手動確認が必要です。");return Optional.of("起動時の一時ワールド回収に失敗しました。手動確認が必要です。"); }

    private List<OwnedWorld> ownedWorlds() {
        if(!Files.isDirectory(worldContainer)) return List.of();
        try(var dirs=Files.list(worldContainer)) {
            return dirs.filter(Files::isDirectory).filter(dir->{String n=dir.getFileName().toString();return n.startsWith("orushio_run_")||n.startsWith("orushio_setup_");}).map(this::readOwnedForRecovery).flatMap(Optional::stream).toList();
        } catch(IOException e) { throw new MapIoException("WORLD_RECOVERY","一時ワールドを確認できません。",e); }
    }

    private Optional<OwnedWorld> readOwned(Path dir) {
        try {
            Path marker=dir.resolve(MARKER); if(!Files.isRegularFile(marker,LinkOption.NOFOLLOW_LINKS)||Files.isSymbolicLink(marker)) return Optional.empty();
            Map<String,String> values=new HashMap<>(); for(String line:Files.readAllLines(marker)){int i=line.indexOf('=');if(i>0)values.put(line.substring(0,i),line.substring(i+1));}
            String purpose=values.get("purpose"),ownership=values.get("ownershipId"),mapId=values.get("mapId");
            if(!Set.of("run","setup").contains(purpose)||ownership==null||mapId==null) return Optional.empty();
            return Optional.of(new OwnedWorld(dir.getFileName().toString(),new MapProfileId(mapId),purpose,ownership,values.getOrDefault("sessionId",""),dir));
        } catch(Exception e) { return Optional.empty(); }
    }

    private Optional<OwnedWorld> readOwnedForRecovery(Path dir){Optional<OwnedWorld> world=readOwned(dir);Path marker=dir.resolve(MARKER);if(world.isEmpty()&&Files.exists(marker,LinkOption.NOFOLLOW_LINKS))throw ownershipFailure("一時ワールドの所有情報を確認できません。");return world;}
    private MapIoException ownershipFailure(String message){MapIoException failure=new MapIoException("WORLD_OWNERSHIP",message);startupRecoveryFailure=failure;return failure;}
}
