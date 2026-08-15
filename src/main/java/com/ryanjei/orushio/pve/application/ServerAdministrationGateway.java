package com.ryanjei.orushio.pve.application;

import java.util.List;

public interface ServerAdministrationGateway {
    List<OnlinePlayerView> onlinePlayers();
    boolean whitelistEnabled();
    void setWhitelistEnabled(boolean enabled);
    List<WhitelistEntryView> whitelistedPlayers();
    WhitelistEntryView addWhitelistedPlayer(String playerName);
    boolean removeWhitelistedPlayer(String playerName);
}
