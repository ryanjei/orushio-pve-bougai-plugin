package com.ryanjei.orushio.pve.paper;

import static org.junit.jupiter.api.Assertions.*;
import com.ryanjei.orushio.pve.application.GameThreadExecutor;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class PaperShutdownControllerTest {
    @Test void PaperShutdownはGameThreadExecutor内で呼ばれる(){AtomicBoolean inside=new AtomicBoolean(),called=new AtomicBoolean();GameThreadExecutor executor=new GameThreadExecutor(){public<T>T execute(Callable<T> task,Duration timeout)throws Exception{inside.set(true);try{return task.call();}finally{inside.set(false);}}public boolean isAcceptingTasks(){return true;}};new PaperShutdownController(executor,()->{assertTrue(inside.get());called.set(true);}).request();assertTrue(called.get());}
}
