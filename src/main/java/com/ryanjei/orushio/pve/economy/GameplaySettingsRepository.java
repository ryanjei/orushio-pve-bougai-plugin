package com.ryanjei.orushio.pve.economy;
public interface GameplaySettingsRepository {GameplaySettings load(String mapId);default void save(String mapId,GameplaySettings settings){throw new UnsupportedOperationException();}}
