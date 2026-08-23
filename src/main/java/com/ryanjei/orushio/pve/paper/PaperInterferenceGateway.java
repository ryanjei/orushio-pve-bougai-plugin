package com.ryanjei.orushio.pve.paper;

import com.ryanjei.orushio.pve.application.GameThreadExecutor;
import com.ryanjei.orushio.pve.domain.*;
import com.ryanjei.orushio.pve.interference.*;
import com.ryanjei.orushio.pve.map.Cuboid;
import com.ryanjei.orushio.pve.pve.RandomSource;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.*;

public final class PaperInterferenceGateway implements InterferenceGateway {
    interface PlayerAccess{Target find(UUID playerId);}interface Target{boolean online();String worldName();int blockX();int blockY();int blockZ();EffectState potionEffect(InterferenceType type);void potionEffect(InterferenceType type,int ticks,int amplifier);Object[]hotbar();void hotbar(Object[]items);}
    record EffectState(int durationTicks,int amplifier){}
    private final GameThreadExecutor thread;private final Supplier<GameSession> games;private final RandomSource random;private final PlayerAccess players;
    public PaperInterferenceGateway(GameThreadExecutor thread,Supplier<GameSession> games,RandomSource random){this(thread,games,random,id->{Player player=Bukkit.getPlayer(id);return player==null?null:new PaperTarget(player);});}
    PaperInterferenceGateway(GameThreadExecutor thread,Supplier<GameSession> games,RandomSource random,PlayerAccess players){this.thread=Objects.requireNonNull(thread);this.games=Objects.requireNonNull(games);this.random=Objects.requireNonNull(random);this.players=Objects.requireNonNull(players);}
    public ExecutionResult apply(UUID sessionId,String runtimeWorld,Cuboid finalRegion,List<UUID> participants,InterferenceType type,InterferenceSettings settings){try{return thread.execute(()->applyOnGameThread(sessionId,runtimeWorld,finalRegion,participants,type,settings),Duration.ofSeconds(5));}catch(RuntimeException failure){throw failure;}catch(Exception failure){throw new IllegalStateException("妨害をゲームスレッドで実行できません。",failure);}}
    private ExecutionResult applyOnGameThread(UUID sessionId,String runtimeWorld,Cuboid finalRegion,List<UUID> participants,InterferenceType type,InterferenceSettings settings){GameSession current=games.get();if(current.state()!=GameState.ACTIVE||!current.sessionId().equals(sessionId))throw new IllegalStateException("妨害対象Sessionが変化しました。");int affected=0,skipped=0;for(UUID participant:participants){if(!current.isParticipant(participant)){skipped++;continue;}Target player=players.find(participant);if(player==null||!player.online()||!player.worldName().equals(runtimeWorld)||!contains(finalRegion,player)){skipped++;continue;}switch(type){case DARKNESS->applyPotionEffectWithoutWeakening(player,type,ticks(settings.darknessDuration()),0);case LEVITATION->applyPotionEffectWithoutWeakening(player,type,ticks(settings.levitationDuration()),settings.levitationAmplifier());case HOTBAR_SHUFFLE->shuffle(player);}affected++;}return new ExecutionResult(affected,skipped);}
    private static void applyPotionEffectWithoutWeakening(Target player,InterferenceType type,int ticks,int amplifier){EffectState existing=player.potionEffect(type);if(existing!=null&&(existing.durationTicks()>ticks||existing.amplifier()>amplifier))return;if(existing==null||existing.durationTicks()!=ticks||existing.amplifier()!=amplifier)player.potionEffect(type,ticks,amplifier);}
    private void shuffle(Target player){Object[]items=player.hotbar();if(items.length!=9)throw new IllegalStateException("Hotbar slot数が不正です。");permute(items,random);player.hotbar(items);}
    static <T> void permute(T[]items,RandomSource random){for(int i=items.length-1;i>0;i--){int j=random.nextInt(i+1);if(j<0||j>i)throw new IllegalStateException("乱数値が範囲外です。");T swap=items[i];items[i]=items[j];items[j]=swap;}}
    private static boolean contains(Cuboid region,Target player){return player.blockX()>=region.minX()&&player.blockX()<=region.maxX()&&player.blockY()>=region.minY()&&player.blockY()<=region.maxY()&&player.blockZ()>=region.minZ()&&player.blockZ()<=region.maxZ();}
    private static int ticks(Duration duration){return Math.toIntExact(duration.toMillis()/50);}
    private record PaperTarget(Player player)implements Target{public boolean online(){return player.isOnline();}public String worldName(){return player.getWorld().getName();}public int blockX(){return player.getLocation().getBlockX();}public int blockY(){return player.getLocation().getBlockY();}public int blockZ(){return player.getLocation().getBlockZ();}public EffectState potionEffect(InterferenceType type){PotionEffect effect=player.getPotionEffect(potionType(type));return effect==null?null:new EffectState(effect.getDuration(),effect.getAmplifier());}public void potionEffect(InterferenceType type,int ticks,int amplifier){player.addPotionEffect(new PotionEffect(potionType(type),ticks,amplifier,false,true,true),true);}public Object[]hotbar(){ItemStack[]items=new ItemStack[9];for(int i=0;i<items.length;i++)items[i]=player.getInventory().getItem(i);return items;}public void hotbar(Object[]items){for(int i=0;i<items.length;i++)player.getInventory().setItem(i,(ItemStack)items[i]);}}
    private static PotionEffectType potionType(InterferenceType type){return switch(type){case DARKNESS->PotionEffectType.DARKNESS;case LEVITATION->PotionEffectType.LEVITATION;case HOTBAR_SHUFFLE->throw new IllegalArgumentException("Potion Effectではない妨害種別です。");};}
}
