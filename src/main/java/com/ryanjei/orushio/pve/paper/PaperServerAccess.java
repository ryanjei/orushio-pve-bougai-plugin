package com.ryanjei.orushio.pve.paper;

import com.ryanjei.orushio.pve.application.OnlinePlayerView;
import com.ryanjei.orushio.pve.application.WhitelistEntryView;
import java.util.List;

interface PaperServerAccess {
    List<OnlinePlayerView> onlinePlayers();
    boolean whitelistEnabled();
    void setWhitelistEnabled(boolean enabled);
    List<WhitelistEntryView> whitelistedPlayers();
    WhitelistEntryView addWhitelistedPlayer(ResolvedPlayerIdentity identity);
    boolean removeWhitelistedPlayer(String playerName);
}
