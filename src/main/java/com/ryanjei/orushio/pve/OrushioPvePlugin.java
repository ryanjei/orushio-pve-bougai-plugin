package com.ryanjei.orushio.pve;

import com.ryanjei.orushio.pve.application.*;
import com.ryanjei.orushio.pve.bootstrap.BootstrapHandoff;
import com.ryanjei.orushio.pve.bootstrap.LauncherShutdownHandoff;
import com.ryanjei.orushio.pve.bootstrap.RuntimeConfiguration;
import com.ryanjei.orushio.pve.bootstrap.StartupState;
import com.ryanjei.orushio.pve.domain.GameSession;
import com.ryanjei.orushio.pve.http.AdminHttpServer;
import com.ryanjei.orushio.pve.logging.AuditLog;
import com.ryanjei.orushio.pve.map.*;
import com.ryanjei.orushio.pve.paper.*;
import com.ryanjei.orushio.pve.persistence.*;
import com.ryanjei.orushio.pve.security.AuthService;
import com.ryanjei.orushio.pve.security.LauncherShutdownToken;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;

public final class OrushioPvePlugin extends JavaPlugin {
    private AdminHttpServer http;
    private PaperGameThreadExecutor gameThread;
    private BootstrapHandoff bootstrapHandoff;
    private LauncherShutdownHandoff shutdownHandoff;
    private BukkitTask lifecycleTimer;
    private ParticipantConnectionDispatcher participantConnections;

