package com.ryanjei.orushio.pve.application;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Duration;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;

class GameThreadExecutorContractTest {
    @Test void ApplicationはGatewayの完了結果を待てる() throws Exception {
        GameThreadExecutor fake = new GameThreadExecutor() {
            public <T> T execute(Callable<T> task, Duration timeout) throws Exception { return task.call(); }
            public boolean isAcceptingTasks() { return true; }
        };
        assertEquals("完了", fake.execute(() -> "完了", Duration.ofSeconds(1)));
    }
}
