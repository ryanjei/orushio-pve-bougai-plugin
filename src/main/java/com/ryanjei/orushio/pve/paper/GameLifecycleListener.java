package com.ryanjei.orushio.pve.paper;

import com.ryanjei.orushio.pve.application.ParticipantConnectionDispatcher;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import com.ryanjei.orushio.pve.application.GameApplicationService;
import org.bukkit.plugin.Plugin;

/** Paper events are reduced to immutable values before persistence is performed asynchronously. */
public final class GameLifecycleListener implements Listener {
    private final ParticipantConnectionDispatcher dispatcher;
    private final GameApplicationService games;
    private final Plugin plugin;

    public GameLifecycleListener(ParticipantConnectionDispatcher dispatcher,GameApplicationService games,Plugin plugin) {
        this.dispatcher = dispatcher;
        this.games = games;
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        dispatcher.connected(player.getUniqueId(), player.getName());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        dispatcher.disconnected(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event){Player player=event.getEntity();if(!games.playerDied(player.getUniqueId(),player.getWorld().getName()))return;event.setKeepInventory(true);event.getDrops().clear();event.setKeepLevel(true);event.setDroppedExp(0);}

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event){var playerId=event.getPlayer().getUniqueId();plugin.getServer().getScheduler().runTask(plugin,()->games.playerRespawned(playerId));}
}
