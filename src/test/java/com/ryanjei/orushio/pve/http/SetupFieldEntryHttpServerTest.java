package com.ryanjei.orushio.pve.http;

import com.ryanjei.orushio.pve.application.*;
import com.ryanjei.orushio.pve.domain.*;
import com.ryanjei.orushio.pve.logging.AuditSink;
import com.ryanjei.orushio.pve.map.*;
import com.ryanjei.orushio.pve.persistence.ActiveSessionRepository;
import com.ryanjei.orushio.pve.security.AuthService;
import org.junit.jupiter.api.*;

import java.net.*;
import java.net.http.*;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SetupFieldEntryHttpServerTest {
    private AdminHttpServer server;
    private HttpClient client;
    private FakeMaps maps;
    private List<String> auditCodes;
    private String base,cookie,csrf;

    @BeforeEach void start() throws Exception {
        maps=new FakeMaps();auditCodes=new ArrayList<>();
        ServerAdministrationService administration=new ServerAdministrationService(){
            public List<OnlinePlayerView> onlinePlayers(){return List.of();}
            public boolean whitelistEnabled(){return false;}
            public boolean setWhitelistEnabled(boolean enabled){return enabled;}
            public List<WhitelistEntryView> whitelistedPlayers(){return List.of();}
            public WhitelistEntryView addWhitelistedPlayer(String name){throw new UnsupportedOperationException();}
            public void removeWhitelistedPlayer(String name){}
        };
        server=new AdminHttpServer(InetAddress.getByName("127.0.0.1"),0,new DefaultGameApplicationService(new MemorySession()),administration,maps,new AuthService(),Map::of,new AuditSink(){public void record(String a,String b,String code,String d){auditCodes.add(code);}public boolean healthy(){return true;}},false);
        server.start();client=HttpClient.newHttpClient();base="http://127.0.0.1:"+server.port();
        String token=server.issueBootstrapToken();
        HttpResponse<String> auth=client.send(HttpRequest.newBuilder(URI.create(base+"/auth/bootstrap")).header("X-Bootstrap-Token",token).POST(HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString());
        cookie=auth.headers().firstValue("Set-Cookie").orElseThrow().split(";",2)[0];csrf=field(auth.body(),"csrfToken");
    }

    @AfterEach void stop(){server.close();}

    @Test void entry変更対象を認証済みPUTで選択する() throws Exception {
        String body="{\"administrator\":\""+maps.administrator+"\",\"field\":\"shopPoints\",\"area\":false,\"index\":2}";
        HttpResponse<String> response=client.send(HttpRequest.newBuilder(URI.create(base+"/api/v1/maps/setup/field-entry")).header("Cookie",cookie).header("Origin",base).header("X-CSRF-Token",csrf).PUT(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,response.statusCode());assertEquals("shopPoints",maps.field);assertEquals(2,maps.index);assertTrue(auditCodes.contains("MAP_SETUP_ENTRY_EDIT_SELECTED"));
    }

    @Test void 認証失敗では成功auditを記録しない() throws Exception {String body="{\"administrator\":\""+maps.administrator+"\",\"field\":\"shopPoints\",\"area\":false,\"index\":2}";HttpResponse<String> response=client.send(HttpRequest.newBuilder(URI.create(base+"/api/v1/maps/setup/field-entry")).header("Origin",base).header("X-CSRF-Token",csrf).PUT(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());assertEquals(401,response.statusCode());assertFalse(auditCodes.contains("MAP_SETUP_ENTRY_EDIT_SELECTED"));}

    @Test void 不正indexでは成功auditを記録しない() throws Exception {String body="{\"administrator\":\""+maps.administrator+"\",\"field\":\"shopPoints\",\"area\":false,\"index\":-1}";HttpResponse<String> response=client.send(HttpRequest.newBuilder(URI.create(base+"/api/v1/maps/setup/field-entry")).header("Cookie",cookie).header("Origin",base).header("X-CSRF-Token",csrf).PUT(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());assertEquals(400,response.statusCode());assertFalse(auditCodes.contains("MAP_SETUP_ENTRY_EDIT_SELECTED"));}

    private static String field(String json,String key){String marker="\""+key+"\":\"";int start=json.indexOf(marker)+marker.length();return json.substring(start,json.indexOf('"',start));}
    private static final class MemorySession implements ActiveSessionRepository{GameSession value;public Optional<GameSession> load(){return Optional.ofNullable(value);}public void save(GameSession session){value=session;}}
    private static final class FakeMaps implements MapAdministrationService{
        final UUID administrator=UUID.randomUUID();String field;int index=-1;
        public List<MapProfile> list(){return List.of();}public Optional<String> fixedNextMapId(){return Optional.empty();}
        public MapProfile importZip(Path zip,String mapId,String displayName){throw new UnsupportedOperationException();}public MapProfile setEnabled(String mapId,boolean enabled){throw new UnsupportedOperationException();}
        public void fixNext(String mapId){}public void delete(String mapId){}public SetupView startSetup(String mapId,UUID administrator,String expectedState){return setup();}
        public SetupView setup(){return new SetupView(true,"map-a","Map A",administrator.toString(),field==null?"":field,false,List.of(),Map.of(),Map.of());}
        public void selectField(UUID administrator,String field,boolean area){}
        public void selectFieldEntry(UUID administrator,String field,boolean area,int index){assertEquals(this.administrator,administrator);if(index<0)throw new IllegalArgumentException("index");this.field=field;this.index=index;}
        public void recordClick(UUID administrator,BlockPoint point,boolean secondaryCorner){}public MapProfile saveSetup(String sessionId){throw new UnsupportedOperationException();}public void discardSetup(String sessionId){}
    }
}
