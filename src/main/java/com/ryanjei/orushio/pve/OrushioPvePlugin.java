package com.ryanjei.orushio.pve;

import com.ryanjei.orushio.pve.application.*;
import com.ryanjei.orushio.pve.bootstrap.RuntimeConfiguration;
import com.ryanjei.orushio.pve.domain.GameSession;
import com.ryanjei.orushio.pve.http.AdminHttpServer;
import com.ryanjei.orushio.pve.logging.AuditLog;
import com.ryanjei.orushio.pve.paper.*;
import com.ryanjei.orushio.pve.persistence.*;
import com.ryanjei.orushio.pve.security.AuthService;
import org.bukkit.plugin.java.JavaPlugin;
import java.net.InetAddress;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;

public final class OrushioPvePlugin extends JavaPlugin {
    private AdminHttpServer http; private PaperGameThreadExecutor gameThread;
    @Override public void onEnable(){Path data=getDataFolder().toPath();try{Files.createDirectories(data);RuntimeConfiguration config=RuntimeConfiguration.load(data);AuditLog audit=new AuditLog(data.resolve("logs"));DefaultGameApplicationService games=createGames(data,config);gameThread=new PaperGameThreadExecutor(this);ensureSecrets(data,config);final boolean[] bound={false};http=new AdminHttpServer(InetAddress.getByName("127.0.0.1"),config.port(),games,new PaperPlayerQuery(gameThread),new AuthService(),()->diagnostics(config,audit,bound[0],data,games),audit,config.diagnosticMode());http.start();bound[0]=true;audit.record(config.traceId(),"SYSTEM",config.diagnosticMode()?"DIAGNOSTIC_START":"NORMAL_START","PLUGIN_ENABLE");getLogger().info(config.diagnosticMode()?"Orushio PVEを診断モードで起動しました。":"Orushio PVE基盤を起動しました。管理画面: http://127.0.0.1:"+config.port()+"/");}catch(Exception e){getLogger().severe("管理HTTPを安全に起動できないためプラグインを停止します。追跡ID: "+UUID.randomUUID());getServer().getPluginManager().disablePlugin(this);}}
    private DefaultGameApplicationService createGames(Path data,RuntimeConfiguration config){if(!config.diagnosticMode())try{return new DefaultGameApplicationService(new YamlActiveSessionRepository(data.resolve("sessions/active.yml")));}catch(Exception ignored){}return new DefaultGameApplicationService(new ActiveSessionRepository(){GameSession value=GameSession.idle();public Optional<GameSession> load(){return Optional.of(value);}public void save(GameSession s){if(config.diagnosticMode())throw new RepositoryException("診断モードでは保存できません。");value=s;}});}
    private Map<String,Object> diagnostics(RuntimeConfiguration c,AuditLog audit,boolean bound,Path data,DefaultGameApplicationService games){List<String>w=new ArrayList<>(c.warnings());if(!audit.healthy())w.add("監査ログへ書き込めません。");Map<String,Object> m=new LinkedHashMap<>();m.put("paperRunning",true);m.put("pluginVersion",getPluginMeta().getVersion());m.put("httpBound",bound);m.put("storageReady",Files.isDirectory(data)&&Files.isWritable(data));m.put("configLoaded",c.configLoaded());m.put("diagnosticMode",c.diagnosticMode());m.put("gameState",games.current().state().name());m.put("warnings",w);m.put("traceId",c.traceId());return m;}
    private void ensureSecrets(Path data,RuntimeConfiguration config){if(config.diagnosticMode())return;byte[] secret=new byte[32];new SecureRandom().nextBytes(secret);new YamlConfigRepository(data.resolve("secrets.yml")).loadOrCreate(Map.of("schemaVersion","1","installationSecret",Base64.getEncoder().encodeToString(secret)));}
    @Override public void onDisable(){if(http!=null){http.close();http=null;}if(gameThread!=null){gameThread.close();gameThread=null;}getLogger().info("Orushio PVE基盤を安全に停止しました。");}
}
