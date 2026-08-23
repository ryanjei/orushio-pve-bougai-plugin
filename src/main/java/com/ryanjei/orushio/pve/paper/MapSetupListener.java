package com.ryanjei.orushio.pve.paper;

import com.ryanjei.orushio.pve.map.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public final class MapSetupListener implements Listener {
    private final MapAdministrationService maps;private final PaperMapWorldGateway worlds;
    public MapSetupListener(MapAdministrationService maps,PaperMapWorldGateway worlds){this.maps=maps;this.worlds=worlds;}
    @EventHandler public void join(PlayerJoinEvent event){var setup=maps.setup();if(SetupToolCleanup.shouldCleanupOnJoin(setup.active(),setup.administrator(),event.getPlayer().getUniqueId().toString()))worlds.cleanupOwnedTools(event.getPlayer());}
    @EventHandler(ignoreCancelled=true) public void click(PlayerInteractEvent event){if(!worlds.isTool(event.getItem())||event.getClickedBlock()==null)return;var player=event.getPlayer();if(!player.isOp()||!player.hasPermission("orushio.pve.admin")){player.sendMessage("この操作を行う権限がありません。");event.setCancelled(true);return;}Action action=event.getAction();if(action!=Action.LEFT_CLICK_BLOCK&&action!=Action.RIGHT_CLICK_BLOCK)return;var block=event.getClickedBlock();try{var setup=maps.setup();boolean second=action==Action.RIGHT_CLICK_BLOCK;BlockPoint point=new BlockPoint(block.getX(),block.getY(),block.getZ(),player.getLocation().getYaw(),player.getLocation().getPitch());maps.recordClick(player.getUniqueId(),point,second);MapSetupMessages.completed(setup.selectedField(),setup.area(),second,point).forEach(player::sendMessage);}catch(RuntimeException e){player.sendMessage("[OPBP] "+e.getMessage());}event.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void place(BlockPlaceEvent event){var token=worlds.setupMarkerToken(event.getItemInHand());if(token.isEmpty())return;var player=event.getPlayer();try{var setup=maps.setup();String worldName=event.getBlock().getWorld().getName();if(!setup.active()||!setup.administrator().equals(player.getUniqueId().toString())||!maps.ownsSetupWorld(player.getUniqueId(),worldName))throw new SecurityException("この設定ブロックを使用する権限がありません。");var block=event.getBlockPlaced();maps.recordSetupMarker(player.getUniqueId(),token.get(),new BlockPoint(block.getX(),block.getY(),block.getZ(),player.getLocation().getYaw(),player.getLocation().getPitch()));worlds.retainReusableMarkerBlock(player,event.getItemInHand());}catch(RuntimeException failure){event.setCancelled(true);player.sendMessage("[OPBP] "+failure.getMessage());}}
    @EventHandler(ignoreCancelled=true) public void breakMarker(BlockBreakEvent event){var player=event.getPlayer();var setup=maps.setup();if(!setup.active()||!setup.administrator().equals(player.getUniqueId().toString()))return;try{if(!maps.ownsSetupWorld(player.getUniqueId(),event.getBlock().getWorld().getName()))return;var block=event.getBlock();var marker=setup.setupMarkerTypes().stream().flatMap(type->type.markers().stream()).filter(value->value.position().x()==block.getX()&&value.position().y()==block.getY()&&value.position().z()==block.getZ()).findFirst().orElse(null);if(marker==null||block.getType()!=PaperMapWorldGateway.material(marker.markerType()))return;maps.removeSetupMarkerAt(player.getUniqueId(),new BlockPoint(block.getX(),block.getY(),block.getZ(),0,0));}catch(RuntimeException failure){event.setCancelled(true);player.sendMessage("[OPBP] "+failure.getMessage());}}
}
