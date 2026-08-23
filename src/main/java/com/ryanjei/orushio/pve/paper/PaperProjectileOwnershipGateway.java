package com.ryanjei.orushio.pve.paper;

import com.ryanjei.orushio.pve.application.GameThreadExecutor;
import com.ryanjei.orushio.pve.pve.ProjectileOwnershipGateway;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import java.time.Duration;
import java.util.*;

public final class PaperProjectileOwnershipGateway implements ProjectileOwnershipGateway {
    private final GameThreadExecutor executor;private final NamespacedKey ownedKey,sessionKey,zoneKey;
    public PaperProjectileOwnershipGateway(Plugin plugin,GameThreadExecutor executor){this.executor=executor;this.ownedKey=new NamespacedKey(plugin,"pve_enemy_owned");this.sessionKey=new NamespacedKey(plugin,"pve_enemy_session");this.zoneKey=new NamespacedKey(plugin,"pve_enemy_zone");}
    public boolean inheritOwnership(UUID projectileId,UUID shooterId){return execute(()->{Entity projectile=Bukkit.getEntity(projectileId),shooter=Bukkit.getEntity(shooterId);if(projectile==null||shooter==null||!projectile.getWorld().equals(shooter.getWorld()))return false;var source=shooter.getPersistentDataContainer();Byte owned=source.get(ownedKey,PersistentDataType.BYTE);String session=source.get(sessionKey,PersistentDataType.STRING),zone=source.get(zoneKey,PersistentDataType.STRING);if(owned==null||owned!=(byte)1||!validUuid(session)||!validUuid(zone))return false;var target=projectile.getPersistentDataContainer();target.set(ownedKey,PersistentDataType.BYTE,(byte)1);target.set(sessionKey,PersistentDataType.STRING,session);target.set(zoneKey,PersistentDataType.STRING,zone);return true;});}
    private static boolean validUuid(String value){if(value==null)return false;try{UUID.fromString(value);return true;}catch(IllegalArgumentException invalid){return false;}}
    private<T>T execute(java.util.concurrent.Callable<T>task){try{return executor.execute(task,Duration.ofSeconds(10));}catch(Exception e){throw new IllegalStateException("Projectile ownership操作に失敗しました。",e);}}
}
