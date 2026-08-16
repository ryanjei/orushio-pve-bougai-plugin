package com.ryanjei.orushio.pve.paper;

import com.ryanjei.orushio.pve.map.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class MapSetupListener implements Listener {
    private final MapAdministrationService maps;private final PaperMapWorldGateway worlds;
    public MapSetupListener(MapAdministrationService maps,PaperMapWorldGateway worlds){this.maps=maps;this.worlds=worlds;}
    @EventHandler(ignoreCancelled=true) public void click(PlayerInteractEvent event){if(!worlds.isTool(event.getItem())||event.getClickedBlock()==null)return;var player=event.getPlayer();if(!player.isOp()||!player.hasPermission("orushio.pve.admin")){player.sendMessage("この操作を行う権限がありません。");event.setCancelled(true);return;}Action action=event.getAction();if(action!=Action.LEFT_CLICK_BLOCK&&action!=Action.RIGHT_CLICK_BLOCK)return;var block=event.getClickedBlock();try{var setup=maps.setup();boolean second=action==Action.RIGHT_CLICK_BLOCK;maps.recordClick(player.getUniqueId(),new BlockPoint(block.getX(),block.getY(),block.getZ(),player.getLocation().getYaw(),player.getLocation().getPitch()),second);MapSetupMessages.completed(setup.selectedField(),setup.area(),second).forEach(player::sendMessage);}catch(RuntimeException e){player.sendMessage(e.getMessage());}event.setCancelled(true);}
}
