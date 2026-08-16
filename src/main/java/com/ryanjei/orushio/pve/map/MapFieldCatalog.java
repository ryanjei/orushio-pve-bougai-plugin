package com.ryanjei.orushio.pve.map;

import java.util.*;

public final class MapFieldCatalog {
    public record Field(String key,String label,boolean area,boolean required,int requiredCount) {}
    public static final List<Field> FIELDS=List.of(
        new Field("farmRegion","ファーム範囲",true,true,1),new Field("farmSpawn","ファーム開始地点",false,true,1),
        new Field("combatEntry","攻略入口",false,true,1),new Field("normalCoreCandidates","通常コア候補",false,true,3),
        new Field("finalCore","最終コア",false,true,1),new Field("gateRegions","最終ゲート範囲",true,true,1),
        new Field("finalRegion","最終エリア範囲",true,true,1),new Field("finalEntryTrigger","最終入口",false,true,1),
        new Field("checkpoints","チェックポイント",true,false,0),new Field("enemyZones","Enemy Zone範囲",true,true,1),
        new Field("resourceZones","資源ゾーン",true,false,0),new Field("shopPoints","ショップ地点",false,true,1));
    private static final Map<String,Field> BY_KEY=new LinkedHashMap<>();static{FIELDS.forEach(field->BY_KEY.put(field.key(),field));}
    private MapFieldCatalog(){}
    public static Field require(String key){Field field=BY_KEY.get(key);if(field==null)throw new IllegalArgumentException("登録項目が不正です。");return field;}
    public static String label(String key){return require(key).label();}
}
