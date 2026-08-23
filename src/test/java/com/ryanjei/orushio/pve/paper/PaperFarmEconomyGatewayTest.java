package com.ryanjei.orushio.pve.paper;

import com.ryanjei.orushio.pve.application.GameThreadExecutor;
import com.ryanjei.orushio.pve.domain.GameSession;
import com.ryanjei.orushio.pve.economy.ItemSpec;
import com.ryanjei.orushio.pve.map.BlockPoint;
import com.ryanjei.orushio.pve.map.GameRuntimeContextResolver;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaperFarmEconomyGatewayTest {
    @Test
    void WorldとInventory操作はGameThreadExecutor経由で直接実行しない() {
        RejectingExecutor executor = new RejectingExecutor();
        GameRuntimeContextResolver runtimes = ignored -> java.util.Optional.empty();
        PaperFarmEconomyGateway gateway = new PaperFarmEconomyGateway(executor, GameSession::idle, runtimes);
        UUID sessionId = UUID.randomUUID();

        assertThrows(IllegalStateException.class, () -> gateway.respawn(sessionId, "runtime", new BlockPoint(1, 2, 3, 0, 0), "IRON_ORE"));
        assertThrows(IllegalStateException.class, () -> gateway.giveItem(sessionId, UUID.randomUUID(), new ItemSpec("STONE", 1, Map.of(), false)));
        assertEquals(2, executor.calls);
    }

    private static final class RejectingExecutor implements GameThreadExecutor {
        private int calls;

        @Override
        public <T> T execute(Callable<T> task, Duration timeout) {
            calls++;
            throw new IllegalStateException("Paper main thread unavailable");
        }

        @Override
        public boolean isAcceptingTasks() {
            return false;
        }
    }
}
