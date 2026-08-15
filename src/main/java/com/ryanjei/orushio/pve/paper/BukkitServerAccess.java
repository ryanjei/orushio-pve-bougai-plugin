package com.ryanjei.orushio.pve.paper;

import com.ryanjei.orushio.pve.application.OnlinePlayerView;
import com.ryanjei.orushio.pve.application.WhitelistEntryView;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import java.util.List;

final class BukkitServerAccess implements PaperServerAccess {
    @Override public List<OnlinePlayerView> onlinePlayers() { return Bukkit.getOnlinePlayers().stream().map(player -> new OnlinePlayerView(player.getUniqueId(), player.getName())).toList(); }
    @Override public boolean whitelistEnabled() { return Bukkit.hasWhitelist(); }
    @Override public void setWhitelistEnabled(boolean enabled) { Bukkit.setWhitelist(enabled); }
    @Override public List<WhitelistEntryView> whitelistedPlayers() { return Bukkit.getWhitelistedPlayers().stream().map(BukkitServerAccess::view).toList(); }
    @Override public WhitelistEntryView addWhitelistedPlayer(ResolvedPlayerIdentity identity) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(identity.uuid());
        player.setWhitelisted(true);
        return new WhitelistEntryView(identity.uuid(), player.getName() == null ? identity.name() : player.getName());
    }
    @Override public boolean removeWhitelistedPlayer(String playerName) {
        return Bukkit.getWhitelistedPlayers().stream().filter(player -> player.getName() != null && player.getName().equalsIgnoreCase(playerName)).findFirst().map(player -> { player.setWhitelisted(false); return true; }).orElse(false);
    }
    private static WhitelistEntryView view(OfflinePlayer player) { return new WhitelistEntryView(player.getUniqueId(), player.getName() == null ? "名前不明" : player.getName()); }
}
