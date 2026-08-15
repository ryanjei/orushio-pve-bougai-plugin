package com.ryanjei.orushio.pve.application;

import java.util.List;

public interface ServerAdministrationService {
    List<OnlinePlayerView> onlinePlayers();
    boolean whitelistEnabled();
    boolean setWhitelistEnabled(boolean enabled);
    List<WhitelistEntryView> whitelistedPlayers();
    WhitelistEntryView addWhitelistedPlayer(String playerName);
    void removeWhitelistedPlayer(String playerName);
}
