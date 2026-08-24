package com.ryanjei.orushio.pve.pve;

public record PveRuntimeView(boolean active,int enemyCount,int enemyLimit,int participantCount,double difficultyMultiplier,boolean finalAreaUnlocked){
    public static PveRuntimeView idle(){return new PveRuntimeView(false,0,0,0,0,false);}
}
