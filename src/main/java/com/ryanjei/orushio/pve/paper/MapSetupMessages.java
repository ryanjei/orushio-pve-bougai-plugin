package com.ryanjei.orushio.pve.paper;

import com.ryanjei.orushio.pve.map.MapFieldCatalog;
import java.util.List;

final class MapSetupMessages {
    private MapSetupMessages(){}
    static List<String> completed(String field,boolean area,boolean secondCorner){String label="spawnMarker".equals(field)?"スポーン地点":MapFieldCatalog.label(field);if(!area)return List.of(label+"を設定しました。");if(!secondCorner)return List.of(label+"：1点目を設定しました。");return List.of(label+"：2点目を設定しました。",label+"の設定が完了しました。");}
}
