package com.ryanjei.orushio.pve.pve;

import org.junit.jupiter.api.Test;
import java.util.*;import java.util.function.*;
import static org.junit.jupiter.api.Assertions.*;

class PveProjectileDamageProtectionTest {
    UUID currentSession=UUID.randomUUID(),otherSession=UUID.randomUUID(),zone=UUID.randomUUID(),arrow=UUID.randomUUID(),skeleton=UUID.randomUUID(),zombie=UUID.randomUUID(),natural=UUID.randomUUID(),player=UUID.randomUUID();
    EnemyOwnership current=new EnemyOwnership(currentSession,zone,"run-a"),other=new EnemyOwnership(otherSession,zone,"run-a");
    @Test void OPBPSkeletonProjectileはShooter生存中にFarm内だけcancelする(){Function<UUID,Optional<EnemyOwnership>>lookup=id->id.equals(skeleton)?Optional.of(current):Optional.empty();Optional<EnemyOwnership>owner=PveDamageProtection.resolveOwnership(true,arrow,skeleton,lookup);assertTrue(PveDamageProtection.shouldCancel(owner,value->true));assertFalse(PveDamageProtection.shouldCancel(owner,value->false));}
    @Test void Shooter削除後もProjectileに保存したownershipでcancelする(){Function<UUID,Optional<EnemyOwnership>>lookup=id->id.equals(arrow)?Optional.of(current):Optional.empty();Optional<EnemyOwnership>owner=PveDamageProtection.resolveOwnership(true,arrow,skeleton,lookup);assertTrue(PveDamageProtection.shouldCancel(owner,value->value.sessionId().equals(currentSession)));}
    @Test void 自然SkeletonとPlayerのProjectileはcancelしない(){Function<UUID,Optional<EnemyOwnership>>lookup=id->Optional.empty();assertFalse(PveDamageProtection.shouldCancel(PveDamageProtection.resolveOwnership(true,arrow,natural,lookup),owner->true));assertFalse(PveDamageProtection.shouldCancel(PveDamageProtection.resolveOwnership(true,arrow,player,lookup),owner->true));}
    @Test void 別SessionEnemyProjectileはcancelしない(){Function<UUID,Optional<EnemyOwnership>>lookup=id->Optional.of(other);assertFalse(PveDamageProtection.shouldCancel(PveDamageProtection.resolveOwnership(true,arrow,skeleton,lookup),owner->owner.sessionId().equals(currentSession)));}
    @Test void 直接Zombie攻撃は従来どおりcancelする(){Function<UUID,Optional<EnemyOwnership>>lookup=id->id.equals(zombie)?Optional.of(current):Optional.empty();assertTrue(PveDamageProtection.shouldCancel(PveDamageProtection.resolveOwnership(false,zombie,null,lookup),owner->owner.sessionId().equals(currentSession)));}
    @Test void ProjectileShooterがEntity以外またはnullなら保存ownershipがなければsourceなし(){Function<UUID,Optional<EnemyOwnership>>lookup=id->Optional.empty();assertTrue(PveDamageProtection.resolveOwnership(true,arrow,null,lookup).isEmpty());}
}
