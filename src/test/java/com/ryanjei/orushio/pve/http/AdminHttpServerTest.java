package com.ryanjei.orushio.pve.http;

import static org.junit.jupiter.api.Assertions.*;
import com.ryanjei.orushio.pve.application.DefaultGameApplicationService;
import com.ryanjei.orushio.pve.domain.GameSession;
import com.ryanjei.orushio.pve.persistence.ActiveSessionRepository;
import com.ryanjei.orushio.pve.security.AuthService;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdminHttpServerTest {
    private AdminHttpServer server;
    private HttpClient client;
    private String base;
    private String cookie;
    private String csrf;

    @BeforeEach void start() throws Exception {
        var games = new DefaultGameApplicationService(new MemoryRepository());
        server = new AdminHttpServer(InetAddress.getByName("127.0.0.1"), 0, games, List::of, new AuthService(), () -> Map.of("paperRunning", true));
        server.start(); client = HttpClient.newHttpClient(); base = "http://127.0.0.1:" + server.port();
        String token = server.issueBootstrapToken();
        var response = client.send(HttpRequest.newBuilder(URI.create(base + "/auth/bootstrap")).header("X-Bootstrap-Token", token).POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        cookie = response.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
        csrf = extract(response.body(), "csrfToken");
    }
    @AfterEach void stop() { server.close(); }

    @Test void 未認証mutationを拒否する() throws Exception {
        var response = sendMutation(false, csrf, "IDLE");
        assertEquals(401, response.statusCode()); assertTrue(response.body().contains("AUTH_REQUIRED"));
    }
    @Test void OriginとCsrfを検証する() throws Exception {
        var response = sendMutation(true, "wrong", "IDLE");
        assertEquals(403, response.statusCode()); assertTrue(response.body().contains("FORBIDDEN"));
    }
    @Test void 正常な受付開始と古い状態の競合を区別する() throws Exception {
        assertEquals(200, sendMutation(true, csrf, "IDLE").statusCode());
        var conflict = sendMutation(true, csrf, "IDLE");
        assertEquals(409, conflict.statusCode()); assertTrue(conflict.body().contains("GAME_STATE_CONFLICT"));
    }
    @Test void bootstrapTokenは一回だけ交換できる() throws Exception {
        String token = server.issueBootstrapToken();
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/auth/bootstrap")).header("X-Bootstrap-Token", token).POST(HttpRequest.BodyPublishers.noBody()).build();
        assertEquals(200, client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
        assertEquals(401, client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
    }
    @Test void 認証済み画面はCsrfTokenを再取得できる() throws Exception {
        var request = HttpRequest.newBuilder(URI.create(base + "/api/v1/auth/session")).header("Cookie", cookie).GET().build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("csrfToken"));
    }
    private HttpResponse<String> sendMutation(boolean authenticated, String csrfValue, String state) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(base + "/api/v1/game/recruiting/start")).header("Origin", base).header("X-CSRF-Token", csrfValue).header("If-Game-State", state).POST(HttpRequest.BodyPublishers.noBody());
        if (authenticated) builder.header("Cookie", cookie);
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
    private static String extract(String json, String key) { String marker = "\"" + key + "\":\""; int start = json.indexOf(marker) + marker.length(); return json.substring(start, json.indexOf('"', start)); }
    private static final class MemoryRepository implements ActiveSessionRepository {
        GameSession value;
        public Optional<GameSession> load() { return Optional.ofNullable(value); }
        public void save(GameSession session) { value = session; }
    }
}
