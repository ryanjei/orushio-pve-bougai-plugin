package com.ryanjei.orushio.pve.paper;
import com.ryanjei.orushio.pve.application.*;
import com.ryanjei.orushio.pve.domain.GameSession;
import org.bukkit.Bukkit;
import java.time.Duration;
public final class PaperClearPresentationGateway implements ClearPresentationGateway{
 private final GameThreadExecutor executor;
 public PaperClearPresentationGateway(GameThreadExecutor executor){this.executor=executor;}
 public void showClear(GameSession session){try{executor.execute(()->{for(var participant:session.participants()){var player=Bukkit.getPlayer(participant.playerUuid());if(player!=null&&player.isOnline())player.sendTitle("CLEAR","攻略完了",10,50,10);}return null;},Duration.ofSeconds(10));}catch(Exception failure){throw new IllegalStateException("CLEAR表示に失敗しました。",failure);}}
}
