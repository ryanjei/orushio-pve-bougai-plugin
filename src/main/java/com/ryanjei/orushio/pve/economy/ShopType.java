package com.ryanjei.orushio.pve.economy;

import com.ryanjei.orushio.pve.map.SetupMarkerType;

public enum ShopType {
    WEAPON(SetupMarkerType.WEAPON_SHOP),
    ARMOR(SetupMarkerType.ARMOR_SHOP),
    RECOVERY(SetupMarkerType.RECOVERY_SHOP),
    SPECIAL(SetupMarkerType.SPECIAL_SHOP);

    private final SetupMarkerType setupMarkerType;

    ShopType(SetupMarkerType setupMarkerType) { this.setupMarkerType = setupMarkerType; }

    public SetupMarkerType setupMarkerType() { return setupMarkerType; }

    public boolean matches(SetupMarkerType markerType) { return setupMarkerType == markerType; }
}
