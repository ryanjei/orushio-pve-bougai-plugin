package com.ryanjei.orushio.pve.pve;

import com.ryanjei.orushio.pve.application.ParticipantPolicy;
import com.ryanjei.orushio.pve.domain.*;
import com.ryanjei.orushio.pve.map.*;
import com.ryanjei.orushio.pve.persistence.GameLaunchSettingsRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;

public final class PveSettingsAdministrationService {
    private final PveSettingsRepository settings;
    private final MapProfileRepository profiles;
    private final GameLaunchSettingsRepository launches;
    private final Supplier<GameSession> games;

    public PveSettingsAdministrationService(PveSettingsRepository settings,MapProfileRepository profiles,GameLaunchSettingsRepository launches,Supplier<GameSession>games){this.settings=settings;this.profiles=profiles;this.launches=launches;this.games=games;}

    public synchronized View load(String mapId){
        MapProfile profile=profile(mapId);PveSettings value=settings.load(mapId);rejectExtraZones(profile,value);
        if(complete(profile,value))PveLifecycleStep.validateConfiguration(profile,value);
        return view(mapId,profile,value);
    }

    public synchronized View update(String mapId,Update update){
        GameState state=games.get().state();if(state!=GameState.IDLE&&state!=GameState.RECRUITING)throw new IllegalStateException("PvE設定は待機中だけ変更できます。");
        MapProfile profile=profile(mapId);PveSettings current=settings.load(mapId);if(!revision(current).equals(update.revision()))throw new IllegalStateException("PvE設定が別画面で更新されています。再読込してください。");
        List<PveSettings.EnemyZone>zones=new ArrayList<>(current.enemyZones());int index=-1;for(int i=0;i<zones.size();i++)if(zones.get(i).enemyZoneId().equals(update.enemyZoneId())){index=i;break;}
        Cuboid region;if(index>=0)region=zones.get(index).region();else region=profile.areas().getOrDefault("enemyZones",List.of()).stream().filter(area->zoneId(mapId,area).equals(update.enemyZoneId())).findFirst().orElseThrow(()->new IllegalArgumentException("指定されたEnemy ZoneはMap Setupにありません。"));
        EnumMap<EnemyType,Integer>weights=new EnumMap<>(EnemyType.class);weights.put(EnemyType.ZOMBIE,update.zombieWeight());weights.put(EnemyType.SKELETON,update.skeletonWeight());weights.put(EnemyType.CREEPER,update.creeperWeight());
        PveSettings.EnemyZone replacement=new PveSettings.EnemyZone(update.enemyZoneId(),region,Duration.ofSeconds(update.spawnIntervalSeconds()),update.baseSpawnCount(),weights,update.minParticipantDistance(),update.maxParticipantDistance());
        if(index>=0)zones.set(index,replacement);else zones.add(replacement);
        PveSettings candidate=new PveSettings(zones);rejectExtraZones(profile,candidate);if(complete(profile,candidate))PveLifecycleStep.validateConfiguration(profile,candidate);
        double multiplier=launches.load(mapId).resolvedEnemyMultiplier();for(var zone:candidate.enemyZones())PveLifecycleStep.spawnCount(zone.baseSpawnCount(),multiplier,ParticipantPolicy.standard().maxParticipants());
        settings.save(mapId,candidate);PveSettings saved=settings.load(mapId);if(!saved.equals(candidate))throw new IllegalStateException("PvE設定の保存結果を確認できませんでした。");return view(mapId,profile,saved);
    }

    private View view(String mapId,MapProfile profile,PveSettings value){
        Cuboid finalRegion=profile.areas().getOrDefault("finalRegion",List.of()).stream().findFirst().orElse(null);boolean editable=games.get().state()==GameState.IDLE||games.get().state()==GameState.RECRUITING;List<ZoneView>zones=new ArrayList<>();
        for(Cuboid region:profile.areas().getOrDefault("enemyZones",List.of())){
            PveSettings.EnemyZone zone=value.enemyZones().stream().filter(candidate->candidate.region().equals(region)).findFirst().orElse(null);
            zones.add(zone==null?new ZoneView(zoneId(mapId,region),region,finalRegion!=null&&contains(finalRegion,region),false,0,0,0,0,0,0,0):new ZoneView(zone.enemyZoneId(),zone.region(),finalRegion!=null&&contains(finalRegion,zone.region()),true,zone.spawnInterval().toSeconds(),zone.baseSpawnCount(),zone.mobWeights().getOrDefault(EnemyType.ZOMBIE,0),zone.mobWeights().getOrDefault(EnemyType.SKELETON,0),zone.mobWeights().getOrDefault(EnemyType.CREEPER,0),zone.minParticipantDistance(),zone.maxParticipantDistance()));
        }
        return new View(mapId,revision(value),editable,zones);
    }

    private MapProfile profile(String mapId){return profiles.find(new MapProfileId(mapId)).orElseThrow(()->new IllegalArgumentException("指定されたマップがありません。"));}
    private static boolean complete(MapProfile profile,PveSettings value){return value.enemyZones().size()==profile.areas().getOrDefault("enemyZones",List.of()).size();}
    private static void rejectExtraZones(MapProfile profile,PveSettings value){List<Cuboid>setup=profile.areas().getOrDefault("enemyZones",List.of());if(value.enemyZones().stream().anyMatch(zone->!setup.contains(zone.region())))throw new IllegalArgumentException("PvE Enemy ZoneがMap Setupと一致しません。");}
    private static boolean contains(Cuboid a,Cuboid b){return b.minX()>=a.minX()&&b.maxX()<=a.maxX()&&b.minY()>=a.minY()&&b.maxY()<=a.maxY()&&b.minZ()>=a.minZ()&&b.maxZ()<=a.maxZ();}
    private static UUID zoneId(String mapId,Cuboid region){return UUID.nameUUIDFromBytes(("opbp-pve-zone:"+mapId+":"+region).getBytes(StandardCharsets.UTF_8));}
    private static String revision(PveSettings value){try{byte[]hash=MessageDigest.getInstance("SHA-256").digest(value.toString().getBytes(StandardCharsets.UTF_8));return HexFormat.of().formatHex(hash);}catch(Exception impossible){throw new IllegalStateException("PvE設定revisionを作成できません。",impossible);}}

    public record View(String mapId,String revision,boolean editable,List<ZoneView>enemyZones){public View{enemyZones=List.copyOf(enemyZones);}}
    public record ZoneView(UUID enemyZoneId,Cuboid region,boolean finalArea,boolean configured,long spawnIntervalSeconds,int baseSpawnCount,int zombieWeight,int skeletonWeight,int creeperWeight,double minParticipantDistance,double maxParticipantDistance){}
    public record Update(String revision,UUID enemyZoneId,long spawnIntervalSeconds,int baseSpawnCount,int zombieWeight,int skeletonWeight,int creeperWeight,double minParticipantDistance,double maxParticipantDistance){public Update{if(revision==null||revision.isBlank())throw new IllegalArgumentException("PvE設定revisionが必要です。");Objects.requireNonNull(enemyZoneId);if(spawnIntervalSeconds>Integer.MAX_VALUE)throw new IllegalArgumentException("spawn intervalが範囲外です。");}}
}
