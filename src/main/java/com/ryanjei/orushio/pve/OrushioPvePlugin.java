package com.ryanjei.orushio.pve;

import com.ryanjei.orushio.pve.application.DefaultGameApplicationService;
import com.ryanjei.orushio.pve.http.AdminHttpServer;
import com.ryanjei.orushio.pve.logging.AuditLog;
import com.ryanjei.orushio.pve.paper.PaperGameThreadExecutor;
import com.ryanjei.orushio.pve.paper.PaperPlayerQuery;
import com.ryanjei.orushio.pve.persistence.YamlActiveSessionRepository;
import com.ryanjei.orushio.pve.persistence.YamlConfigRepository;
import com.ryanjei.orushio.pve.security.AuthService;
import org.bukkit.plugin.java.JavaPlugin;
import java.net.InetAddress;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public final class OrushioPvePlugin extends JavaPlugin {
    private AdminHttpServer http;
    private PaperGameThreadExecutor gameThread;

    @Override public void onEnable() {
        try {
            Files.createDirectories(getDataFolder().toPath());
            var systemRepo = new YamlConfigRepository(getDataFolder().toPath().resolve("config.yml"));
            Map<String,String> system = systemRepo.loadOrCreate(defaultSystemConfig());
            new YamlConfigRepository(getDataFolder().toPath().resolve("game-config.yml")).loadOrCreate(defaultGameConfig());
            ensureSecrets();
            var games = new DefaultGameApplicationService(new YamlActiveSessionRepository(getDataFolder().toPath().resolve("sessions/active.yml")));
            gameThread = new PaperGameThreadExecutor(this);
            var audit = new AuditLog(getDataFolder().toPath().resolve("logs"));
            int port = Integer.parseInt(system.get("httpPort"));
            http = new AdminHttpServer(InetAddress.getByName("127.0.0.1"), port, games, new PaperPlayerQuery(gameThread), new AuthService(), () -> Map.of(
                "paperRunning", true,
                "pluginVersion", getPluginMeta().getVersion(),
                "httpBound", http != null,
                "storageReady", Files.isWritable(getDataFolder().toPath()),
                "configLoaded", true,
                "warnings", java.util.List.of()
            ), message -> audit.record("ADMIN_OPERATION", message));
            http.start();
            audit.record("SYSTEM", "管理HTTPサーバーを起動しました。");
            getLogger().info("Orushio PVE基盤を起動しました。管理画面: http://127.0.0.1:" + port + "/");
        } catch (Exception e) {
            getLogger().severe("Orushio PVEの起動に失敗しました。診断情報: " + e.getClass().getSimpleName());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override public void onDisable() {
        if (http != null) { http.close(); http = null; }
        if (gameThread != null) { gameThread.close(); gameThread = null; }
        getLogger().info("Orushio PVE基盤を安全に停止しました。");
    }

    private void ensureSecrets() {
        var repo = new YamlConfigRepository(getDataFolder().toPath().resolve("secrets.yml"));
        byte[] secret = new byte[32]; new SecureRandom().nextBytes(secret);
        repo.loadOrCreate(Map.of("schemaVersion", "1", "installationSecret", Base64.getEncoder().encodeToString(secret)));
    }
    private static Map<String,String> defaultSystemConfig() { Map<String,String> v = new LinkedHashMap<>(); v.put("schemaVersion", "1"); v.put("httpHost", "127.0.0.1"); v.put("httpPort", "8765"); return v; }
    private static Map<String,String> defaultGameConfig() { return Map.of("schemaVersion", "1", "maxParticipants", "4"); }
}
