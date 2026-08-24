package com.ryanjei.orushio.pve.economy;

import com.ryanjei.orushio.pve.core.*;
import com.ryanjei.orushio.pve.domain.*;
import com.ryanjei.orushio.pve.map.*;
import com.ryanjei.orushio.pve.persistence.RepositoryException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;

/** 管理画面からCore/資源設定を、既存のMap別Repositoryへ安全に保存する。 */
public final class MapEconomySettingsAdministrationService {
    private final CoreSettingsRepository cores; private final GameplaySettingsRepository gameplay;
    private final MapProfileRepository profiles; private final ResourceMaterialValidator materials;
    private final Supplier<GameSession> games;
    public MapEconomySettingsAdministrationService(CoreSettingsRepository cores,GameplaySettingsRepository gameplay,MapProfileRepository profiles,ResourceMaterialValidator materials,Supplier<GameSession> games){this.cores=cores;this.gameplay=gameplay;this.profiles=profiles;this.materials=materials;this.games=games;}

    public synchronized CoreView loadCore(String mapId){profile(mapId);try{CoreSettings v=cores.load(mapId);return new CoreView(mapId,true,v.normalCoreHp(),v.finalCoreHp(),editable());}catch(RepositoryException missing){if(!"core-settings.ymlがありません。".equals(missing.getMessage()))throw missing;return new CoreView(mapId,false,null,null,editable());}}
    public synchronized CoreView updateCore(String mapId,double normal,double finale){writable();profile(mapId);CoreSettings value=new CoreSettings(normal,finale);cores.save(mapId,value);CoreSettings saved=cores.load(mapId);if(!saved.equals(value))throw new IllegalStateException("Core HP設定の保存結果を確認できませんでした。");return new CoreView(mapId,true,saved.normalCoreHp(),saved.finalCoreHp(),editable());}

    public synchronized ResourceView loadResources(String mapId){MapProfile profile=profile(mapId);GameplaySettings value=gameplay.load(mapId);rejectExtra(profile,value);return resourceView(mapId,profile,value);}
    public synchronized ResourceView updateResource(String mapId,ResourceUpdate update){
        writable();MapProfile profile=profile(mapId);GameplaySettings current=gameplay.load(mapId);rejectExtra(profile,current);if(!revision(current).equals(update.revision()))throw new IllegalStateException("資源設定が別画面で更新されています。再読込してください。");
        List<GameplaySettings.ResourceZone> zones=new ArrayList<>(current.resourceZones());int index=-1;for(int i=0;i<zones.size();i++)if(zones.get(i).resourceZoneId().equals(update.resourceZoneId())){index=i;break;}
        Cuboid region=index>=0?zones.get(index).region():profile.areas().getOrDefault("resourceZones",List.of()).stream().filter(r->zoneId(mapId,r).equals(update.resourceZoneId())).findFirst().orElseThrow(()->new IllegalArgumentException("指定された資源ZoneはMap Setupにありません。"));
        String material=update.material().trim();if(!material.equals(material.toUpperCase(Locale.ROOT))||!materials.isValidBlockMaterial(material))throw new IllegalArgumentException("資源Materialは有効な大文字Block Material名が必要です。");
        GameplaySettings.ResourceZone replacement=new GameplaySettings.ResourceZone(update.resourceZoneId(),update.category(),material,update.pointValue(),Duration.ofSeconds(update.respawnSeconds()),region);
        if(index>=0)zones.set(index,replacement);else zones.add(replacement);
        GameplaySettings candidate=new GameplaySettings(zones,current.shops());rejectExtra(profile,candidate);gameplay.save(mapId,candidate);GameplaySettings saved=gameplay.load(mapId);if(!saved.equals(candidate))throw new IllegalStateException("資源設定の保存結果を確認できませんでした。");return resourceView(mapId,profile,saved);
    }
    private ResourceView resourceView(String mapId,MapProfile profile,GameplaySettings value){List<ResourceZoneView> result=new ArrayList<>();for(Cuboid region:profile.areas().getOrDefault("resourceZones",List.of())){List<GameplaySettings.ResourceZone> matches=value.resourceZones().stream().filter(z->z.region().equals(region)).toList();if(matches.isEmpty())result.add(new ResourceZoneView(zoneId(mapId,region),region,false,"","",0,0));else for(var z:matches)result.add(new ResourceZoneView(z.resourceZoneId(),region,true,z.category().name(),z.material(),z.pointValue(),z.respawnDelay().toSeconds()));}return new ResourceView(mapId,revision(value),editable(),result);}
    private void rejectExtra(MapProfile profile,GameplaySettings value){List<Cuboid> setup=profile.areas().getOrDefault("resourceZones",List.of());if(value.resourceZones().stream().anyMatch(z->!setup.contains(z.region())))throw new IllegalArgumentException("資源Zone設定がMap Setupと一致しません。");}
    private MapProfile profile(String mapId){return profiles.find(new MapProfileId(mapId)).orElseThrow(()->new IllegalArgumentException("指定されたマップがありません。"));}
    private boolean editable(){GameState s=games.get().state();return s==GameState.IDLE||s==GameState.RECRUITING;}
    private void writable(){if(!editable())throw new IllegalStateException("Core・資源設定は待機中だけ変更できます。");}
    private static UUID zoneId(String mapId,Cuboid region){return UUID.nameUUIDFromBytes(("opbp-resource-zone:"+mapId+":"+region).getBytes(StandardCharsets.UTF_8));}
    private static String revision(GameplaySettings value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.toString().getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    public record CoreView(String mapId,boolean configured,Double normalCoreHp,Double finalCoreHp,boolean editable){}
    public record ResourceView(String mapId,String revision,boolean editable,List<ResourceZoneView> resourceZones){public ResourceView{resourceZones=List.copyOf(resourceZones);}}
    public record ResourceZoneView(UUID resourceZoneId,Cuboid region,boolean configured,String category,String material,int pointValue,long respawnSeconds){}
    public record ResourceUpdate(String revision,UUID resourceZoneId,PointCategory category,String material,int pointValue,long respawnSeconds){public ResourceUpdate{if(revision==null||revision.isBlank()||resourceZoneId==null||category==null||material==null)throw new IllegalArgumentException("資源設定の必須項目がありません。");if(respawnSeconds<=0)throw new IllegalArgumentException("再生成時間は1秒以上が必要です。");}}
}
