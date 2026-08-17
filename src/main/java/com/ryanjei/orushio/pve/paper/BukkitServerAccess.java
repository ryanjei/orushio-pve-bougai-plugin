package com.ryanjei.orushio.pve.paper;

import com.ryanjei.orushio.pve.application.OnlinePlayerView;
import com.ryanjei.orushio.pve.application.WhitelistEntryView;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import java.util.List;
import java.util.UUID;

final class BukkitServerAccess implements PaperServerAccess {
    private final NamespacedKey grantedKey;
    private final SetupAdministratorOwnership ownership;
    BukkitServerAccess(SetupAdministratorOwnership ownership) { this.ownership=ownership;grantedKey=new NamespacedKey("orushio","setup_administrator_granted"); }
    @Override public List<OnlinePlayerView> onlinePlayers() { return Bukkit.getOnlinePlayers().stream().map(player -> new OnlinePlayerView(player.getUniqueId(), player.getName(), setupAdministrator(player),revocable(player))).toList(); }
    @Override public OnlinePlayerView grantSetupAdministrator(UUID playerId) {
        var player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) throw new com.ryanjei.orushio.pve.application.ServerAdministrationException("PLAYER_NOT_ONLINE", "指定されたプレイヤーは現在オンラインではありません。");
        if (!setupAdministrator(player)) {
            ownership.add(playerId);
            try {
                player.getPersistentDataContainer().set(grantedKey,PersistentDataType.BYTE,(byte)1);
                player.setOp(true);
            } catch (RuntimeException failure) {
                player.getPersistentDataContainer().remove(grantedKey);
                ownership.remove(playerId);
                throw failure;
            }
        }
        return new OnlinePlayerView(player.getUniqueId(), player.getName(), setupAdministrator(player),revocable(player));
    }
    @Override public OnlinePlayerView revokeSetupAdministrator(UUID playerId) { var player=Bukkit.getPlayer(playerId);if(player==null||!player.isOnline())throw new com.ryanjei.orushio.pve.application.ServerAdministrationException("PLAYER_NOT_ONLINE","指定されたプレイヤーは現在オンラインではありません。");if(!revocable(player))throw new com.ryanjei.orushio.pve.application.ServerAdministrationException("ADMINISTRATOR_NOT_PLUGIN_GRANTED","この管理者権限はOrushioから付与されたものではないため解除できません。");player.setOp(false);player.getPersistentDataContainer().remove(grantedKey);ownership.remove(playerId);return new OnlinePlayerView(player.getUniqueId(),player.getName(),setupAdministrator(player),false); }
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
    private static boolean setupAdministrator(org.bukkit.entity.Player player) { return player.isOp() && player.hasPermission("orushio.pve.admin"); }
    private boolean revocable(org.bukkit.entity.Player player){return ownership.contains(player.getUniqueId())||player.getPersistentDataContainer().has(grantedKey,PersistentDataType.BYTE);}
}
