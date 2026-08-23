package com.ryanjei.orushio.pve.map;

import java.util.Objects;
import java.util.UUID;

public record SpawnMarker(UUID markerId, String areaId, BlockPoint position, boolean enabled) {
    public SpawnMarker {
        Objects.requireNonNull(markerId);
        if (areaId == null || !areaId.matches("[A-Za-z][A-Za-z0-9]*:[0-9]+")) throw new IllegalArgumentException("areaIdが不正です。");
        Objects.requireNonNull(position);
    }
    public SpawnMarker withPosition(BlockPoint value){return new SpawnMarker(markerId,areaId,value,enabled);}
    public SpawnMarker withEnabled(boolean value){return new SpawnMarker(markerId,areaId,position,value);}
}
