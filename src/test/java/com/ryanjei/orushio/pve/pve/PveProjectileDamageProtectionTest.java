package com.ryanjei.orushio.pve.pve;

import org.junit.jupiter.api.Test;
import java.util.*;import java.util.function.*;
import static org.junit.jupiter.api.Assertions.*;

class PveProjectileDamageProtectionTest {
    UUID currentSession=UUID.randomUUID(),otherSession=UUID.randomUUID(),zone=UUID.randomUUID(),skeleton=UUID.randomUUID(),zombie=UUID.randomUUID(),natural=UUID.randomUUID(),player=UUID.randomUUID();
    EnemyOwnership current=new EnemyOwnership(currentSession,zone,"run-a"),other=new EnemyOwnership(otherSession,zone,"run-a");
    @Test void OPBPSkeletonProjectileはFarm内だけcancelする(){Function<UUID,Optional<EnemyOwnership>>lookup=id->id.equals(skeleton)?Optional.of(current):Optional.empty();Optional<UUID>source=DamageSourceSelector.select(true,UUID.randomUUID(),skeleton);assertTrue(PveDamageProtection.shouldCancel(source,lookup,owner->true));assertFalse(PveDamageProtection.shouldCancel(source,lookup,owner->false));}
    @Test void 自然SkeletonとPlayerのProjectileはcancelしない(){Function<UUID,Optional<EnemyOwnership>>lookup=id->Optional.empty();assertFalse(PveDamageProtection.shouldCancel(DamageSourceSelector.select(true,UUID.randomUUID(),natural),lookup,owner->true));assertFalse(PveDamageProtection.shouldCancel(DamageSourceSelector.select(true,UUID.randomUUID(),player),lookup,owner->true));}
    @Test void 別SessionEnemyProjectileはcancelしない(){Function<UUID,Optional<EnemyOwnership>>lookup=id->Optional.of(other);assertFalse(PveDamageProtection.shouldCancel(DamageSourceSelector.select(true,UUID.randomUUID(),skeleton),lookup,owner->owner.sessionId().equals(currentSession)));}
    @Test void 直接Zombie攻撃は従来どおりcancelする(){Function<UUID,Optional<EnemyOwnership>>lookup=id->id.equals(zombie)?Optional.of(current):Optional.empty();assertTrue(PveDamageProtection.shouldCancel(DamageSourceSelector.select(false,zombie,null),lookup,owner->owner.sessionId().equals(currentSession)));}
    @Test void ProjectileShooterがEntity以外またはnullならsourceなし(){assertTrue(DamageSourceSelector.select(true,UUID.randomUUID(),null).isEmpty());assertEquals(zombie,DamageSourceSelector.select(false,zombie,null).orElseThrow());}
}
