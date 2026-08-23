package com.ryanjei.orushio.pve.core;
import com.ryanjei.orushio.pve.application.GameApplicationService;
import java.util.*;
import java.util.function.*;
/** Connects Core progress to the shared game lifecycle without coupling Core runtime to cleanup timing. */
public final class GameCoreProgressListener implements CoreProgressListener{
 private final BiConsumer<UUID,Integer> normalProgress;private final Supplier<? extends GameApplicationService> games;private final Consumer<Runnable> recoveryDispatcher;
 public GameCoreProgressListener(BiConsumer<UUID,Integer> normalProgress,Supplier<? extends GameApplicationService> games,Consumer<Runnable> recoveryDispatcher){this.normalProgress=Objects.requireNonNull(normalProgress);this.games=Objects.requireNonNull(games);this.recoveryDispatcher=Objects.requireNonNull(recoveryDispatcher);}
 public void normalCoreDestroyed(UUID sessionId,int destroyed,int required){normalProgress.accept(sessionId,destroyed);}
 public void finalCoreDestroyed(UUID sessionId){GameApplicationService game=Objects.requireNonNull(games.get(),"Game lifecycleが初期化されていません。");game.completeGame(sessionId);recoveryDispatcher.accept(()->game.recoverClearGame(sessionId));}
}
