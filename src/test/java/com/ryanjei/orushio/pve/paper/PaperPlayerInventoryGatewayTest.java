package com.ryanjei.orushio.pve.paper;

import static org.junit.jupiter.api.Assertions.*;import com.ryanjei.orushio.pve.application.*;import java.time.Duration;import java.util.UUID;import java.util.concurrent.Callable;import org.junit.jupiter.api.Test;
class PaperPlayerInventoryGatewayTest {
 @Test void 全Player操作はGameThreadExecutor経由で直接実行しない(){RejectingExecutor executor=new RejectingExecutor();PaperPlayerInventoryGateway gateway=new PaperPlayerInventoryGateway(executor);UUID player=UUID.randomUUID();assertThrows(IllegalStateException.class,()->gateway.initializeIfOnline(player));assertThrows(IllegalStateException.class,()->gateway.rollback(player,new PlayerInventoryGateway.RollbackState(){}));assertThrows(IllegalStateException.class,()->gateway.returnToLobbyAndCleanup(player));assertEquals(3,executor.calls);}
 private static final class RejectingExecutor implements GameThreadExecutor{int calls;public<T>T execute(Callable<T> task,Duration timeout){calls++;throw new IllegalStateException("Paper main thread unavailable");}public boolean isAcceptingTasks(){return false;}}
}
