package com.ryanjei.orushio.pve.pve;

import java.util.*;
public final class DamageSourceSelector {
    private DamageSourceSelector() {}
    public static Optional<UUID> select(boolean projectile,UUID directDamager,UUID projectileShooterEntity){return projectile?Optional.ofNullable(projectileShooterEntity):Optional.ofNullable(directDamager);}
}
