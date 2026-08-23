package com.ryanjei.orushio.pve.map;

public enum SetupMarkerType {
    NORMAL_ENEMY_SPAWN("通常敵スポーン",Category.ENEMY),NORMAL_CORE_ROOM_ENEMY_SPAWN("通常コア部屋 敵スポーン",Category.ENEMY),FINAL_AREA_ENEMY_SPAWN("最終エリア 敵スポーン",Category.ENEMY),FINAL_CORE_ROOM_ENEMY_SPAWN("最終コア部屋 敵スポーン",Category.ENEMY),
    NORMAL_CORE("通常コア候補",Category.CORE),FINAL_CORE("最終コア",Category.CORE),ORE_RESOURCE("鉱石生成",Category.RESOURCE),FARM_RESOURCE("農作物生成",Category.RESOURCE),
    WEAPON_SHOP("武器ショップ",Category.SHOP),ARMOR_SHOP("防具ショップ",Category.SHOP),RECOVERY_SHOP("回復ショップ",Category.SHOP),SPECIAL_SHOP("特殊ショップ",Category.SHOP),
    FARM_RESPAWN("ファーム復帰地点",Category.MOVEMENT),GAME_START("ゲーム開始地点",Category.MOVEMENT),FINAL_AREA_ENTRY("最終エリア移動地点",Category.MOVEMENT),CHECKPOINT("チェックポイント",Category.MOVEMENT),FINAL_GATE("最終ゲート構成ブロック",Category.GATE);
    private final String label;private final Category category;SetupMarkerType(String label,Category category){this.label=label;this.category=category;}public String label(){return label;}public Category category(){return category;}
    public enum Category{ENEMY,CORE,RESOURCE,SHOP,MOVEMENT,GATE}
}
