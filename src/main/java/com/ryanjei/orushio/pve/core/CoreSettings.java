package com.ryanjei.orushio.pve.core;
public record CoreSettings(double normalCoreHp,double finalCoreHp){public CoreSettings{if(!Double.isFinite(normalCoreHp)||normalCoreHp<=0||!Double.isFinite(finalCoreHp)||finalCoreHp<=0)throw new IllegalArgumentException("Core HPは正の有限値が必要です。");}}
