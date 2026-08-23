package com.ryanjei.orushio.pve.application;
import com.ryanjei.orushio.pve.domain.GameLaunchSettings;
import java.util.List;
public record GameStartView(String mapId,String mapName,List<OnlinePlayerView> participants,GameLaunchSettings overrides,int timeLimitMinutes,int requiredNormalCores,double enemyMultiplier,boolean ready,List<String> missing){}
