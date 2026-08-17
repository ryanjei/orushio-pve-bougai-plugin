package com.ryanjei.orushio.pve.application;

import java.util.List;
import java.util.UUID;

public interface ServerAdministrationGateway {
    List<OnlinePlayerView> onlinePlayers();
    default OnlinePlayerView grantSetupAdministrator(UUID playerId) { throw new UnsupportedOperationException(); }
    default OnlinePlayerView revokeSetupAdministrator(UUID playerId) { throw new UnsupportedOperationException(); }
    default OnlinePlayerView adoptSetupAdministrator(UUID playerId) { throw new UnsupportedOperationException(); }
    boolean whitelistEnabled();
    void setWhitelistEnabled(boolean enabled);
    List<WhitelistEntryView> whitelistedPlayers();
    WhitelistEntryView addWhitelistedPlayer(String playerName);
    boolean removeWhitelistedPlayer(String playerName);
}
