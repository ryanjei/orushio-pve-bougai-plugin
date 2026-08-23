package com.ryanjei.orushio.pve.pve;

public interface PveSettingsRepository {
    PveSettings load(String mapId);
    default void save(String mapId, PveSettings settings) { throw new UnsupportedOperationException(); }
}
