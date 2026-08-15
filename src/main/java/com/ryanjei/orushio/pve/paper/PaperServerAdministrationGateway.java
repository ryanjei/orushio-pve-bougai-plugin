package com.ryanjei.orushio.pve.paper;

import com.ryanjei.orushio.pve.application.*;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;

public final class PaperServerAdministrationGateway implements ServerAdministrationGateway {
    private final GameThreadExecutor executor;
    private final PlayerIdentityResolver identityResolver;
    private final PaperServerAccess server;
    private final Duration timeout;

    public PaperServerAdministrationGateway(GameThreadExecutor executor) {
        this(executor, new PaperPlayerIdentityResolver(executor), new BukkitServerAccess(), Duration.ofSeconds(3));
    }

    PaperServerAdministrationGateway(GameThreadExecutor executor, PlayerIdentityResolver identityResolver, PaperServerAccess server) {
        this(executor, identityResolver, server, Duration.ofSeconds(3));
    }

    PaperServerAdministrationGateway(GameThreadExecutor executor, PlayerIdentityResolver identityResolver, PaperServerAccess server, Duration timeout) {
        this.executor = executor;
        this.identityResolver = identityResolver;
        this.server = server;
        this.timeout = timeout;
    }

    @Override public List<OnlinePlayerView> onlinePlayers() { return execute(server::onlinePlayers); }
    @Override public boolean whitelistEnabled() { return execute(server::whitelistEnabled); }
    @Override public void setWhitelistEnabled(boolean enabled) { execute(() -> { server.setWhitelistEnabled(enabled); return null; }); }
    @Override public List<WhitelistEntryView> whitelistedPlayers() { return execute(server::whitelistedPlayers); }

    @Override public WhitelistEntryView addWhitelistedPlayer(String playerName) {
        try {
            ResolvedPlayerIdentity identity = identityResolver.resolve(playerName);
            return execute(() -> server.addWhitelistedPlayer(identity));
        } catch (ServerAdministrationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw unavailable();
        }
    }

    @Override public boolean removeWhitelistedPlayer(String playerName) { return execute(() -> server.removeWhitelistedPlayer(playerName)); }

    private <T> T execute(Callable<T> task) {
        try { return executor.execute(task, timeout); }
        catch (Exception exception) { throw unavailable(); }
    }

    private static ServerAdministrationException unavailable() { return new ServerAdministrationException("MINECRAFT_UNAVAILABLE", "Minecraftサーバー情報を取得できませんでした。"); }
}
