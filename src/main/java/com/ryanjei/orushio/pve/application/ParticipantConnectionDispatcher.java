package com.ryanjei.orushio.pve.application;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Applies Paper connection events in submission order without performing file I/O on the main thread. */
public final class ParticipantConnectionDispatcher implements AutoCloseable {
    private final GameApplicationService games;
    private final Consumer<RuntimeException> failureHandler;
    private final ExecutorService executor;

    public ParticipantConnectionDispatcher(GameApplicationService games, Consumer<RuntimeException> failureHandler) {
        this(games, failureHandler, Executors.newSingleThreadExecutor(Thread.ofPlatform().name("opbp-participant-events").factory()));
    }

    ParticipantConnectionDispatcher(GameApplicationService games, Consumer<RuntimeException> failureHandler, ExecutorService executor) {
        this.games = Objects.requireNonNull(games);
        this.failureHandler = Objects.requireNonNull(failureHandler);
        this.executor = Objects.requireNonNull(executor);
    }

    public void connected(UUID playerId, String name) {
        submit(() -> games.playerConnected(playerId, name));
    }

    public void disconnected(UUID playerId) {
        submit(() -> games.playerDisconnected(playerId));
    }

    private void submit(Runnable action) {
        executor.execute(() -> {
            try { action.run(); }
            catch (RuntimeException failure) { failureHandler.accept(failure); }
        });
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) executor.shutdownNow();
        } catch (InterruptedException interrupted) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
