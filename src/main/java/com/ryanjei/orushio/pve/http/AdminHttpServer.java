package com.ryanjei.orushio.pve.http;

import com.ryanjei.orushio.pve.application.GameApplicationService;
import com.ryanjei.orushio.pve.application.OnlinePlayerView;
import com.ryanjei.orushio.pve.application.PlayerQuery;
import com.ryanjei.orushio.pve.domain.DomainException;
import com.ryanjei.orushio.pve.domain.GameSession;
import com.ryanjei.orushio.pve.security.AuthService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.function.Consumer;

public final class AdminHttpServer implements AutoCloseable {
    private static final String COOKIE = "orushio_session";
    private final HttpServer server;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final GameApplicationService games;
    private final PlayerQuery players;
    private final AuthService auth;
    private final Diagnostics diagnostics;
    private final Consumer<String> operationLog;
    private final String expectedOrigin;
    private final String expectedHost;

    public AdminHttpServer(InetAddress bind, int port, GameApplicationService games, PlayerQuery players, AuthService auth, Diagnostics diagnostics) throws IOException {
        this(bind, port, games, players, auth, diagnostics, ignored -> {});
    }

    public AdminHttpServer(InetAddress bind, int port, GameApplicationService games, PlayerQuery players, AuthService auth, Diagnostics diagnostics, Consumer<String> operationLog) throws IOException {
        if (!bind.isLoopbackAddress()) throw new IllegalArgumentException("管理画面はlocalhost以外へ公開できません。");
        this.games = games; this.players = players; this.auth = auth; this.diagnostics = diagnostics; this.operationLog = operationLog;
        server = HttpServer.create(new InetSocketAddress(bind, port), 0);
        this.expectedHost = "127.0.0.1:" + server.getAddress().getPort();
        this.expectedOrigin = "http://" + expectedHost;
        server.createContext("/", this::route);
        server.setExecutor(executor);
    }

    public void start() { server.start(); }
    public int port() { return server.getAddress().getPort(); }
    public String issueBootstrapToken() { return auth.issueBootstrap(Duration.ofMinutes(2)); }

    private void route(HttpExchange exchange) throws IOException {
        try {
            addSecurityHeaders(exchange);
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/auth/bootstrap") && (exchange.getRequestMethod().equals("POST") || exchange.getRequestMethod().equals("GET"))) { bootstrap(exchange); return; }
            if (!path.startsWith("/api/v1/")) { staticAsset(exchange, path); return; }
            AuthService.Session session = authenticate(exchange).orElseThrow(() -> new HttpProblem(401, "AUTH_REQUIRED", "認証が必要です。"));
            boolean mutation = !exchange.getRequestMethod().equals("GET");
            if (mutation) validateMutation(exchange, session);
            if (path.equals("/api/v1/auth/session") && method(exchange, "GET")) ok(exchange, Map.of("csrfToken", session.csrf()));
            else if (path.equals("/api/v1/status") && method(exchange, "GET")) ok(exchange, status());
            else if (path.equals("/api/v1/players") && method(exchange, "GET")) ok(exchange, Map.of("players", players.onlinePlayers().stream().map(this::player).toList()));
            else if (path.equals("/api/v1/game/current") && method(exchange, "GET")) ok(exchange, game(games.current()));
            else if (path.equals("/api/v1/system/diagnostics") && method(exchange, "GET")) ok(exchange, diagnostics.snapshot());
            else if (path.equals("/api/v1/game/recruiting/start") && method(exchange, "POST")) ok(exchange, operation(() -> games.startRecruiting(header(exchange, "If-Game-State"))));
            else if (path.equals("/api/v1/game/recruiting/close") && method(exchange, "POST")) ok(exchange, operation(() -> games.closeRecruiting(header(exchange, "If-Session-Id"))));
            else throw new HttpProblem(404, "NOT_FOUND", "指定された機能はありません。");
        } catch (DomainException e) { error(exchange, e.code().equals("SESSION_MISMATCH") ? 409 : 409, e.code(), e.getMessage()); }
        catch (HttpProblem e) { error(exchange, e.status, e.code, e.getMessage()); }
        catch (Exception e) { error(exchange, 500, "INTERNAL_ERROR", "内部エラーが発生しました。"); }
        finally { exchange.close(); }
    }

    private void bootstrap(HttpExchange x) throws IOException {
        String token = header(x, "X-Bootstrap-Token");
        if (token.isEmpty() && x.getRequestURI().getRawQuery() != null) {
            token = Arrays.stream(x.getRequestURI().getRawQuery().split("&")).filter(v -> v.startsWith("token=")).map(v -> v.substring(6)).findFirst().orElse("");
        }
        AuthService.Session session = auth.exchange(token).orElseThrow(() -> new HttpProblem(401, "AUTH_REQUIRED", "bootstrap tokenが無効です。"));
        x.getResponseHeaders().add("Set-Cookie", COOKIE + "=" + session.id() + "; Path=/; HttpOnly; SameSite=Strict");
        if (x.getRequestMethod().equals("GET")) {
            x.getResponseHeaders().set("Location", "/");
            x.sendResponseHeaders(303, -1);
        } else send(x, 200, Json.value(Map.of("ok", true, "data", Map.of("csrfToken", session.csrf()))));
    }

