package com.ryanjei.orushio.pve.http;

import static org.junit.jupiter.api.Assertions.*;
import com.ryanjei.orushio.pve.application.*;
import com.ryanjei.orushio.pve.domain.*;
import com.ryanjei.orushio.pve.interference.*;
import com.ryanjei.orushio.pve.logging.AuditSink;
import com.ryanjei.orushio.pve.map.*;
import com.ryanjei.orushio.pve.persistence.ActiveSessionRepository;
import com.ryanjei.orushio.pve.security.AuthService;
import java.net.*;import java.net.http.*;import java.time.*;import java.util.*;
import org.junit.jupiter.api.*;

class InterferenceHttpServerTest {
    AdminHttpServer server;HttpClient client;String base,cookie,csrf;UUID sessionId=UUID.randomUUID();GameSession active;FakeGateway gateway;java.util.concurrent.atomic.AtomicReference<GameSession>interferenceSession;java.util.concurrent.atomic.AtomicBoolean diagnostic,recovery;
    @BeforeEach void start()throws Exception{Instant now=Instant.now();active=new GameSession(sessionId,GameState.ACTIVE,List.of(new Participant(UUID.randomUUID(),"A",true)),now,"map-a",1,60,2,1,now,now.plusSeconds(60),Set.of());DefaultGameApplicationService games=new DefaultGameApplicationService(new Memory(),active);MapProfile profile=new MapProfile(new MapProfileId("map-a"),"A",false,"template",Map.of(),Map.of("finalRegion",List.of(new Cuboid(0,0,0,1,1,1))),now);GameRuntimeContext context=new GameRuntimeContext(sessionId,"runtime-a",RuntimeMap.resolve(profile));gateway=new FakeGateway();AuditSink audit=new NoAudit();interferenceSession=new java.util.concurrent.atomic.AtomicReference<>(active);diagnostic=new java.util.concurrent.atomic.AtomicBoolean();recovery=new java.util.concurrent.atomic.AtomicBoolean();InterferenceApplicationService interference=new InterferenceApplicationService(interferenceSession::get,id->Optional.of(context),gateway,InterferenceSettings.defaults(),audit);server=new AdminHttpServer(InetAddress.getByName("127.0.0.1"),0,games,new EmptyAdmin(),null,new AuthService(),Map::of,audit,diagnostic::get,recovery::get,null,null,interference);server.start();client=HttpClient.newHttpClient();base="http://127.0.0.1:"+server.port();String token=server.issueBootstrapToken();var auth=client.send(HttpRequest.newBuilder(URI.create(base+"/auth/bootstrap")).header("X-Bootstrap-Token",token).POST(HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString());cookie=auth.headers().firstValue("Set-Cookie").orElseThrow().split(";",2)[0];csrf=field(auth.body(),"csrfToken");}
    @AfterEach void stop(){server.close();}
    @Test void 未認証とCSRF不正を拒否する()throws Exception{assertEquals(401,send("DARKNESS",false,csrf,sessionId).statusCode());assertEquals(403,send("DARKNESS",true,"wrong",sessionId).statusCode());}
    @Test void 正常TypeはPaper処理結果を返し未知Typeは400()throws Exception{var success=send("DARKNESS",true,csrf,sessionId);assertEquals(200,success.statusCode());assertTrue(success.body().contains("affectedPlayerCount\":1"));assertEquals(400,send("UNKNOWN",true,csrf,sessionId).statusCode());}
    @Test void ACTIVE外と対象なしと旧Sessionを409で拒否する()throws Exception{interferenceSession.set(GameSession.idle());assertEquals(409,send("DARKNESS",true,csrf,sessionId).statusCode());interferenceSession.set(active);gateway.result=new InterferenceGateway.ExecutionResult(0,1);assertEquals(409,send("LEVITATION",true,csrf,sessionId).statusCode());assertEquals(409,send("HOTBAR_SHUFFLE",true,csrf,UUID.randomUUID()).statusCode());}
    @Test void diagnosticとRecovery中は既存mutation保護で拒否する()throws Exception{diagnostic.set(true);assertEquals(503,send("DARKNESS",true,csrf,sessionId).statusCode());diagnostic.set(false);recovery.set(true);assertEquals(503,send("DARKNESS",true,csrf,sessionId).statusCode());}
    private HttpResponse<String>send(String type,boolean authenticated,String token,UUID session)throws Exception{HttpRequest.Builder request=HttpRequest.newBuilder(URI.create(base+"/api/v1/game/interference/test")).header("Origin",base).header("X-CSRF-Token",token).header("If-Session-Id",session.toString()).POST(HttpRequest.BodyPublishers.ofString("{\"type\":\""+type+"\"}"));if(authenticated)request.header("Cookie",cookie);return client.send(request.build(),HttpResponse.BodyHandlers.ofString());}
    private static String field(String json,String key){String marker="\""+key+"\":\"";int start=json.indexOf(marker)+marker.length();return json.substring(start,json.indexOf('"',start));}
    private static final class FakeGateway implements InterferenceGateway{ExecutionResult result=new ExecutionResult(1,0);public ExecutionResult apply(UUID s,String w,Cuboid r,List<UUID>p,InterferenceType t,InterferenceSettings x){return result;}}
    private static final class Memory implements ActiveSessionRepository{public Optional<GameSession>load(){return Optional.empty();}public void save(GameSession session){}}
    private static final class NoAudit implements AuditSink{public void record(String a,String b,String c,String d){}public boolean healthy(){return true;}}
    private static final class EmptyAdmin implements ServerAdministrationService{public List<OnlinePlayerView>onlinePlayers(){return List.of();}public boolean whitelistEnabled(){return false;}public boolean setWhitelistEnabled(boolean value){return value;}public List<WhitelistEntryView>whitelistedPlayers(){return List.of();}public WhitelistEntryView addWhitelistedPlayer(String name){throw new UnsupportedOperationException();}public void removeWhitelistedPlayer(String name){}}
}
