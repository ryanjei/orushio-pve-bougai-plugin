package com.ryanjei.orushio.pve.paper;

import com.ryanjei.orushio.pve.application.ParticipantConnectionDispatcher;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Paper events are reduced to immutable values before persistence is performed asynchronously. */
public final class GameLifecycleListener implements Listener {
    private final ParticipantConnectionDispatcher dispatcher;

    public GameLifecycleListener(ParticipantConnectionDispatcher dispatcher) {
        this.dispatcher = dispatcher;
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
}
