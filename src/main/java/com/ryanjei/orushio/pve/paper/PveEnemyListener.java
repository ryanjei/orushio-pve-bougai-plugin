package com.ryanjei.orushio.pve.paper;

import com.ryanjei.orushio.pve.application.GameApplicationService;
import com.ryanjei.orushio.pve.map.BlockPoint;
import com.ryanjei.orushio.pve.pve.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;

public final class PveEnemyListener implements Listener {
    private final GameApplicationService games;private final PveLifecycleStep pve;private final PveEnemyGateway gateway;
    public PveEnemyListener(GameApplicationService games,PveLifecycleStep pve,PveEnemyGateway gateway){this.games=games;this.pve=pve;this.gateway=gateway;}
    @EventHandler(ignoreCancelled=true)public void death(EntityDeathEvent event){gateway.ownership(event.getEntity().getUniqueId()).ifPresent(owner->pve.entityRemoved(event.getEntity().getUniqueId(),owner));}
    @EventHandler(ignoreCancelled=true,priority=EventPriority.HIGHEST)public void explosion(EntityExplodeEvent event){boolean creeper=event.getEntity() instanceof Creeper;gateway.ownership(event.getEntity().getUniqueId()).filter(owner->CreeperProtectionPolicy.suppressBlockDamage(creeper,pve.isCurrent(owner))).ifPresent(owner->event.blockList().clear());}
    @EventHandler(ignoreCancelled=true,priority=EventPriority.HIGHEST)public void damage(EntityDamageByEntityEvent event){if(!(event.getDamager() instanceof LivingEntity enemy)||!(event.getEntity() instanceof Player player))return;gateway.ownership(enemy.getUniqueId()).filter(owner->pve.protectsParticipant(games.current(),player.getUniqueId(),player.getWorld().getName(),new BlockPoint(player.getLocation().getBlockX(),player.getLocation().getBlockY(),player.getLocation().getBlockZ(),0,0),owner)).ifPresent(owner->event.setCancelled(true));}
}
