package com.ryanjei.orushio.pve.application;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.UUID;

public final class DefaultServerAdministrationService implements ServerAdministrationService {
    private static final Pattern PLAYER_NAME = Pattern.compile("[A-Za-z0-9_]{3,16}");
    private final ServerAdministrationGateway gateway;

    public DefaultServerAdministrationService(ServerAdministrationGateway gateway) {
        this.gateway = Objects.requireNonNull(gateway);
    }

    @Override public List<OnlinePlayerView> onlinePlayers() {
        return gateway.onlinePlayers().stream().sorted(Comparator.comparing(OnlinePlayerView::name, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    @Override public synchronized OnlinePlayerView grantSetupAdministrator(UUID playerId) {
        return gateway.grantSetupAdministrator(Objects.requireNonNull(playerId));
    }

    @Override public boolean whitelistEnabled() { return gateway.whitelistEnabled(); }

    @Override public synchronized boolean setWhitelistEnabled(boolean enabled) {
        gateway.setWhitelistEnabled(enabled);
        return gateway.whitelistEnabled();
    }

    @Override public List<WhitelistEntryView> whitelistedPlayers() {
        return gateway.whitelistedPlayers().stream().sorted(Comparator.comparing(WhitelistEntryView::name, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    @Override public synchronized WhitelistEntryView addWhitelistedPlayer(String playerName) {
        String name = validatedName(playerName);
        if (gateway.whitelistedPlayers().stream().anyMatch(entry -> entry.name().equalsIgnoreCase(name))) {
            throw new ServerAdministrationException("PLAYER_ALREADY_WHITELISTED", "このプレイヤーはすでに登録されています。");
        }
        return gateway.addWhitelistedPlayer(name);
    }

    @Override public synchronized void removeWhitelistedPlayer(String playerName) {
        String name = validatedName(playerName);
        if (!gateway.removeWhitelistedPlayer(name)) {
            throw new ServerAdministrationException("PLAYER_NOT_WHITELISTED", "指定されたプレイヤーは登録されていません。");
        }
    }

    private static String validatedName(String playerName) {
        String name = playerName == null ? "" : playerName.trim();
        if (!PLAYER_NAME.matcher(name).matches()) {
            throw new ServerAdministrationException("INVALID_PLAYER_NAME", "プレイヤー名は3～16文字の半角英数字またはアンダースコアで入力してください。");
        }
        return name;
    }
}
