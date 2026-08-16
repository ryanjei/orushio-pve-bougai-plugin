package com.ryanjei.orushio.pve.map;

import com.ryanjei.orushio.pve.domain.MapProfileId;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class TemporaryWorldManager {
    public record OwnedWorld(String worldName, MapProfileId mapId, String purpose, String ownershipId, Path directory) {}
    private static final String MARKER = ".orushio-world-owner";
    private final Path mapsRoot, worldContainer;
    public TemporaryWorldManager(Path mapsRoot, Path worldContainer) { this.mapsRoot=mapsRoot.toAbsolutePath().normalize(); this.worldContainer=worldContainer.toAbsolutePath().normalize(); }

    public synchronized OwnedWorld create(MapProfile profile, String purpose) {
        if(!Set.of("run","setup").contains(purpose)) throw new IllegalArgumentException("一時ワールド用途が不正です。");
        String ownership=UUID.randomUUID().toString(), name="orushio_"+purpose+"_"+ownership.replace("-","");
        Path source=mapsRoot.resolve(profile.mapId().value()).resolve(profile.templateDirectory()).normalize(), target=worldContainer.resolve(name).normalize();
        if(!source.startsWith(mapsRoot)||!target.getParent().equals(worldContainer)) throw new MapIoException("WORLD_PATH","一時ワールドパスが不正です。");
        try {
            if(!Files.isRegularFile(source.resolve("level.dat"))) throw new MapIoException("TEMPLATE_INVALID","原本ワールドが不正です。");
            SafeFiles.copyTree(source,target);
            Files.writeString(target.resolve(MARKER),"mapId="+profile.mapId().value()+"\npurpose="+purpose+"\nownershipId="+ownership+"\n",StandardOpenOption.CREATE_NEW);
            return new OwnedWorld(name,profile.mapId(),purpose,ownership,target);
        } catch(Exception e) {
            try { if(Files.exists(target)) SafeFiles.deleteTree(target); } catch(IOException ignored) {}
            if(e instanceof MapIoException mapError) throw mapError;
            throw new MapIoException("WORLD_COPY","一時ワールドを作成できません。",e);
        }
    }

    public synchronized void delete(OwnedWorld world) {
        Path target=worldContainer.resolve(world.worldName()).normalize();
        if(!target.equals(world.directory().toAbsolutePath().normalize())||!target.getParent().equals(worldContainer)) throw new MapIoException("WORLD_OWNERSHIP","削除対象が不正です。");
        OwnedWorld actual=readOwned(target).orElseThrow(()->new MapIoException("WORLD_OWNERSHIP","所有情報を確認できません。"));
        if(!actual.mapId().equals(world.mapId())||!actual.purpose().equals(world.purpose())||!actual.ownershipId().equals(world.ownershipId())) throw new MapIoException("WORLD_OWNERSHIP","所有情報が一致しません。");
        try { SafeFiles.deleteTree(target); } catch(IOException e) { throw new MapIoException("WORLD_DELETE","一時ワールドを削除できません。",e); }
    }

    public synchronized boolean hasOwnedWorld(MapProfileId mapId) { return ownedWorlds().stream().anyMatch(world->world.mapId().equals(mapId)); }
    public synchronized List<Path> recoverOwnedWorlds() { List<Path> removed=new ArrayList<>(); for(OwnedWorld world:ownedWorlds()){delete(world);removed.add(world.directory());} return List.copyOf(removed); }

    private List<OwnedWorld> ownedWorlds() {
        if(!Files.isDirectory(worldContainer)) return List.of();
        try(var dirs=Files.list(worldContainer)) {
            return dirs.filter(Files::isDirectory).filter(dir->{String n=dir.getFileName().toString();return n.startsWith("orushio_run_")||n.startsWith("orushio_setup_");}).map(this::readOwned).flatMap(Optional::stream).toList();
        } catch(IOException e) { throw new MapIoException("WORLD_RECOVERY","一時ワールドを確認できません。",e); }
    }

    private Optional<OwnedWorld> readOwned(Path dir) {
        try {
            Path marker=dir.resolve(MARKER); if(!Files.isRegularFile(marker,LinkOption.NOFOLLOW_LINKS)||Files.isSymbolicLink(marker)) return Optional.empty();
            Map<String,String> values=new HashMap<>(); for(String line:Files.readAllLines(marker)){int i=line.indexOf('=');if(i>0)values.put(line.substring(0,i),line.substring(i+1));}
            String purpose=values.get("purpose"),ownership=values.get("ownershipId"),mapId=values.get("mapId");
            if(!Set.of("run","setup").contains(purpose)||ownership==null||mapId==null) return Optional.empty();
            return Optional.of(new OwnedWorld(dir.getFileName().toString(),new MapProfileId(mapId),purpose,ownership,dir));
        } catch(Exception e) { return Optional.empty(); }
    }
}
