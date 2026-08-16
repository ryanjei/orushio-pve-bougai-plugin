package com.ryanjei.orushio.pve;

import com.ryanjei.orushio.pve.application.*;
import com.ryanjei.orushio.pve.bootstrap.RuntimeConfiguration;
import com.ryanjei.orushio.pve.bootstrap.StartupState;
import com.ryanjei.orushio.pve.domain.GameSession;
import com.ryanjei.orushio.pve.http.AdminHttpServer;
import com.ryanjei.orushio.pve.logging.AuditLog;
import com.ryanjei.orushio.pve.map.*;
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
    @Override public void onEnable(){Path data=getDataFolder().toPath();try{Files.createDirectories(data);StartupState startup=StartupState.load(data);RuntimeConfiguration config=startup.configuration();AuditLog audit=new AuditLog(data.resolve("logs"));DefaultGameApplicationService games=createGames(data,startup);gameThread=new PaperGameThreadExecutor(this);ServerAdministrationService serverAdministration=new DefaultServerAdministrationService(new PaperServerAdministrationGateway(gameThread));Path mapsRoot=data.resolve("maps");TemporaryWorldManager temporaryWorlds=new TemporaryWorldManager(mapsRoot,getServer().getWorldContainer().toPath());java.util.concurrent.CompletableFuture.runAsync(temporaryWorlds::recoverOwnedWorlds);PaperMapWorldGateway mapWorlds=new PaperMapWorldGateway(this,gameThread);MapAdministrationService maps=new DefaultMapAdministrationService(mapsRoot,new YamlMapProfileRepository(mapsRoot),new SafeWorldZipImporter(mapsRoot,SafeWorldZipImporter.Limits.defaults()),temporaryWorlds,mapWorlds,games,new MapSelectionService(new Random()));getServer().getPluginManager().registerEvents(new MapSetupListener(maps,mapWorlds),this);ensureSecrets(data,config);final boolean[] bound={false};http=new AdminHttpServer(InetAddress.getByName("127.0.0.1"),config.port(),games,serverAdministration,maps,new AuthService(),()->diagnostics(startup,audit,bound[0],data,games),audit,startup.diagnosticMode());http.start();bound[0]=true;audit.record(config.traceId(),"SYSTEM",startup.diagnosticMode()?"DIAGNOSTIC_START":"NORMAL_START","PLUGIN_ENABLE");getLogger().info(startup.diagnosticMode()?"Orushio PVEを診断モードで起動しました。":"Orushio PVE基盤を起動しました。管理画面: http://127.0.0.1:"+config.port()+"/");}catch(Exception e){getLogger().severe("管理HTTPを安全に起動できないためプラグインを停止します。追跡ID: "+UUID.randomUUID());getServer().getPluginManager().disablePlugin(this);}}
    private DefaultGameApplicationService createGames(Path data,StartupState startup){GameSession initial=startup.session().orElseGet(GameSession::idle);if(!startup.diagnosticMode())return new DefaultGameApplicationService(new YamlActiveSessionRepository(data.resolve("sessions/active.yml")),initial);return new DefaultGameApplicationService(new ActiveSessionRepository(){public Optional<GameSession> load(){return Optional.of(initial);}public void save(GameSession ignored){throw new RepositoryException("診断モードではゲーム状態を保存できません。");}},initial);}
    private Map<String,Object> diagnostics(StartupState startup,AuditLog audit,boolean bound,Path data,DefaultGameApplicationService games){RuntimeConfiguration c=startup.configuration();List<String>w=new ArrayList<>(startup.warnings());if(!audit.healthy())w.add("監査ログへ書き込めません。");Map<String,Object> m=new LinkedHashMap<>();m.put("paperRunning",true);m.put("pluginVersion",getPluginMeta().getVersion());m.put("httpBound",bound);m.put("storageReady",Files.isDirectory(data)&&Files.isWritable(data));m.put("configLoaded",c.configLoaded());m.put("sessionLoaded",startup.sessionLoaded());m.put("recoveryRequired",startup.recoveryRequired());m.put("diagnosticMode",startup.diagnosticMode());m.put("gameState",games.current().state().name());m.put("warnings",w);m.put("traceId",c.traceId());return m;}
    private void ensureSecrets(Path data,RuntimeConfiguration config){if(config.diagnosticMode())return;byte[] secret=new byte[32];new SecureRandom().nextBytes(secret);new YamlConfigRepository(data.resolve("secrets.yml")).loadOrCreate(Map.of("schemaVersion","1","installationSecret",Base64.getEncoder().encodeToString(secret)));}
    @Override public void onDisable(){if(http!=null){http.close();http=null;}if(gameThread!=null){gameThread.close();gameThread=null;}getLogger().info("Orushio PVE基盤を安全に停止しました。");}
}
