package com.ryanjei.orushio.pve.paper;

import com.ryanjei.orushio.pve.application.GameApplicationService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.logging.Level;

/** Paper events are reduced to immutable values before persistence is performed asynchronously. */
public final class GameLifecycleListener implements Listener {
    private final JavaPlugin plugin;
    private final GameApplicationService games;

    public GameLifecycleListener(JavaPlugin plugin, GameApplicationService games) {
        this.plugin = plugin;
        this.games = games;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        updateConnected(player.getUniqueId(), player.getName());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin,
                () -> safely(() -> games.playerDisconnected(playerId)));
    }

    private void updateConnected(UUID playerId, String name) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin,
                () -> safely(() -> games.playerConnected(playerId, name)));
    }

    private void safely(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException failure) {
            plugin.getLogger().log(Level.SEVERE,
                    "ゲーム参加者の接続状態を保存できませんでした。管理画面の診断情報を確認してください。");
        }
    }
}
