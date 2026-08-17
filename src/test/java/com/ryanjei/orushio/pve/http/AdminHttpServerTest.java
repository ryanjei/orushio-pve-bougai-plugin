package com.ryanjei.orushio.pve.http;

import static org.junit.jupiter.api.Assertions.*;
import com.ryanjei.orushio.pve.application.*;
import com.ryanjei.orushio.pve.domain.GameSession;
import com.ryanjei.orushio.pve.persistence.ActiveSessionRepository;
import com.ryanjei.orushio.pve.security.AuthService;
import com.ryanjei.orushio.pve.logging.AuditSink;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
        server = new AdminHttpServer(InetAddress.getByName("127.0.0.1"), 0, games, new FakeAdministration(), new AuthService(), () -> Map.of("paperRunning", true));
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
    @Test void Origin不一致を単独で拒否する() throws Exception {var request=HttpRequest.newBuilder(URI.create(base+"/api/v1/game/recruiting/start")).header("Cookie",cookie).header("Origin","http://evil.invalid").header("X-CSRF-Token",csrf).header("If-Game-State","IDLE").POST(HttpRequest.BodyPublishers.noBody()).build();assertEquals(403,client.send(request,HttpResponse.BodyHandlers.ofString()).statusCode());}
    @Test void Host不一致を単独で拒否する() throws Exception {try(var socket=new java.net.Socket("127.0.0.1",server.port())){String raw="POST /api/v1/game/recruiting/start HTTP/1.1\r\nHost: evil.invalid\r\nOrigin: "+base+"\r\nCookie: "+cookie+"\r\nX-CSRF-Token: "+csrf+"\r\nIf-Game-State: IDLE\r\nContent-Length: 0\r\nConnection: close\r\n\r\n";socket.getOutputStream().write(raw.getBytes(java.nio.charset.StandardCharsets.US_ASCII));String response=new String(socket.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);assertTrue(response.startsWith("HTTP/1.1 403"));}}
    @Test void 入力不足と未実装APIを成功にしない() throws Exception {var missing=HttpRequest.newBuilder(URI.create(base+"/api/v1/game/recruiting/start")).header("Cookie",cookie).header("Origin",base).header("X-CSRF-Token",csrf).POST(HttpRequest.BodyPublishers.noBody()).build();assertEquals(400,client.send(missing,HttpResponse.BodyHandlers.ofString()).statusCode());var unknown=HttpRequest.newBuilder(URI.create(base+"/api/v1/game/prepare")).header("Cookie",cookie).GET().build();assertEquals(404,client.send(unknown,HttpResponse.BodyHandlers.ofString()).statusCode());}
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
    @Test void 受付終了APIも正常動作しsession不一致は409() throws Exception {assertEquals(200,sendMutation(true,csrf,"IDLE").statusCode());String session=(await("/api/v1/game/current"));var bad=close(java.util.UUID.randomUUID().toString());assertEquals(409,bad.statusCode());assertEquals(200,close(extract(session,"sessionId")).statusCode());}
    @Test void 診断モードは診断APIを提供しmutationを拒否する() throws Exception {CapturingAudit audit=new CapturingAudit();try(AdminHttpServer diagnostic=new AdminHttpServer(InetAddress.getByName("127.0.0.1"),0,new DefaultGameApplicationService(new MemoryRepository()),new FakeAdministration(),new AuthService(),()->Map.of("diagnosticMode",true,"configLoaded",false,"warnings",List.of("設定エラー")),audit,true)){diagnostic.start();String b="http://127.0.0.1:"+diagnostic.port();String token=diagnostic.issueBootstrapToken();var authResponse=client.send(HttpRequest.newBuilder(URI.create(b+"/auth/bootstrap")).header("X-Bootstrap-Token",token).POST(HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString());String c=authResponse.headers().firstValue("Set-Cookie").orElseThrow().split(";",2)[0],xs=extract(authResponse.body(),"csrfToken");var diag=client.send(HttpRequest.newBuilder(URI.create(b+"/api/v1/system/diagnostics")).header("Cookie",c).GET().build(),HttpResponse.BodyHandlers.ofString());assertEquals(200,diag.statusCode());assertTrue(diag.body().contains("configLoaded"));var mutation=client.send(HttpRequest.newBuilder(URI.create(b+"/api/v1/game/recruiting/start")).header("Cookie",c).header("Origin",b).header("X-CSRF-Token",xs).header("If-Game-State","IDLE").POST(HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString());assertEquals(503,mutation.statusCode());}}
    @Test void 内部失敗のtraceIdは監査と一致し詳細を漏らさない() throws Exception {CapturingAudit audit=new CapturingAudit();ActiveSessionRepository failing=new ActiveSessionRepository(){public Optional<GameSession> load(){return Optional.empty();}public void save(GameSession s){throw new RuntimeException("SECRET_INTERNAL_PATH");}};try(AdminHttpServer s=new AdminHttpServer(InetAddress.getByName("127.0.0.1"),0,new DefaultGameApplicationService(failing),new FakeAdministration(),new AuthService(),()->Map.of(),audit,false)){s.start();String b="http://127.0.0.1:"+s.port(),token=s.issueBootstrapToken();var a=client.send(HttpRequest.newBuilder(URI.create(b+"/auth/bootstrap")).header("X-Bootstrap-Token",token).POST(HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString());String c=a.headers().firstValue("Set-Cookie").orElseThrow().split(";",2)[0],xs=extract(a.body(),"csrfToken");var response=client.send(HttpRequest.newBuilder(URI.create(b+"/api/v1/game/recruiting/start")).header("Cookie",c).header("Origin",b).header("X-CSRF-Token",xs).header("If-Game-State","IDLE").POST(HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString());assertEquals(500,response.statusCode());assertFalse(response.body().contains("SECRET_INTERNAL_PATH"));assertEquals(extract(response.body(),"traceId"),audit.trace);}}
    @Test void playersAPIはGateway相当の取得完了を待つ() throws Exception {long start=System.nanoTime();FakeAdministration administration=new FakeAdministration(){@Override public List<OnlinePlayerView> onlinePlayers(){try{Thread.sleep(150);}catch(InterruptedException e){throw new RuntimeException(e);}return List.of();}};try(AdminHttpServer delayed=new AdminHttpServer(InetAddress.getByName("127.0.0.1"),0,new DefaultGameApplicationService(new MemoryRepository()),administration,new AuthService(),()->Map.of())){delayed.start();String b="http://127.0.0.1:"+delayed.port(),token=delayed.issueBootstrapToken();var a=client.send(HttpRequest.newBuilder(URI.create(b+"/auth/bootstrap")).header("X-Bootstrap-Token",token).POST(HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString());String c=a.headers().firstValue("Set-Cookie").orElseThrow().split(";",2)[0];assertEquals(200,client.send(HttpRequest.newBuilder(URI.create(b+"/api/v1/players")).header("Cookie",c).GET().build(),HttpResponse.BodyHandlers.ofString()).statusCode());assertTrue(java.time.Duration.ofNanos(System.nanoTime()-start).toMillis()>=150);}}
    @Test void ホワイトリスト状態一覧追加削除APIが実状態を返す() throws Exception {
        FakeAdministration administration = new FakeAdministration();
        try (AuthenticatedServer test = authenticated(administration)) {
            assertTrue(test.get("/api/v1/server/whitelist").body().contains("\"enabled\":false"));
            assertEquals(200, test.mutate("/api/v1/server/whitelist", "PUT", "{\"enabled\":true}").statusCode());
            assertEquals(200, test.mutate("/api/v1/server/whitelist/players", "POST", "{\"name\":\"Player_1\"}").statusCode());
            assertTrue(test.get("/api/v1/server/whitelist").body().contains("Player_1"));
            assertEquals(200, test.mutate("/api/v1/server/whitelist/players", "DELETE", "{\"name\":\"Player_1\"}").statusCode());
            assertFalse(test.get("/api/v1/server/whitelist").body().contains("Player_1"));
        }
    }
    @Test void セットアップ管理者付与APIは認証OriginCsrfを要求しオンラインUUIDだけを受け付ける() throws Exception {
        FakeAdministration administration = new FakeAdministration(); UUID playerId = UUID.randomUUID(); administration.online.add(new OnlinePlayerView(playerId,"Player_1",false,false));
        try (AuthenticatedServer test = authenticated(administration)) {
            var success=test.mutate("/api/v1/players/setup-administrator","PUT","{\"uuid\":\""+playerId+"\"}");assertEquals(200,success.statusCode());assertTrue(success.body().contains("\"setupAdministrator\":true"));assertTrue(success.body().contains("\"administratorRevocable\":true"));var revoked=test.mutate("/api/v1/players/setup-administrator","DELETE","{\"uuid\":\""+playerId+"\"}");assertEquals(200,revoked.statusCode());assertTrue(revoked.body().contains("\"setupAdministrator\":false"));
            assertEquals(409,test.mutate("/api/v1/players/setup-administrator","PUT","{\"uuid\":\""+UUID.randomUUID()+"\"}").statusCode());
            var unauth=HttpRequest.newBuilder(URI.create(test.baseUrl+"/api/v1/players/setup-administrator")).header("Origin",test.baseUrl).header("X-CSRF-Token",test.csrfValue).PUT(HttpRequest.BodyPublishers.ofString("{\"uuid\":\""+playerId+"\"}")).build();assertEquals(401,client.send(unauth,HttpResponse.BodyHandlers.ofString()).statusCode());
            var badCsrf=HttpRequest.newBuilder(URI.create(test.baseUrl+"/api/v1/players/setup-administrator")).header("Cookie",test.cookieValue).header("Origin",test.baseUrl).header("X-CSRF-Token","bad").PUT(HttpRequest.BodyPublishers.ofString("{\"uuid\":\""+playerId+"\"}")).build();assertEquals(403,client.send(badCsrf,HttpResponse.BodyHandlers.ofString()).statusCode());
        }
    }
    @Test void ホワイトリストAPIは入力異常とMinecraft取得失敗を安全に返す() throws Exception {
        try (AuthenticatedServer test = authenticated(new FakeAdministration())) {
            assertEquals(400, test.mutate("/api/v1/server/whitelist/players", "POST", "{}").statusCode());
        }
        try (AuthenticatedServer test = authenticated(new FailingAdministration())) {
            var response = test.get("/api/v1/server/whitelist");
            assertEquals(503, response.statusCode());
            assertTrue(response.body().contains("MINECRAFT_UNAVAILABLE"));
            assertFalse(response.body().contains("SECRET_SERVER_DETAIL"));
        }
    }
    @Test void HTTP停止後は新規受付を拒否する() throws Exception {AdminHttpServer stopped=new AdminHttpServer(InetAddress.getByName("127.0.0.1"),0,new DefaultGameApplicationService(new MemoryRepository()),new FakeAdministration(),new AuthService(),()->Map.of());stopped.start();int port=stopped.port();stopped.close();assertThrows(Exception.class,()->client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/status")).GET().build(),HttpResponse.BodyHandlers.ofString()));}
    private HttpResponse<String> sendMutation(boolean authenticated, String csrfValue, String state) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(base + "/api/v1/game/recruiting/start")).header("Origin", base).header("X-CSRF-Token", csrfValue).header("If-Game-State", state).POST(HttpRequest.BodyPublishers.noBody());
        if (authenticated) builder.header("Cookie", cookie);
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
    private HttpResponse<String> close(String session)throws Exception{return client.send(HttpRequest.newBuilder(URI.create(base+"/api/v1/game/recruiting/close")).header("Cookie",cookie).header("Origin",base).header("X-CSRF-Token",csrf).header("If-Session-Id",session).POST(HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString());}
    private String await(String path)throws Exception{return client.send(HttpRequest.newBuilder(URI.create(base+path)).header("Cookie",cookie).GET().build(),HttpResponse.BodyHandlers.ofString()).body();}
    private AuthenticatedServer authenticated(ServerAdministrationService administration)throws Exception{return new AuthenticatedServer(administration);}
    private static String extract(String json, String key) { String marker = "\"" + key + "\":\""; int start = json.indexOf(marker) + marker.length(); return json.substring(start, json.indexOf('"', start)); }
    private static final class MemoryRepository implements ActiveSessionRepository {
        GameSession value;
        public Optional<GameSession> load() { return Optional.ofNullable(value); }
        public void save(GameSession session) { value = session; }
    }
    private static final class CapturingAudit implements AuditSink {String trace;public void record(String t,String c,String code,String op){trace=t;}public boolean healthy(){return true;}}
    private final class AuthenticatedServer implements AutoCloseable {
        final AdminHttpServer value; final String baseUrl,cookieValue,csrfValue;
        AuthenticatedServer(ServerAdministrationService administration)throws Exception{value=new AdminHttpServer(InetAddress.getByName("127.0.0.1"),0,new DefaultGameApplicationService(new MemoryRepository()),administration,new AuthService(),()->Map.of());value.start();baseUrl="http://127.0.0.1:"+value.port();String token=value.issueBootstrapToken();var auth=client.send(HttpRequest.newBuilder(URI.create(baseUrl+"/auth/bootstrap")).header("X-Bootstrap-Token",token).POST(HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString());cookieValue=auth.headers().firstValue("Set-Cookie").orElseThrow().split(";",2)[0];csrfValue=extract(auth.body(),"csrfToken");}
        HttpResponse<String> get(String path)throws Exception{return client.send(HttpRequest.newBuilder(URI.create(baseUrl+path)).header("Cookie",cookieValue).GET().build(),HttpResponse.BodyHandlers.ofString());}
        HttpResponse<String> mutate(String path,String method,String body)throws Exception{var builder=HttpRequest.newBuilder(URI.create(baseUrl+path)).header("Cookie",cookieValue).header("Origin",baseUrl).header("X-CSRF-Token",csrfValue).header("Content-Type","application/json").method(method,HttpRequest.BodyPublishers.ofString(body));return client.send(builder.build(),HttpResponse.BodyHandlers.ofString());}
        public void close(){value.close();}
    }
    private static class FakeAdministration implements ServerAdministrationService {
        boolean enabled; final List<WhitelistEntryView> entries=new java.util.ArrayList<>(); final List<OnlinePlayerView> online=new java.util.ArrayList<>();
        public List<OnlinePlayerView> onlinePlayers(){return List.copyOf(online);}public OnlinePlayerView grantSetupAdministrator(UUID id){for(int i=0;i<online.size();i++)if(online.get(i).uuid().equals(id)){var value=new OnlinePlayerView(id,online.get(i).name(),true);online.set(i,value);return value;}throw new ServerAdministrationException("PLAYER_NOT_ONLINE","指定されたプレイヤーは現在オンラインではありません。");}public OnlinePlayerView revokeSetupAdministrator(UUID id){for(int i=0;i<online.size();i++)if(online.get(i).uuid().equals(id)){var value=new OnlinePlayerView(id,online.get(i).name(),false);online.set(i,value);return value;}throw new ServerAdministrationException("PLAYER_NOT_ONLINE","指定されたプレイヤーは現在オンラインではありません。");}public boolean whitelistEnabled(){return enabled;}public boolean setWhitelistEnabled(boolean value){enabled=value;return enabled;}public List<WhitelistEntryView> whitelistedPlayers(){return List.copyOf(entries);}public WhitelistEntryView addWhitelistedPlayer(String name){var entry=new WhitelistEntryView(UUID.randomUUID(),name);entries.add(entry);return entry;}public void removeWhitelistedPlayer(String name){entries.removeIf(entry->entry.name().equalsIgnoreCase(name));}
    }
    private static final class FailingAdministration extends FakeAdministration { @Override public boolean whitelistEnabled(){throw new ServerAdministrationException("MINECRAFT_UNAVAILABLE","Minecraftサーバー情報を取得できませんでした。");} }
}
