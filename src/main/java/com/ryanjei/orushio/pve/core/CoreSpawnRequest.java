package com.ryanjei.orushio.pve.core;
import com.ryanjei.orushio.pve.map.BlockPoint;
public record CoreSpawnRequest(CoreOwnership ownership,BlockPoint position,double maxHp){}
