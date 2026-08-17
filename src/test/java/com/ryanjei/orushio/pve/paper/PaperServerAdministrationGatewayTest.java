package com.ryanjei.orushio.pve.paper;

import static org.junit.jupiter.api.Assertions.*;
import com.ryanjei.orushio.pve.application.*;
import com.ryanjei.orushio.pve.domain.GameSession;
import com.ryanjei.orushio.pve.http.AdminHttpServer;
import com.ryanjei.orushio.pve.persistence.ActiveSessionRepository;
import com.ryanjei.orushio.pve.security.AuthService;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class PaperServerAdministrationGatewayTest {
    @Test void 名前解決はPaperメインスレッド処理の外で実行する() {
        TrackingExecutor executor = new TrackingExecutor();
        UUID uuid = UUID.randomUUID();
        AtomicBoolean resolutionCalled = new AtomicBoolean();
        PlayerIdentityResolver resolver = name -> {
            assertFalse(executor.insideMainThreadTask.get());
            resolutionCalled.set(true);
            return new ResolvedPlayerIdentity(uuid, name);
        };
        PaperServerAccess server = new StubServerAccess() {
            @Override public WhitelistEntryView addWhitelistedPlayer(ResolvedPlayerIdentity identity) {
                assertTrue(executor.insideMainThreadTask.get());
                assertEquals(uuid, identity.uuid());
                return new WhitelistEntryView(identity.uuid(), identity.name());
            }
        };
        var gateway = new PaperServerAdministrationGateway(executor, resolver, server);
        assertEquals("Player_1", gateway.addWhitelistedPlayer("Player_1").name());
        assertTrue(resolutionCalled.get());
    }

    @Test void timeoutを返したwhitelist変更は後から適用されない() throws Exception {
        DelayedScheduler scheduler = new DelayedScheduler();
        PaperGameThreadExecutor executor = new PaperGameThreadExecutor(() -> true, () -> false, scheduler, () -> {});
        AtomicBoolean whitelistEnabled = new AtomicBoolean(false);
        PaperServerAccess serverAccess = new StubServerAccess() {
            @Override public boolean whitelistEnabled(){return whitelistEnabled.get();}
            @Override public void setWhitelistEnabled(boolean enabled){whitelistEnabled.set(enabled);}
        };
        var gateway = new PaperServerAdministrationGateway(executor, name -> new ResolvedPlayerIdentity(UUID.randomUUID(), name), serverAccess, Duration.ofMillis(30));
        var games = new DefaultGameApplicationService(new MemoryRepository());
        try (AdminHttpServer http = new AdminHttpServer(InetAddress.getByName("127.0.0.1"), 0, games, new DefaultServerAdministrationService(gateway), new AuthService(), Map::of)) {
            http.start();
            HttpClient client = HttpClient.newHttpClient(); String base = "http://127.0.0.1:" + http.port(); String token = http.issueBootstrapToken();
            var auth = client.send(HttpRequest.newBuilder(URI.create(base + "/auth/bootstrap")).header("X-Bootstrap-Token", token).POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
            String cookie = auth.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0]; String csrf = extract(auth.body(), "csrfToken");
            var response = client.send(HttpRequest.newBuilder(URI.create(base + "/api/v1/server/whitelist")).header("Cookie", cookie).header("Origin", base).header("X-CSRF-Token", csrf).PUT(HttpRequest.BodyPublishers.ofString("{\"enabled\":true}")).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(503, response.statusCode());
            assertFalse(whitelistEnabled.get());
            scheduler.runQueuedTask();
            Thread.sleep(20);
            assertFalse(whitelistEnabled.get());
        }
    }

    @Test void 管理者付与はPaperメインスレッド上でオンラインUUIDだけを対象にする() {
        TrackingExecutor executor = new TrackingExecutor();
        UUID playerId = UUID.randomUUID();
        PaperServerAccess server = new StubServerAccess() {
            @Override public OnlinePlayerView grantSetupAdministrator(UUID requested) {
                assertTrue(executor.insideMainThreadTask.get());
                if (!playerId.equals(requested)) throw new ServerAdministrationException("PLAYER_NOT_ONLINE", "指定されたプレイヤーは現在オンラインではありません。");
                return new OnlinePlayerView(playerId, "Player_1", true);
            }
        };
        var gateway = new PaperServerAdministrationGateway(executor, name -> { throw new AssertionError(); }, server);
        assertTrue(gateway.grantSetupAdministrator(playerId).setupAdministrator());
        assertEquals("PLAYER_NOT_ONLINE", assertThrows(ServerAdministrationException.class, () -> gateway.grantSetupAdministrator(UUID.randomUUID())).code());
    }
    @Test void 管理者解除もPaperメインスレッドを経由する() {TrackingExecutor executor=new TrackingExecutor();UUID id=UUID.randomUUID();PaperServerAccess server=new StubServerAccess(){@Override public OnlinePlayerView revokeSetupAdministrator(UUID requested){assertTrue(executor.insideMainThreadTask.get());assertEquals(id,requested);return new OnlinePlayerView(id,"Player_1",false);}};assertFalse(new PaperServerAdministrationGateway(executor,name->{throw new AssertionError();},server).revokeSetupAdministrator(id).setupAdministrator());}
    @Test void 管理者ownership引継ぎもPaperメインスレッドを経由する(){TrackingExecutor executor=new TrackingExecutor();UUID id=UUID.randomUUID();PaperServerAccess server=new StubServerAccess(){@Override public OnlinePlayerView adoptSetupAdministrator(UUID requested){assertTrue(executor.insideMainThreadTask.get());assertEquals(id,requested);return new OnlinePlayerView(id,"LegacyOp",true,true);}};assertTrue(new PaperServerAdministrationGateway(executor,name->{throw new AssertionError();},server).adoptSetupAdministrator(id).administratorRevocable());}

    private static final class TrackingExecutor implements GameThreadExecutor {
        final AtomicBoolean insideMainThreadTask = new AtomicBoolean();
        public <T> T execute(Callable<T> task, Duration timeout) throws Exception { insideMainThreadTask.set(true); try { return task.call(); } finally { insideMainThreadTask.set(false); } }
        public boolean isAcceptingTasks() { return true; }
    }
    private static class StubServerAccess implements PaperServerAccess {
        public List<OnlinePlayerView> onlinePlayers(){return List.of();}public boolean whitelistEnabled(){return false;}public void setWhitelistEnabled(boolean enabled){}public List<WhitelistEntryView> whitelistedPlayers(){return List.of();}public WhitelistEntryView addWhitelistedPlayer(ResolvedPlayerIdentity identity){throw new UnsupportedOperationException();}public boolean removeWhitelistedPlayer(String playerName){return false;}
    }
    private static final class DelayedScheduler implements PaperGameThreadExecutor.MainThreadScheduler {
        Runnable queued;
        public PaperGameThreadExecutor.ScheduledTask schedule(Runnable runnable){queued=runnable;return () -> {};}
        void runQueuedTask(){queued.run();}
    }
    private static final class MemoryRepository implements ActiveSessionRepository {
        GameSession value;
        public Optional<GameSession> load(){return Optional.ofNullable(value);}public void save(GameSession session){value=session;}
    }
    private static String extract(String json,String key){String marker="\""+key+"\":\"";int start=json.indexOf(marker)+marker.length();return json.substring(start,json.indexOf('"',start));}
}
