package com.ryanjei.orushio.pve.paper;

import com.ryanjei.orushio.pve.application.*;
import org.bukkit.*;import org.bukkit.inventory.ItemStack;
import java.time.Duration;import java.util.*;

public final class PaperPlayerInventoryGateway implements PlayerInventoryGateway {
    private final GameThreadExecutor executor;
    public PaperPlayerInventoryGateway(GameThreadExecutor executor){this.executor=Objects.requireNonNull(executor);}
    public Optional<RollbackState> initializeIfOnline(UUID playerId){return execute(()->{var player=Bukkit.getPlayer(playerId);if(player==null||!player.isOnline())return Optional.empty();var inventory=player.getInventory();State state=new State(cloneItems(inventory.getStorageContents()),cloneItems(inventory.getArmorContents()),cloneItem(inventory.getItemInOffHand()));inventory.setStorageContents(new ItemStack[inventory.getStorageContents().length]);inventory.setArmorContents(new ItemStack[inventory.getArmorContents().length]);inventory.setItemInOffHand(null);return Optional.of(state);});}
    public void rollback(UUID playerId,RollbackState rollback){execute(()->{if(!(rollback instanceof State state))throw new IllegalArgumentException("rollback状態が不正です。");var player=Bukkit.getPlayer(playerId);if(player==null||!player.isOnline())throw new IllegalStateException("rollback対象Participantがofflineです。");var inventory=player.getInventory();inventory.setStorageContents(cloneItems(state.storage));inventory.setArmorContents(cloneItems(state.armor));inventory.setItemInOffHand(cloneItem(state.offhand));return null;});}
    public CleanupResult returnToLobbyAndCleanup(UUID playerId){return execute(()->{var player=Bukkit.getPlayer(playerId);if(player==null||!player.isOnline())return CleanupResult.OFFLINE;World lobby=Bukkit.getWorlds().stream().filter(world->!world.getName().startsWith("orushio_run_")&&!world.getName().startsWith("orushio_setup_")).findFirst().orElseThrow(()->new IllegalStateException("ロビーworldがありません。"));if(!player.teleport(lobby.getSpawnLocation()))throw new IllegalStateException("Participantをロビーへ戻せません。");var inventory=player.getInventory();inventory.setStorageContents(new ItemStack[inventory.getStorageContents().length]);inventory.setArmorContents(new ItemStack[inventory.getArmorContents().length]);inventory.setItemInOffHand(null);return CleanupResult.CLEANED;});}
    private<T>T execute(java.util.concurrent.Callable<T> task){try{return executor.execute(task,Duration.ofSeconds(10));}catch(Exception failure){throw new IllegalStateException("Paper Player Inventory操作に失敗しました。",failure);}}
    private static ItemStack[] cloneItems(ItemStack[] values){ItemStack[] copy=new ItemStack[values.length];for(int i=0;i<values.length;i++)copy[i]=cloneItem(values[i]);return copy;}
    private static ItemStack cloneItem(ItemStack value){return value==null?null:value.clone();}
    private record State(ItemStack[] storage,ItemStack[] armor,ItemStack offhand) implements RollbackState {}
}
