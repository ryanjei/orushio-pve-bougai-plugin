package com.ryanjei.orushio.pve.pve;

import java.util.*;import java.util.function.*;
public final class PveDamageProtection {
    private PveDamageProtection() {}
    public static Optional<EnemyOwnership> resolveOwnership(boolean projectile,UUID damager,UUID shooter,Function<UUID,Optional<EnemyOwnership>> ownership){if(!projectile)return ownership.apply(damager);Optional<EnemyOwnership>stored=ownership.apply(damager);return stored.isPresent()?stored:Optional.ofNullable(shooter).flatMap(ownership);}
    public static boolean shouldCancel(Optional<EnemyOwnership> ownership,Predicate<EnemyOwnership> protectedTarget){return ownership.filter(protectedTarget).isPresent();}
}
