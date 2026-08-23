package com.ryanjei.orushio.pve.paper;

import com.ryanjei.orushio.pve.application.GameThreadExecutor;
import com.ryanjei.orushio.pve.domain.GameSession;
import com.ryanjei.orushio.pve.map.*;
import com.ryanjei.orushio.pve.pve.*;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import static org.junit.jupiter.api.Assertions.*;

class PaperPveEnemyGatewayTest {
    @Test void Entity操作はすべてGameThreadExecutorを経由する(){RejectingExecutor executor=new RejectingExecutor();PaperPveEnemyGateway gateway=new PaperPveEnemyGateway(executor,GameSession::idle,id->Optional.empty(),bound->0);UUID session=UUID.randomUUID(),zone=UUID.randomUUID(),entity=UUID.randomUUID();EnemySpawnRequest request=new EnemySpawnRequest(session,"run",zone,new Cuboid(10,60,10,20,70,20),new Cuboid(0,60,0,5,70,5),EnemyType.ZOMBIE,2,30);assertThrows(IllegalStateException.class,()->gateway.spawn(request,16));assertThrows(IllegalStateException.class,()->gateway.cleanupOwned(session,"run"));assertThrows(IllegalStateException.class,()->gateway.removeOwnedInside(session,"run",request.farmRegion()));assertThrows(IllegalStateException.class,()->gateway.ownership(entity));assertEquals(4,executor.calls);}
    private static final class RejectingExecutor implements GameThreadExecutor{int calls;public<T>T execute(Callable<T>task,Duration timeout){calls++;throw new IllegalStateException("Paper main thread unavailable");}public boolean isAcceptingTasks(){return false;}}
}
