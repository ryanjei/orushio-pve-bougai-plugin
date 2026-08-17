package com.ryanjei.orushio.pve.application;

import com.ryanjei.orushio.pve.domain.*;
import com.ryanjei.orushio.pve.map.*;
import com.ryanjei.orushio.pve.persistence.*;
import com.ryanjei.orushio.pve.logging.AuditSink;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GameLifecycleApplicationServiceTest {
    private final UUID alice = UUID.randomUUID();
    private final MemoryActive sessions = new MemoryActive();
    private final MemorySettings settings = new MemorySettings();
    private final List<String> auditCodes = new ArrayList<>();
    private final List<OnlinePlayerView> online = new ArrayList<>(List.of(new OnlinePlayerView(alice, "Alice")));
    private final MapProfile profile = completeMap();
    private final MapProfileRepository maps = new MapProfileRepository() {
        public List<MapProfile> findAll() { return List.of(profile); }
        public Optional<MapProfile> find(MapProfileId id) { return id.equals(profile.mapId()) ? Optional.of(profile) : Optional.empty(); }
        public void save(MapProfile ignored) { }
        public void delete(MapProfileId ignored) { }
    };

    @Test void online参加者だけを選択し重複追加は冪等で上限を守る() {
        var service = service();
        assertEquals(1, service.addParticipant(alice).participants().size());
        assertEquals(1, service.addParticipant(alice).participants().size());
        assertThrows(DomainException.class, () -> service.addParticipant(UUID.randomUUID()));
        for (int i=0;i<4;i++) online.add(new OnlinePlayerView(UUID.randomUUID(), "P"+i));
        online.stream().skip(1).limit(3).forEach(p -> service.addParticipant(p.uuid()));
        assertThrows(DomainException.class, () -> service.addParticipant(online.get(4).uuid()));
    }

    @Test void startViewは有効マップと接続中参加者を検証する() {
        var service=service();
        assertFalse(service.startView("map-a").ready());
        service.addParticipant(alice);
        assertTrue(service.startView("map-a").ready());
        service.playerDisconnected(alice); online.clear();
        assertFalse(service.startView("map-a").ready());
    }

    @Test void 準備中を明示的に経由して開始時人数とサーバー時刻を固定する() {
        var service=service(); service.addParticipant(alice);
        service.saveLaunchSettings("map-a", new GameLaunchSettings(Optional.of(30), Optional.of(4), Optional.of(1.5)));
        service.prepareGame("IDLE", "map-a");
        assertEquals(GameState.PREPARING,service.current().state());
        assertEquals(1,service.current().participantCountAtStart());
        Instant now=Instant.parse("2026-08-18T00:00:00Z");
        service.activateGame(service.current().sessionId().toString(),now);
        assertEquals(GameState.ACTIVE,service.current().state());
        assertEquals(now.plusSeconds(1800),service.current().endsAt().orElseThrow());
        assertThrows(DomainException.class,()->service.addParticipant(UUID.randomUUID()));
    }

    @Test void 切断者を参加者として保持し中止後にpendingCleanupへ残す() {
        var service=service(); service.addParticipant(alice);service.prepareGame("IDLE","map-a");
        service.playerDisconnected(alice);
        assertEquals(1,service.current().participants().size());assertFalse(service.current().participants().getFirst().connected());
        service.abortGame(service.current().sessionId().toString(),"ADMIN");
        assertEquals(GameState.IDLE,service.current().state());assertTrue(service.current().pendingCleanup().contains(alice));
    }

    @Test void 制限時間前は継続し期限到達後だけ安全中止する() {
        var service=service();service.addParticipant(alice);service.prepareGame("IDLE","map-a");
        Instant now=Instant.parse("2026-08-18T00:00:00Z");service.activateGame(service.current().sessionId().toString(),now);
        assertFalse(service.expireIfNeeded(now.plusSeconds(59*60)));
        assertTrue(service.expireIfNeeded(now.plusSeconds(60*60)));
        assertEquals(GameState.IDLE,service.current().state());
    }

    @Test void prepare途中失敗は失敗stepを含め逆順補償し再試行で二重生成しない(){
        var first=new RecordingStep(false,false);var second=new RecordingStep(true,false);var service=service(List.of(first,second),ParticipantPolicy.standard());service.addParticipant(alice);service.prepareGame("IDLE","map-a");String id=service.current().sessionId().toString();
        assertThrows(RuntimeException.class,()->service.activateGame(id,Instant.now()));assertEquals(GameState.PREPARING,service.current().state());assertEquals(0,first.resources);assertEquals(0,second.resources);
        assertThrows(RuntimeException.class,()->service.activateGame(id,Instant.now()));assertEquals(0,first.resources);assertEquals(0,second.resources);
    }

    @Test void prepare補償不能とcleanup失敗はRECOVERINGへ隔離して再試行できる(){
        var step=new RecordingStep(true,true);var service=service(List.of(step),ParticipantPolicy.standard());service.addParticipant(alice);service.prepareGame("IDLE","map-a");String id=service.current().sessionId().toString();
        assertThrows(RuntimeException.class,()->service.activateGame(id,Instant.now()));assertEquals(GameState.RECOVERING,service.current().state());assertTrue(auditCodes.contains("GAME_PREPARATION_RECOVERY_REQUIRED"));
        step.failPrepare=false;assertThrows(RuntimeException.class,()->service.abortGame(id,"RECOVERY"));assertEquals(GameState.RECOVERING,service.current().state());assertTrue(auditCodes.contains("GAME_CLEANUP_FAILED"));
        step.failCleanup=false;service.abortGame(id,"RECOVERY");assertEquals(GameState.IDLE,service.current().state());
    }

    @Test void 参加上限Policyを8へ差替えるとApplication変更なしで8人まで扱える(){
        for(int i=0;i<8;i++)online.add(new OnlinePlayerView(UUID.randomUUID(),"X"+i));var service=service(List.of(),new ParticipantPolicy(8));online.stream().limit(8).forEach(p->service.addParticipant(p.uuid()));assertEquals(8,service.current().participants().size());assertEquals(8,service.participantLimit());service.prepareGame("IDLE","map-a");assertEquals(8,service.current().participantCountAtStart());
    }

    @Test void 標準Policyでは1人から4人まで開始でき5人目を拒否する(){for(int count=1;count<=4;count++){online.clear();for(int i=0;i<count;i++)online.add(new OnlinePlayerView(UUID.randomUUID(),"P"+i));var service=service();online.forEach(p->service.addParticipant(p.uuid()));assertTrue(service.startView("map-a").ready());service.prepareGame("IDLE","map-a");assertEquals(count,service.current().participantCountAtStart());}online.clear();for(int i=0;i<5;i++)online.add(new OnlinePlayerView(UUID.randomUUID(),"Q"+i));var service=service();online.stream().limit(4).forEach(p->service.addParticipant(p.uuid()));assertThrows(DomainException.class,()->service.addParticipant(online.get(4).uuid()));assertEquals(4,service.participantLimit());}

    @Test void ACTIVE中のquit即joinも参加情報と開始時人数を維持する(){var service=service();service.addParticipant(alice);service.prepareGame("IDLE","map-a");service.activateGame(service.current().sessionId().toString(),Instant.now());try(var dispatcher=new ParticipantConnectionDispatcher(service,failure->fail(failure))){dispatcher.disconnected(alice);dispatcher.connected(alice,"Alice2");}assertEquals(GameState.ACTIVE,service.current().state());assertEquals(1,service.current().participantCountAtStart());assertEquals(1,service.current().participants().size());assertTrue(service.current().participants().getFirst().connected());assertEquals("Alice2",service.current().participants().getFirst().lastKnownName());}

    private DefaultGameApplicationService service(){return service(List.of(),ParticipantPolicy.standard());}
    private DefaultGameApplicationService service(List<GameLifecycleStep> steps,ParticipantPolicy policy){return new DefaultGameApplicationService(sessions,GameSession.idle(),()->List.copyOf(online),maps,settings,steps,new AuditSink(){public void record(String trace,String category,String code,String operation){auditCodes.add(code);}public boolean healthy(){return true;}},policy);}
    private static MapProfile completeMap(){Map<String,List<BlockPoint>> p=new HashMap<>();for(String k:MapProfile.REQUIRED_SINGLE_POINTS)p.put(k,List.of(point()));p.put("normalCoreCandidates",List.of(point(),point(),point()));p.put("shopPoints",List.of(point()));Map<String,List<Cuboid>> a=new HashMap<>();for(String k:MapProfile.REQUIRED_AREA_COUNTS.keySet())a.put(k,List.of(new Cuboid(0,0,0,1,1,1)));return new MapProfile(new MapProfileId("map-a"),"Map A",true,"original",p,a,Instant.now());}
    private static BlockPoint point(){return new BlockPoint(0,64,0,0,0);}
    private static final class MemoryActive implements ActiveSessionRepository{GameSession value;public Optional<GameSession> load(){return Optional.ofNullable(value);}public void save(GameSession value){this.value=value;}}
    private static final class MemorySettings implements GameLaunchSettingsRepository{GameLaunchSettings value=GameLaunchSettings.defaults();public GameLaunchSettings load(String ignored){return value;}public void save(String ignored,GameLaunchSettings value){this.value=value;}}
    private static final class RecordingStep implements GameLifecycleStep{boolean failPrepare,failCleanup;int resources;RecordingStep(boolean failPrepare,boolean failCleanup){this.failPrepare=failPrepare;this.failCleanup=failCleanup;}public void prepare(GameSession ignored){resources++;if(failPrepare)throw new RuntimeException("prepare");}public void rollbackPreparation(GameSession ignored){if(failCleanup)throw new RuntimeException("rollback");resources--;}public void cleanup(GameSession ignored){if(failCleanup)throw new RuntimeException("cleanup");resources=0;}}
}
