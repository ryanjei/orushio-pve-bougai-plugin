package com.ryanjei.orushio.pve.application;
import com.ryanjei.orushio.pve.domain.GameLaunchSettings;import com.ryanjei.orushio.pve.map.MapProfile;import java.util.*;
public interface GameReadinessValidator{List<String>missing(MapProfile map,GameLaunchSettings launch);static GameReadinessValidator none(){return (map,launch)->List.of();}}
