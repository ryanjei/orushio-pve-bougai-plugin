package com.ryanjei.orushio.pve.paper;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;

class PaperGameThreadExecutorTest {
    @Test void timeoutした未開始mutationは後から実行されない() throws Exception {
        DelayedScheduler scheduler = new DelayedScheduler();
        PaperGameThreadExecutor executor = new PaperGameThreadExecutor(() -> true, () -> false, scheduler, () -> {});
        AtomicBoolean changed = new AtomicBoolean(false);
        assertThrows(TimeoutException.class, () -> executor.execute(() -> { changed.set(true); return null; }, Duration.ofMillis(20)));
        assertTrue(scheduler.cancelled.get());
        scheduler.runQueuedTask();
        assertFalse(changed.get());
    }

    @Test void 開始済みmutationは完了結果を待って返す() throws Exception {
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
        PaperGameThreadExecutor executor = new PaperGameThreadExecutor(() -> true, () -> false, runnable -> {
            new Thread(runnable).start();
            return () -> {};
        }, () -> {});
        CompletableFuture<String> response = CompletableFuture.supplyAsync(() -> {
            try { return executor.execute(() -> { started.countDown(); release.await(); return "適用済み"; }, Duration.ofMillis(20)); }
            catch (Exception exception) { throw new CompletionException(exception); }
        });
        assertTrue(started.await(1, TimeUnit.SECONDS));
        Thread.sleep(50);
        assertFalse(response.isDone());
        release.countDown();
        assertEquals("適用済み", response.get(1, TimeUnit.SECONDS));
    }

    private static final class DelayedScheduler implements PaperGameThreadExecutor.MainThreadScheduler {
        final AtomicBoolean cancelled = new AtomicBoolean();
        Runnable queued;
        public PaperGameThreadExecutor.ScheduledTask schedule(Runnable runnable) { queued = runnable; return () -> cancelled.set(true); }
        void runQueuedTask() { queued.run(); }
    }
}
