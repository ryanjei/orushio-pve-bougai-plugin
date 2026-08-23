package com.ryanjei.orushio.pve.paper;

import com.ryanjei.orushio.pve.application.GameApplicationService;
import com.ryanjei.orushio.pve.progression.FinalAreaProgressionStep;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerMoveEvent;

public final class FinalAreaListener implements Listener{
 private final GameApplicationService games;private final FinalAreaProgressionStep progression;
 public FinalAreaListener(GameApplicationService games,FinalAreaProgressionStep progression){this.games=games;this.progression=progression;}
 @EventHandler(ignoreCancelled=true)public void move(PlayerMoveEvent event){if(event.getTo()==null)return;var from=event.getFrom();var to=event.getTo();if(from.getBlockX()==to.getBlockX()&&from.getBlockY()==to.getBlockY()&&from.getBlockZ()==to.getBlockZ()&&from.getWorld().equals(to.getWorld()))return;progression.contact(games.current(),event.getPlayer().getUniqueId(),to.getWorld().getName(),to.getBlockX(),to.getBlockY()-1,to.getBlockZ());}
}
