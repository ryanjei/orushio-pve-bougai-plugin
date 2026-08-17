package com.ryanjei.orushio.pve.domain;

import java.util.Optional;

public record GameLaunchSettings(Optional<Integer> timeLimitMinutes,Optional<Integer> requiredNormalCores,Optional<Double> enemyMultiplier){
    public static final int DEFAULT_TIME_LIMIT_MINUTES=60,DEFAULT_REQUIRED_NORMAL_CORES=2;public static final double DEFAULT_ENEMY_MULTIPLIER=1.0;
    public GameLaunchSettings{timeLimitMinutes=timeLimitMinutes==null?Optional.empty():timeLimitMinutes;requiredNormalCores=requiredNormalCores==null?Optional.empty():requiredNormalCores;enemyMultiplier=enemyMultiplier==null?Optional.empty():enemyMultiplier;timeLimitMinutes.ifPresent(v->range(v,1,1440,"制限時間"));requiredNormalCores.ifPresent(v->range(v,1,1000,"攻略通常コア数"));enemyMultiplier.ifPresent(v->{if(!Double.isFinite(v)||v<0.01||v>100)throw new IllegalArgumentException("敵人数倍率が範囲外です。");});}
    public static GameLaunchSettings defaults(){return new GameLaunchSettings(Optional.empty(),Optional.empty(),Optional.empty());}
    public int resolvedTimeLimitMinutes(){return timeLimitMinutes.orElse(DEFAULT_TIME_LIMIT_MINUTES);}public int resolvedRequiredNormalCores(){return requiredNormalCores.orElse(DEFAULT_REQUIRED_NORMAL_CORES);}public double resolvedEnemyMultiplier(){return enemyMultiplier.orElse(DEFAULT_ENEMY_MULTIPLIER);}
    private static void range(int value,int minimum,int maximum,String name){if(value<minimum||value>maximum)throw new IllegalArgumentException(name+"が範囲外です。");}
}
