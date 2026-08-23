package com.ryanjei.orushio.pve.core;
public interface CoreSettingsRepository { CoreSettings load(String mapId); void save(String mapId,CoreSettings settings); }
