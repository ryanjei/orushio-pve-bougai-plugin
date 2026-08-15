package com.ryanjei.orushio.pve.paper;

import com.ryanjei.orushio.pve.application.GameThreadExecutor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

public final class PaperGameThreadExecutor implements GameThreadExecutor, AutoCloseable {
    enum State { QUEUED, STARTED, FINISHED, CANCELLED }
    @FunctionalInterface interface MainThreadScheduler { ScheduledTask schedule(Runnable runnable); }
    @FunctionalInterface interface ScheduledTask { void cancel(); }

    private final AtomicBoolean accepting = new AtomicBoolean(true);
    private final BooleanSupplier pluginEnabled;
    private final BooleanSupplier primaryThread;
    private final MainThreadScheduler scheduler;
    private final Runnable cancelAll;

    public PaperGameThreadExecutor(Plugin plugin) {
        this(plugin::isEnabled, Bukkit::isPrimaryThread,
                runnable -> { var task = Bukkit.getScheduler().runTask(plugin, runnable); return task::cancel; },
                () -> Bukkit.getScheduler().cancelTasks(plugin));
    }

    PaperGameThreadExecutor(BooleanSupplier pluginEnabled, BooleanSupplier primaryThread, MainThreadScheduler scheduler, Runnable cancelAll) {
        this.pluginEnabled = pluginEnabled;
        this.primaryThread = primaryThread;
        this.scheduler = scheduler;
        this.cancelAll = cancelAll;
    }

    @Override public <T> T execute(Callable<T> task, Duration timeout) throws Exception {
        if (!accepting.get() || !pluginEnabled.getAsBoolean()) throw new IllegalStateException("プラグインは停止中です。");
        if (primaryThread.getAsBoolean()) return task.call();
        CompletableFuture<T> result = new CompletableFuture<>();
        AtomicReference<State> state = new AtomicReference<>(State.QUEUED);
        ScheduledTask scheduled = scheduler.schedule(() -> {
            if (!state.compareAndSet(State.QUEUED, State.STARTED)) return;
            try { result.complete(task.call()); }
            catch (Throwable exception) { result.completeExceptionally(exception); }
            finally { state.set(State.FINISHED); }
        });
        try {
            return result.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException timeoutException) {
            if (state.compareAndSet(State.QUEUED, State.CANCELLED)) {
                scheduled.cancel();
                throw timeoutException;
            }
            return result.get();
        }
    }

    @Override public boolean isAcceptingTasks() { return accepting.get(); }
    @Override public void close() { accepting.set(false); cancelAll.run(); }
}
