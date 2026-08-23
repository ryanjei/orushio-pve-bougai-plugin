package com.ryanjei.orushio.pve.pve;

import com.ryanjei.orushio.pve.map.Cuboid;
import com.ryanjei.orushio.pve.persistence.RepositoryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class YamlPveSettingsRepositoryTest {
    @TempDir Path temp;
    @Test void schema1をAtomic保存しbackup付きで再読込する(){var repository=new YamlPveSettingsRepository(temp);PveSettings value=settings();repository.save("map-a",value);assertEquals(value,repository.load("map-a"));repository.save("map-a",value);assertTrue(Files.exists(temp.resolve("map-a/pve-settings.yml.bak")));}
    @Test void 未設定はemptyでschema1のemptyも読める()throws Exception{var repository=new YamlPveSettingsRepository(temp);assertEquals(PveSettings.empty(),repository.load("map-a"));write("schemaVersion: \"1\"\nenemyZoneCount: \"0\"\n");assertEquals(PveSettings.empty(),repository.load("map-a"));}
    @Test void unknownSchemaと欠落とunknownKeyを拒否する()throws Exception{var repository=new YamlPveSettingsRepository(temp);write("schemaVersion: \"99\"\nenemyZoneCount: \"0\"\n");assertThrows(RepositoryException.class,()->repository.load("map-a"));write("enemyZoneCount: \"0\"\n");assertThrows(RepositoryException.class,()->repository.load("map-a"));write("schemaVersion: \"1\"\nenemyZoneCount: \"0\"\nfuture: \"x\"\n");assertThrows(RepositoryException.class,()->repository.load("map-a"));}
    @Test void stableIdとRegionの重複を拒否する(){UUID id=UUID.randomUUID();Cuboid a=new Cuboid(20,60,20,25,70,25);var zone=zone(id,a,Map.of(EnemyType.ZOMBIE,1));assertThrows(IllegalArgumentException.class,()->new PveSettings(List.of(zone,zone(UUID.randomUUID(),a,Map.of(EnemyType.SKELETON,1)))));assertThrows(IllegalArgumentException.class,()->new PveSettings(List.of(zone,zone(id,new Cuboid(30,60,30,35,70,35),Map.of(EnemyType.CREEPER,1)))));}
    @Test void interval_spawn数_weightの異常値を拒否する(){Cuboid a=new Cuboid(20,60,20,25,70,25);assertThrows(IllegalArgumentException.class,()->new PveSettings.EnemyZone(UUID.randomUUID(),a,Duration.ZERO,1,Map.of(EnemyType.ZOMBIE,1),2,20));assertThrows(IllegalArgumentException.class,()->new PveSettings.EnemyZone(UUID.randomUUID(),a,Duration.ofSeconds(1),0,Map.of(EnemyType.ZOMBIE,1),2,20));assertThrows(IllegalArgumentException.class,()->zone(UUID.randomUUID(),a,Map.of(EnemyType.ZOMBIE,-1)));assertThrows(IllegalArgumentException.class,()->zone(UUID.randomUUID(),a,Map.of()));}
    private PveSettings settings(){return new PveSettings(List.of(zone(UUID.randomUUID(),new Cuboid(20,60,20,25,70,25),Map.of(EnemyType.ZOMBIE,70,EnemyType.SKELETON,20,EnemyType.CREEPER,10))));}
    private PveSettings.EnemyZone zone(UUID id,Cuboid region,Map<EnemyType,Integer>weights){return new PveSettings.EnemyZone(id,region,Duration.ofSeconds(5),2,weights,2,30);}
    private Path path(){return temp.resolve("map-a/pve-settings.yml");}private void write(String value)throws Exception{Files.createDirectories(path().getParent());Files.writeString(path(),value);}
}
