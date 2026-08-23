package com.ryanjei.orushio.pve.pve;

import java.util.*;import java.util.function.*;
public final class PveDamageProtection {
    private PveDamageProtection() {}
    public static boolean shouldCancel(Optional<UUID> source,Function<UUID,Optional<EnemyOwnership>> ownership,Predicate<EnemyOwnership> protectedTarget){return source.flatMap(ownership).filter(protectedTarget).isPresent();}
}
