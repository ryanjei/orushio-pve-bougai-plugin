package com.ryanjei.orushio.pve.persistence;
import com.ryanjei.orushio.pve.domain.GameLaunchSettings;
public interface GameLaunchSettingsRepository{GameLaunchSettings load(String mapId);void save(String mapId,GameLaunchSettings settings);}