    private Optional<AuthService.Session> authenticate(HttpExchange x) {
        String cookies = x.getRequestHeaders().getFirst("Cookie");
        if (cookies == null) return Optional.empty();
        return Arrays.stream(cookies.split(";")).map(String::trim).filter(v -> v.startsWith(COOKIE + "=")).findFirst().flatMap(v -> auth.authenticate(v.substring(COOKIE.length() + 1)));
    }

    private void validateMutation(HttpExchange x, AuthService.Session session) {
        if (!expectedHost.equalsIgnoreCase(header(x, "Host"))) throw new HttpProblem(403, "FORBIDDEN", "Hostが許可されていません。");
        if (!expectedOrigin.equalsIgnoreCase(header(x, "Origin"))) throw new HttpProblem(403, "FORBIDDEN", "Originが許可されていません。");
        if (!session.csrf().equals(header(x, "X-CSRF-Token"))) throw new HttpProblem(403, "FORBIDDEN", "CSRF tokenが無効です。");
    }

    private Map<String, Object> status() { Map<String,Object> data = new LinkedHashMap<>(game(games.current())); data.putAll(diagnostics.snapshot()); return data; }
    private Map<String, Object> game(GameSession s) { return Map.of("gameState", s.state().name(), "sessionId", s.sessionId().toString(), "participantCount", s.participants().size(), "participantLimit", 4); }
    private Map<String, Object> player(OnlinePlayerView p) { return Map.of("uuid", p.uuid().toString(), "name", p.name()); }
    private Map<String, Object> operation(Supplier<com.ryanjei.orushio.pve.application.OperationResult> action) { var r = action.get(); operationLog.accept("operationId=" + r.operationId() + " state=" + r.state()); return Map.of("operationId", r.operationId(), "sessionId", r.sessionId(), "state", r.state()); }
    private static boolean method(HttpExchange x, String value) { if (!x.getRequestMethod().equals(value)) throw new HttpProblem(405, "INVALID_INPUT", "HTTPメソッドが正しくありません。"); return true; }
    private static String header(HttpExchange x, String name) { String v = x.getRequestHeaders().getFirst(name); return v == null ? "" : v; }

    private void staticAsset(HttpExchange x, String path) throws IOException {
        String resource = switch (path) { case "/", "/index.html" -> "/web/index.html"; case "/app.js" -> "/web/app.js"; case "/style.css" -> "/web/style.css"; default -> null; };
        if (resource == null) throw new HttpProblem(404, "NOT_FOUND", "ページがありません。");
        try (var stream = getClass().getResourceAsStream(resource)) {
            if (stream == null) throw new HttpProblem(404, "NOT_FOUND", "ページがありません。");
            byte[] bytes = stream.readAllBytes();
            x.getResponseHeaders().set("Content-Type", resource.endsWith(".html") ? "text/html; charset=utf-8" : resource.endsWith(".css") ? "text/css; charset=utf-8" : "text/javascript; charset=utf-8");
            x.sendResponseHeaders(200, bytes.length); x.getResponseBody().write(bytes);
        }
    }
    private static void ok(HttpExchange x, Object data) throws IOException { send(x, 200, Json.value(Map.of("ok", true, "data", data))); }
    private static void error(HttpExchange x, int status, String code, String message) throws IOException { send(x, status, Json.value(Map.of("ok", false, "error", Map.of("code", code, "message", message, "fields", Map.of(), "traceId", java.util.UUID.randomUUID().toString())))); }
    private static void send(HttpExchange x, int status, String body) throws IOException { byte[] bytes = body.getBytes(StandardCharsets.UTF_8); x.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8"); x.sendResponseHeaders(status, bytes.length); x.getResponseBody().write(bytes); }
    private static void addSecurityHeaders(HttpExchange x) { x.getResponseHeaders().set("X-Content-Type-Options", "nosniff"); x.getResponseHeaders().set("Content-Security-Policy", "default-src 'self'; connect-src 'self'; script-src 'self'; style-src 'self'"); x.getResponseHeaders().set("Cache-Control", "no-store"); }
    public void close() { server.stop(1); executor.shutdownNow(); }
    private static final class HttpProblem extends RuntimeException { final int status; final String code; HttpProblem(int status, String code, String message) { super(message); this.status = status; this.code = code; } }
}
