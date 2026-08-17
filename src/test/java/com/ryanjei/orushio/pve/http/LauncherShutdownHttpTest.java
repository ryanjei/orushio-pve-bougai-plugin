package com.ryanjei.orushio.pve.http;

import static org.junit.jupiter.api.Assertions.*;
import com.ryanjei.orushio.pve.application.*;
import com.ryanjei.orushio.pve.domain.GameSession;
import com.ryanjei.orushio.pve.logging.AuditSink;
import com.ryanjei.orushio.pve.persistence.ActiveSessionRepository;
import com.ryanjei.orushio.pve.security.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class LauncherShutdownHttpTest {
    @Test void launcherTokenはlocalhostで一回だけ使えshutdownを監査する()throws Exception{LauncherShutdownToken token=new LauncherShutdownToken();String secret=token.issue();AtomicBoolean shutdown=new AtomicBoolean();List<String> audit=new ArrayList<>();try(AdminHttpServer server=server(token,()->shutdown.set(true),audit,false)){server.start();String url="http://127.0.0.1:"+server.port()+"/launcher/shutdown";HttpClient client=HttpClient.newHttpClient();assertEquals(401,post(client,url,"invalid").statusCode());assertEquals(202,post(client,url,secret).statusCode());for(int i=0;i<20&&!shutdown.get();i++)Thread.sleep(50);assertTrue(shutdown.get());assertEquals(401,post(client,url,secret).statusCode());assertTrue(audit.stream().anyMatch(value->value.startsWith("SYSTEM_SHUTDOWN_REQUESTED:")));assertTrue(audit.stream().noneMatch(value->value.contains(secret)));}}
    @Test void diagnosticModeでも安全停止を許可しlocalhost以外へbindしない()throws Exception{LauncherShutdownToken token=new LauncherShutdownToken();String secret=token.issue();AtomicBoolean shutdown=new AtomicBoolean();try(AdminHttpServer server=server(token,()->shutdown.set(true),new ArrayList<>(),true)){server.start();assertEquals(202,post(HttpClient.newHttpClient(),"http://127.0.0.1:"+server.port()+"/launcher/shutdown",secret).statusCode());}assertThrows(IllegalArgumentException.class,()->new AdminHttpServer(InetAddress.getByName("0.0.0.0"),0,new DefaultGameApplicationService(new Memory()),new EmptyAdmin(),null,new AuthService(),Map::of,new EmptyAudit(),false,token,()->{}));}
    private static HttpResponse<String> post(HttpClient client,String url,String token)throws Exception{return client.send(HttpRequest.newBuilder(URI.create(url)).header("X-OPBP-Shutdown-Token",token).POST(HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString());}
    private static AdminHttpServer server(LauncherShutdownToken token,Runnable shutdown,List<String> audit,boolean diagnostic)throws Exception{return new AdminHttpServer(InetAddress.getByName("127.0.0.1"),0,new DefaultGameApplicationService(new Memory()),new EmptyAdmin(),null,new AuthService(),Map::of,new AuditSink(){public void record(String trace,String category,String code,String operation){audit.add(code+":"+operation);}public boolean healthy(){return true;}},diagnostic,token,shutdown);}
    private static final class Memory implements ActiveSessionRepository{GameSession value;public Optional<GameSession> load(){return Optional.ofNullable(value);}public void save(GameSession session){value=session;}}
    private static final class EmptyAdmin implements ServerAdministrationService{public List<OnlinePlayerView> onlinePlayers(){return List.of();}public boolean whitelistEnabled(){return false;}public boolean setWhitelistEnabled(boolean value){return value;}public List<WhitelistEntryView> whitelistedPlayers(){return List.of();}public WhitelistEntryView addWhitelistedPlayer(String name){throw new UnsupportedOperationException();}public void removeWhitelistedPlayer(String name){}}
    private static final class EmptyAudit implements AuditSink{public void record(String a,String b,String c,String d){}public boolean healthy(){return true;}}
}
