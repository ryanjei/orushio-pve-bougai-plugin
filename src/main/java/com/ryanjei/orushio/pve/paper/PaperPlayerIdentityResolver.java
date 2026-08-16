package com.ryanjei.orushio.pve.paper;

import com.ryanjei.orushio.pve.application.GameThreadExecutor;
import org.bukkit.Bukkit;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

final class PaperPlayerIdentityResolver implements PlayerIdentityResolver {
    private static final Duration TIMEOUT = Duration.ofSeconds(3);
    private final GameThreadExecutor executor;

    PaperPlayerIdentityResolver(GameThreadExecutor executor) { this.executor = executor; }

    @Override public ResolvedPlayerIdentity resolve(String playerName) throws Exception {
        var incomplete = executor.execute(() -> Bukkit.createProfile(playerName), TIMEOUT);
        var resolved = incomplete.update().get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        if (resolved.getId() == null) throw new IllegalStateException("プレイヤーUUIDを解決できません。");
        String resolvedName = resolved.getName() == null ? playerName : resolved.getName();
        return new ResolvedPlayerIdentity(resolved.getId(), resolvedName);
    }
}
