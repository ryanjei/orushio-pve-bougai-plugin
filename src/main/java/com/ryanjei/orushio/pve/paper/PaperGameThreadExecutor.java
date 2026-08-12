package com.ryanjei.orushio.pve.paper;

import com.ryanjei.orushio.pve.application.GameThreadExecutor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PaperGameThreadExecutor implements GameThreadExecutor, AutoCloseable {
    private final Plugin plugin;
    private final AtomicBoolean accepting = new AtomicBoolean(true);
    public PaperGameThreadExecutor(Plugin plugin) { this.plugin = plugin; }
    public <T> T execute(Callable<T> task, Duration timeout) throws Exception {
        if (!accepting.get() || !plugin.isEnabled()) throw new IllegalStateException("プラグインは停止中です。");
        if (Bukkit.isPrimaryThread()) return task.call();
        CompletableFuture<T> result = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> { try { result.complete(task.call()); } catch (Throwable e) { result.completeExceptionally(e); } });
        return result.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }
    public boolean isAcceptingTasks() { return accepting.get(); }
    public void close() { accepting.set(false); Bukkit.getScheduler().cancelTasks(plugin); }
}
