package com.ryanjei.orushio.pve.pve;

import com.ryanjei.orushio.pve.domain.*;
import com.ryanjei.orushio.pve.map.*;
import com.ryanjei.orushio.pve.persistence.GameLaunchSettingsRepository;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PveSettingsAdministrationServiceTest {
    private final UUID normalId=UUID.randomUUID(),finalId=UUID.randomUUID();
    private final Cuboid farm=new Cuboid(0,60,0,10,70,10),normal=new Cuboid(20,60,20,25,70,25),finalRegion=new Cuboid(30,60,30,45,75,45),finalZone=new Cuboid(32,60,32,38,70,38);
    private final MemorySettings settings=new MemorySettings();
    private final MutableGames games=new MutableGames();
    private PveSettingsAdministrationService service;

    @BeforeEach void setUp(){settings.value=new PveSettings(List.of(zone(normalId,normal,2,70,20,10),zone(finalId,finalZone,1,1,0,0)));service=new PveSettingsAdministrationService(settings,new Profiles(profile()),new Launches(1.0),games::session);}

    @Test void loadはstableId座標と通常_Final区分を返す(){var view=service.load("map-a");assertTrue(view.editable());assertEquals(2,view.enemyZones().size());assertFalse(view.enemyZones().getFirst().finalArea());assertTrue(view.enemyZones().getLast().finalArea());assertEquals(normalId,view.enemyZones().getFirst().enemyZoneId());}
    @Test void validなZone更新は全体検証後に保存再読込できる(){var before=service.load("map-a");var saved=service.update("map-a",update(before.revision(),normalId,3,2,60,30,10,3,28));assertEquals(1,settings.saves);assertNotEquals(before.revision(),saved.revision());assertEquals(3,saved.enemyZones().getFirst().spawnIntervalSeconds());assertEquals(2,saved.enemyZones().size());}
    @Test void invalid値_未知Zone_staleRevisionでは元設定を維持する(){PveSettings original=settings.value;String revision=service.load("map-a").revision();assertThrows(IllegalArgumentException.class,()->service.update("map-a",update(revision,normalId,2,1,0,0,0,2,30)));assertThrows(IllegalArgumentException.class,()->service.update("map-a",update(revision,normalId,2,1,1,0,0,31,30)));assertThrows(IllegalArgumentException.class,()->service.update("map-a",update(revision,UUID.randomUUID(),2,1,1,0,0,2,30)));assertThrows(IllegalStateException.class,()->service.update("map-a",update("stale",normalId,2,1,1,0,0,2,30)));assertSame(original,settings.value);assertEquals(0,settings.saves);}
    @Test void ACTIVE中は編集不可で危険な最大人数Scalingも拒否する(){String revision=service.load("map-a").revision();games.state=GameState.ACTIVE;assertThrows(IllegalStateException.class,()->service.update("map-a",update(revision,normalId,2,1,1,0,0,2,30)));games.state=GameState.IDLE;service=new PveSettingsAdministrationService(settings,new Profiles(profile()),new Launches(100),games::session);assertThrows(IllegalArgumentException.class,()->service.update("map-a",update(revision,normalId,2,64,1,0,0,2,30)));assertEquals(0,settings.saves);}

    private PveSettingsAdministrationService.Update update(String revision,UUID id,long interval,int count,int zombie,int skeleton,int creeper,double min,double max){return new PveSettingsAdministrationService.Update(revision,id,interval,count,zombie,skeleton,creeper,min,max);}
    private PveSettings.EnemyZone zone(UUID id,Cuboid region,int count,int zombie,int skeleton,int creeper){return new PveSettings.EnemyZone(id,region,Duration.ofSeconds(2),count,Map.of(EnemyType.ZOMBIE,zombie,EnemyType.SKELETON,skeleton,EnemyType.CREEPER,creeper),2,30);}
    private MapProfile profile(){return new MapProfile(new MapProfileId("map-a"),"A",false,"template",Map.of("farmSpawn",List.of(new BlockPoint(1,64,1,0,0))),Map.of("farmRegion",List.of(farm),"enemyZones",List.of(normal,finalZone),"finalRegion",List.of(finalRegion)),Instant.now());}
    private static final class MemorySettings implements PveSettingsRepository{PveSettings value;int saves;public PveSettings load(String id){return value;}public void save(String id,PveSettings value){this.value=value;saves++;}}
    private record Profiles(MapProfile value)implements MapProfileRepository{public List<MapProfile>findAll(){return List.of(value);}public Optional<MapProfile>find(MapProfileId id){return value.mapId().equals(id)?Optional.of(value):Optional.empty();}public void save(MapProfile ignored){}public void delete(MapProfileId ignored){}}
    private record Launches(double multiplier)implements GameLaunchSettingsRepository{public GameLaunchSettings load(String id){return new GameLaunchSettings(Optional.empty(),Optional.empty(),Optional.of(multiplier));}public void save(String id,GameLaunchSettings ignored){}}
    private static final class MutableGames{GameState state=GameState.IDLE;GameSession session(){return new GameSession(UUID.randomUUID(),state,List.of(),Instant.now(),null,0,60,2,1,null,null,Set.of());}}
}
