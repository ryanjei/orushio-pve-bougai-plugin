package com.ryanjei.orushio.pve.paper;

import com.ryanjei.orushio.pve.application.GameApplicationService;
import com.ryanjei.orushio.pve.map.BlockPoint;
import com.ryanjei.orushio.pve.pve.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;

import java.util.*;

public final class PveEnemyListener implements Listener {
    private final GameApplicationService games;private final PveLifecycleStep pve;private final PveEnemyGateway gateway;private final ProjectileOwnershipGateway projectiles;
    public PveEnemyListener(GameApplicationService games,PveLifecycleStep pve,PveEnemyGateway gateway,ProjectileOwnershipGateway projectiles){this.games=games;this.pve=pve;this.gateway=gateway;this.projectiles=projectiles;}
    @EventHandler(ignoreCancelled=true)public void death(EntityDeathEvent event){gateway.ownership(event.getEntity().getUniqueId()).ifPresent(owner->pve.entityRemoved(event.getEntity().getUniqueId(),owner));}
    @EventHandler(ignoreCancelled=true)public void launch(ProjectileLaunchEvent event){Projectile projectile=event.getEntity();if(projectile.getShooter() instanceof Entity shooter)projectiles.inheritOwnership(projectile.getUniqueId(),shooter.getUniqueId());}
    @EventHandler(ignoreCancelled=true,priority=EventPriority.HIGHEST)public void explosion(EntityExplodeEvent event){boolean creeper=event.getEntity() instanceof Creeper;gateway.ownership(event.getEntity().getUniqueId()).filter(owner->CreeperProtectionPolicy.suppressBlockDamage(creeper,pve.isCurrent(owner))).ifPresent(owner->event.blockList().clear());}
    @EventHandler(ignoreCancelled=true,priority=EventPriority.HIGHEST)public void damage(EntityDamageByEntityEvent event){if(!(event.getEntity() instanceof Player player))return;Entity damager=event.getDamager();UUID shooter=null;if(damager instanceof Projectile projectile&&projectile.getShooter() instanceof Entity entity)shooter=entity.getUniqueId();Optional<EnemyOwnership>ownership=PveDamageProtection.resolveOwnership(damager instanceof Projectile,damager.getUniqueId(),shooter,gateway::ownership);BlockPoint position=new BlockPoint(player.getLocation().getBlockX(),player.getLocation().getBlockY(),player.getLocation().getBlockZ(),0,0);if(PveDamageProtection.shouldCancel(ownership,owner->pve.protectsParticipant(games.current(),player.getUniqueId(),player.getWorld().getName(),position,owner)))event.setCancelled(true);}
}
