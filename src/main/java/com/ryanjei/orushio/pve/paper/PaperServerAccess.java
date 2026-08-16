package com.ryanjei.orushio.pve.paper;

import com.ryanjei.orushio.pve.application.OnlinePlayerView;
import com.ryanjei.orushio.pve.application.WhitelistEntryView;
import java.util.List;
import java.util.UUID;

interface PaperServerAccess {
    List<OnlinePlayerView> onlinePlayers();
    default OnlinePlayerView grantSetupAdministrator(UUID playerId) { throw new UnsupportedOperationException(); }
    boolean whitelistEnabled();
    void setWhitelistEnabled(boolean enabled);
    List<WhitelistEntryView> whitelistedPlayers();
    WhitelistEntryView addWhitelistedPlayer(ResolvedPlayerIdentity identity);
    boolean removeWhitelistedPlayer(String playerName);
}