    @Override
    public void onEnable() {
        Path data = getDataFolder().toPath();
        try {
            Files.createDirectories(data);
            bootstrapHandoff = new BootstrapHandoff(data.resolve("admin-bootstrap.url"));
            bootstrapHandoff.clear();
            shutdownHandoff = new LauncherShutdownHandoff(data.resolve("launcher-shutdown.token"));
            shutdownHandoff.clear();

            StartupState startup = StartupState.load(data);
            RuntimeConfiguration config = startup.configuration();
            AuditLog audit = new AuditLog(data.resolve("logs"));
            gameThread = new PaperGameThreadExecutor(this);
            ServerAdministrationService serverAdministration = new DefaultServerAdministrationService(
                    new PaperServerAdministrationGateway(gameThread));

            Path mapsRoot = data.resolve("maps");
            MapProfileRepository mapProfiles = new YamlMapProfileRepository(mapsRoot);
            TemporaryWorldManager temporaryWorlds = new TemporaryWorldManager(
                    mapsRoot, getServer().getWorldContainer().toPath());
            String recoverySessionId=startup.recoverableGameSession()?startup.session().orElseThrow().sessionId().toString():"";
            temporaryWorlds.prepareStartupRecovery(recoverySessionId,!startup.sessionLoaded());
            java.util.concurrent.CompletableFuture.runAsync(temporaryWorlds::recoverPreparedWorlds)
                    .exceptionally(error -> {
                        getLogger().severe("起動時の一時ワールド回収に失敗しました。管理画面の診断情報を確認してください。");
                        return null;
                    });

            PaperMapWorldGateway mapWorlds = new PaperMapWorldGateway(this, gameThread);
            GameRuntimeLifecycleStep runtimeStep = new GameRuntimeLifecycleStep(
                    mapProfiles, temporaryWorlds, new PaperGameRuntimeGateway(gameThread), audit);
            DefaultGameApplicationService games = createGames(
                    data, startup, serverAdministration, mapProfiles, mapsRoot, audit, List.of(runtimeStep));
            MapAdministrationService maps = new DefaultMapAdministrationService(
                    mapsRoot, mapProfiles,
                    new SafeWorldZipImporter(mapsRoot, SafeWorldZipImporter.Limits.defaults()),
                    temporaryWorlds, mapWorlds, games, new MapSelectionService(new Random()), audit);
            getServer().getPluginManager().registerEvents(new MapSetupListener(maps, mapWorlds), this);
            if (!startup.diagnosticMode()) {
                participantConnections = new ParticipantConnectionDispatcher(games, failure ->
                        getLogger().log(Level.SEVERE,
                                "ゲーム参加者の接続状態を保存できませんでした。管理画面の診断情報を確認してください。"));
                getServer().getPluginManager().registerEvents(new GameLifecycleListener(participantConnections), this);
                lifecycleTimer = getServer().getScheduler().runTaskTimerAsynchronously(this,
                        () -> expireGameSafely(games), 20L, 20L);
            }

            ensureSecrets(data, config);
            final boolean[] bound = {false};
            LauncherShutdownToken shutdownToken = new LauncherShutdownToken();
            PaperShutdownController shutdownController = new PaperShutdownController(
                    gameThread, () -> getServer().shutdown());
            http = new AdminHttpServer(
                    InetAddress.getByName("127.0.0.1"), config.port(), games, serverAdministration, maps,
                    new AuthService(),
                    () -> diagnostics(startup, audit, bound[0], data, games, temporaryWorlds),
                    audit, ()->startup.diagnosticMode()||temporaryWorlds.recoveryRequired(),
                    ()->games.current().state()==com.ryanjei.orushio.pve.domain.GameState.RECOVERING,
                    shutdownToken, shutdownController::request);
            http.start();
            bound[0] = true;
            bootstrapHandoff.publish(config.port(), http.issueBootstrapToken());
            shutdownHandoff.publish(shutdownToken.issue());
            audit.record(config.traceId(), "SYSTEM",
                    startup.diagnosticMode() ? "DIAGNOSTIC_START" : "NORMAL_START", "PLUGIN_ENABLE");
            getLogger().info(startup.diagnosticMode()
                    ? "OPBPを診断モードで起動しました。"
                    : "OPBP基盤を起動しました。管理画面: http://127.0.0.1:" + config.port() + "/");
        } catch (Exception failure) {
            getLogger().severe("管理HTTPを安全に起動できないためプラグインを停止します。追跡ID: " + UUID.randomUUID());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private DefaultGameApplicationService createGames(
            Path data, StartupState startup, ServerAdministrationService serverAdministration,
            MapProfileRepository maps, Path mapsRoot, AuditLog audit,List<GameLifecycleStep> lifecycleSteps) {
        GameSession initial = startup.session().orElseGet(GameSession::idle);
        ActiveSessionRepository repository;
        if (startup.diagnosticMode()) {
            GameSession diagnosticSession = initial;
            repository = new ActiveSessionRepository() {
                @Override public Optional<GameSession> load() { return Optional.of(diagnosticSession); }
                @Override public void save(GameSession ignored) {
                    throw new RepositoryException("診断モードではゲーム状態を保存できません。");
                }
            };
        } else {
            repository = new YamlActiveSessionRepository(data.resolve("sessions/active.yml"));
            GameSession recovered = StartupState.recoverySessionAfterRestart(initial);
            if (recovered != initial) {
                repository.save(recovered);
                initial = recovered;
                audit.record(UUID.randomUUID().toString(), "SYSTEM", "GAME_RESTART_RECOVERY_REQUIRED", "PLUGIN_ENABLE");
            }
        }
        return new DefaultGameApplicationService(
                repository, initial, serverAdministration::onlinePlayers, maps,
                new YamlGameLaunchSettingsRepository(mapsRoot), lifecycleSteps, audit);
    }

    private void expireGameSafely(DefaultGameApplicationService games) {
        try {
            games.expireIfNeeded(Instant.now());
        } catch (RuntimeException failure) {
            getLogger().log(Level.SEVERE,
                    "ゲーム制限時間終了処理に失敗しました。管理画面の診断情報を確認してください。");
        }
    }

    private Map<String, Object> diagnostics(
            StartupState startup, AuditLog audit, boolean bound, Path data,
            DefaultGameApplicationService games, TemporaryWorldManager temporaryWorlds) {
        RuntimeConfiguration configuration = startup.configuration();
        boolean gameRecoveryRequired = games.current().state() == com.ryanjei.orushio.pve.domain.GameState.RECOVERING;
        List<String> warnings = new ArrayList<>(startup.recoverableGameSession()&&!gameRecoveryRequired
                ? startup.configuration().warnings() : startup.warnings());
        if (!audit.healthy()) warnings.add("監査ログへ書き込めません。");
        temporaryWorlds.startupRecoveryWarning().ifPresent(warnings::add);
        if (gameRecoveryRequired) warnings.add("ゲーム準備または清掃が完了していません。復旧清掃を再試行してください。");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paperRunning", true);
        result.put("pluginVersion", getPluginMeta().getVersion());
        result.put("httpBound", bound);
        result.put("storageReady", Files.isDirectory(data) && Files.isWritable(data));
        result.put("configLoaded", configuration.configLoaded());
        result.put("sessionLoaded", startup.sessionLoaded());
        result.put("recoveryRequired",
                (startup.recoveryRequired()&&!startup.recoverableGameSession()) || temporaryWorlds.startupRecoveryWarning().isPresent() || gameRecoveryRequired);
        result.put("temporaryWorldRecoveryComplete", temporaryWorlds.startupRecoveryComplete());
        result.put("diagnosticMode", startup.diagnosticMode()||temporaryWorlds.recoveryRequired());
        result.put("gameState", games.current().state().name());
        result.put("warnings", warnings);
        result.put("traceId", configuration.traceId());
        return result;
    }

    private void ensureSecrets(Path data, RuntimeConfiguration config) {
        if (config.diagnosticMode()) return;
        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        new YamlConfigRepository(data.resolve("secrets.yml")).loadOrCreate(Map.of(
                "schemaVersion", "1",
                "installationSecret", Base64.getEncoder().encodeToString(secret)));
    }

    @Override
    public void onDisable() {
        if (lifecycleTimer != null) {
            lifecycleTimer.cancel();
            lifecycleTimer = null;
        }
        if (participantConnections != null) {
            participantConnections.close();
            participantConnections = null;
        }
        if (bootstrapHandoff != null) {
            try { bootstrapHandoff.clear(); } catch (Exception ignored) { }
            bootstrapHandoff = null;
        }
        if (shutdownHandoff != null) {
            try { shutdownHandoff.clear(); } catch (Exception ignored) { }
            shutdownHandoff = null;
        }
        if (http != null) {
            http.close();
            http = null;
        }
        if (gameThread != null) {
            gameThread.close();
            gameThread = null;
        }
        getLogger().info("OPBP基盤を安全に停止しました。");
    }
}
