package com.ryanjei.orushio.pve.pve;

public final class CreeperProtectionPolicy {
    private CreeperProtectionPolicy() {}
    public static boolean suppressBlockDamage(boolean creeper,boolean currentOwnedRuntime){return creeper&&currentOwnedRuntime;}
}
