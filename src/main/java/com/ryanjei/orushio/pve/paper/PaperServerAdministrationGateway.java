package com.ryanjei.orushio.pve.paper;

import com.ryanjei.orushio.pve.application.*;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import java.time.Duration;
import java.util.List;

public final class PaperServerAdministrationGateway implements ServerAdministrationGateway {
    private static final Duration TIMEOUT = Duration.ofSeconds(3);
    private final GameThreadExecutor executor;

    public PaperServerAdministrationGateway(GameThreadExecutor executor) { this.executor = executor; }

    @Override public List<OnlinePlayerView> onlinePlayers() {
        return execute(() -> Bukkit.getOnlinePlayers().stream().map(player -> new OnlinePlayerView(player.getUniqueId(), player.getName())).toList());
    }

    @Override public boolean whitelistEnabled() { return execute(Bukkit::hasWhitelist); }

    @Override public void setWhitelistEnabled(boolean enabled) { execute(() -> { Bukkit.setWhitelist(enabled); return null; }); }

    @Override public List<WhitelistEntryView> whitelistedPlayers() {
        return execute(() -> Bukkit.getWhitelistedPlayers().stream().map(PaperServerAdministrationGateway::view).toList());
    }

    @Override public WhitelistEntryView addWhitelistedPlayer(String playerName) {
        return execute(() -> { OfflinePlayer player = Bukkit.getOfflinePlayer(playerName); player.setWhitelisted(true); return view(player); });
    }

    @Override public boolean removeWhitelistedPlayer(String playerName) {
        return execute(() -> Bukkit.getWhitelistedPlayers().stream().filter(player -> player.getName() != null && player.getName().equalsIgnoreCase(playerName)).findFirst().map(player -> { player.setWhitelisted(false); return true; }).orElse(false));
    }

    private static WhitelistEntryView view(OfflinePlayer player) {
        String name = player.getName();
        return new WhitelistEntryView(player.getUniqueId(), name == null ? "名前不明" : name);
    }

    private <T> T execute(java.util.concurrent.Callable<T> task) {
        try { return executor.execute(task, TIMEOUT); }
        catch (Exception exception) { throw new ServerAdministrationException("MINECRAFT_UNAVAILABLE", "Minecraftサーバー情報を取得できませんでした。"); }
    }
}
