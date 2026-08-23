package com.ryanjei.orushio.pve.paper;

import com.ryanjei.orushio.pve.application.GameThreadExecutor;
import com.ryanjei.orushio.pve.map.*;
import com.ryanjei.orushio.pve.progression.FinalAreaGateway;
import java.time.Duration;
import java.util.UUID;
import org.bukkit.*;

public final class PaperFinalAreaGateway implements FinalAreaGateway{
 private final GameThreadExecutor executor;private final GameRuntimeContextResolver runtimes;
 public PaperFinalAreaGateway(GameThreadExecutor executor,GameRuntimeContextResolver runtimes){this.executor=executor;this.runtimes=runtimes;}
 public void teleport(UUID sessionId,String runtimeWorld,UUID playerId,BlockPoint destination){execute(()->{requireRuntime(sessionId,runtimeWorld);World world=Bukkit.getWorld(runtimeWorld);var player=Bukkit.getPlayer(playerId);if(world==null)throw new IllegalStateException("Runtime Worldがありません。");if(player!=null&&!player.teleport(new Location(world,destination.x()+0.5,destination.y()+1.0,destination.z()+0.5,destination.yaw(),destination.pitch())))throw new IllegalStateException("Participantを転送できません。");return null;});}
 public void notifyLocked(UUID sessionId,String runtimeWorld,UUID playerId){execute(()->{requireRuntime(sessionId,runtimeWorld);var player=Bukkit.getPlayer(playerId);if(player!=null)player.sendMessage("[OPBP] 最終エリアはまだ解放されていません。");return null;});}
 private void requireRuntime(UUID sessionId,String world){GameRuntimeContext context=runtimes.resolve(sessionId).orElseThrow(()->new IllegalStateException("Runtime ownershipを確認できません。"));if(!context.worldName().equals(world))throw new IllegalStateException("Runtime World ownershipが一致しません。");}
 private<T>T execute(java.util.concurrent.Callable<T>task){try{return executor.execute(task,Duration.ofSeconds(5));}catch(Exception failure){throw new IllegalStateException("最終エリアのPaper操作に失敗しました。",failure);}}
}
