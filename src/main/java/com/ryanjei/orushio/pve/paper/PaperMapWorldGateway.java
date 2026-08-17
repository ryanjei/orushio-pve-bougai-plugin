package com.ryanjei.orushio.pve.paper;

import com.ryanjei.orushio.pve.application.GameThreadExecutor;
import com.ryanjei.orushio.pve.map.*;
import java.time.Duration;
import java.util.UUID;
import java.util.Arrays;
import org.bukkit.*;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class PaperMapWorldGateway implements MapWorldGateway {
    public static final String TOOL_KEY="setup_tool";
    private final GameThreadExecutor executor;private final NamespacedKey key;
    public PaperMapWorldGateway(Plugin plugin,GameThreadExecutor executor){this.executor=executor;key=new NamespacedKey(plugin,TOOL_KEY);}
    @Override public void loadForSetup(TemporaryWorldManager.OwnedWorld owned,UUID administrator){execute(()->{var player=Bukkit.getPlayer(administrator);validateAdministrator(player!=null,player!=null&&player.isOp(),player!=null&&player.hasPermission("orushio.pve.admin"));World world=null;try{world=Bukkit.createWorld(new WorldCreator(owned.worldName()));if(world==null)throw new IllegalStateException("セットアップワールドをロードできません。");ItemStack tool=new ItemStack(Material.BLAZE_ROD);var meta=tool.getItemMeta();meta.setDisplayName("マップ設定ツール");meta.getPersistentDataContainer().set(key,PersistentDataType.BYTE,(byte)1);tool.setItemMeta(meta);player.getInventory().addItem(tool);if(!player.teleport(world.getSpawnLocation()))throw new IllegalStateException("管理者をセットアップワールドへ転送できません。");return null;}catch(RuntimeException failure){if(world!=null)try{rollbackLoadedWorld(world);}catch(RuntimeException rollback){failure.addSuppressed(rollback);}throw failure;}});}
    @Override public void unload(TemporaryWorldManager.OwnedWorld owned){execute(()->{World world=Bukkit.getWorld(owned.worldName());if(world!=null)rollbackLoadedWorld(world);return null;});}
    @Override public void completeSetup(TemporaryWorldManager.OwnedWorld owned,UUID administrator,String message){execute(()->{World world=Bukkit.getWorld(owned.worldName());var player=Bukkit.getPlayer(administrator);if(player!=null){removeOwnedTools(player.getInventory());player.sendMessage(message);}if(world!=null)rollbackLoadedWorld(world);return null;});}
    static void removeOwnedTools(PlayerInventory inventory,NamespacedKey key){SetupToolCleanup.ownedSlots(Arrays.asList(inventory.getContents()),item->isOwnedTool(item,key)).forEach(slot->inventory.setItem(slot,null));}
    private void removeOwnedTools(PlayerInventory inventory){removeOwnedTools(inventory,key);}
    static boolean isOwnedTool(ItemStack item,NamespacedKey key){return item!=null&&item.hasItemMeta()&&item.getItemMeta().getPersistentDataContainer().has(key,PersistentDataType.BYTE);}
    static void validateAdministrator(boolean online,boolean op,boolean permitted){if(!online||!op||!permitted)throw new SecurityException("オンラインの許可済み管理者が必要です。");}
    private static void rollbackLoadedWorld(World world){World fallback=Bukkit.getWorlds().stream().filter(w->!w.equals(world)).findFirst().orElseThrow(()->new IllegalStateException("帰還先ワールドがありません。"));for(var player:world.getPlayers())player.teleport(fallback.getSpawnLocation());if(!Bukkit.unloadWorld(world,false))throw new IllegalStateException("一時ワールドをアンロードできません。");}
    public boolean isTool(ItemStack item){return isOwnedTool(item,key);}
    void cleanupOwnedTools(org.bukkit.entity.Player player){removeOwnedTools(player.getInventory());}
    private<T>T execute(java.util.concurrent.Callable<T> task){try{return executor.execute(task,Duration.ofSeconds(5));}catch(Exception e){throw new MapIoException("PAPER_WORLD","Paperワールド操作に失敗しました。",e);}}
}
