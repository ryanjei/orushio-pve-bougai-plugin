package com.ryanjei.orushio.pve.paper;

import com.ryanjei.orushio.pve.application.GameThreadExecutor;
import java.time.Duration;

public final class PaperShutdownController {
    private final GameThreadExecutor executor;private final Runnable shutdown;
    public PaperShutdownController(GameThreadExecutor executor,Runnable shutdown){this.executor=executor;this.shutdown=shutdown;}
    public void request(){try{executor.execute(()->{shutdown.run();return null;},Duration.ofSeconds(10));}catch(Exception exception){throw new IllegalStateException("Paperへ安全停止を要求できませんでした。",exception);}}
}
