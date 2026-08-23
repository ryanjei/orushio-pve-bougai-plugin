package com.ryanjei.orushio.pve.paper;

import com.ryanjei.orushio.pve.map.MapFieldCatalog;
import com.ryanjei.orushio.pve.map.BlockPoint;
import java.util.List;

final class MapSetupMessages {
    private MapSetupMessages(){}
    static List<String> completed(String field,boolean area,boolean secondCorner){return completed(field,area,secondCorner,null);}
    static List<String> completed(String field,boolean area,boolean secondCorner,BlockPoint point){String label="spawnMarker".equals(field)?"スポーン地点":MapFieldCatalog.label(field),position=point==null?"":" X="+point.x()+" Y="+point.y()+" Z="+point.z();if(!area)return List.of("[OPBP] "+label+"を設定しました。"+position);if(!secondCorner)return List.of("[OPBP] "+label+"の1地点目を設定しました。"+position,"[OPBP] 2地点目を指定してください。");return List.of("[OPBP] "+label+"の2地点目を設定しました。"+position,"[OPBP] "+label+"の登録が完了しました。");}
}
