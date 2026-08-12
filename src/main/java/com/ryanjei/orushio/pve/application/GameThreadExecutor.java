package com.ryanjei.orushio.pve.application;

import java.time.Duration;
import java.util.concurrent.Callable;

public interface GameThreadExecutor {
    <T> T execute(Callable<T> task, Duration timeout) throws Exception;
    boolean isAcceptingTasks();
}
