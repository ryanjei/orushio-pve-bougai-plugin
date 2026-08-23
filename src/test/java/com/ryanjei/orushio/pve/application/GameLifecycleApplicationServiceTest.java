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

    @Test void startViewは有効マップを検証しoffline参加者も保持して開始できる() {
        var service=service();
        assertFalse(service.startView("map-a").ready());
        service.addParticipant(alice);
        assertTrue(service.startView("map-a").ready());
        service.playerDisconnected(alice); online.clear();
        assertTrue(service.startView("map-a").ready());
    }

    @Test void Readiness不足はまとめて表示しRuntime準備前に拒否する(){RecordingStep runtime=new RecordingStep(false,false);GameReadinessValidator validator=(map,launch)->List.of("資源採取エリアのPoint・再生成設定がありません。","敵出現エリアの敵・出現間隔設定がありません。");var service=new DefaultGameApplicationService(sessions,GameSession.idle(),()->List.copyOf(online),maps,settings,List.of(runtime),new AuditSink(){public void record(String a,String b,String c,String d){}public boolean healthy(){return true;}},ParticipantPolicy.standard(),validator);service.addParticipant(alice);GameStartView view=service.startView("map-a");assertFalse(view.ready());assertEquals(2,view.missing().size());assertThrows(DomainException.class,()->service.prepareGame("IDLE","map-a"));assertEquals(0,runtime.resources);assertEquals(GameState.IDLE,service.current().state());}

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

    @Test void ACTIVE保存後のpreparationCommit失敗は必ずRECOVERINGへ隔離する(){CommitFailureStep step=new CommitFailureStep();var service=service(List.of(step),ParticipantPolicy.standard());service.addParticipant(alice);service.prepareGame("IDLE","map-a");RuntimeException failure=assertThrows(RuntimeException.class,()->service.activateGame(service.current().sessionId().toString(),Instant.now()));assertEquals("commit",failure.getMessage());assertEquals(GameState.RECOVERING,service.current().state());assertTrue(auditCodes.contains("GAME_PREPARATION_COMMIT_FAILED"));assertTrue(auditCodes.contains("GAME_PREPARATION_RECOVERY_REQUIRED"));step.failCommit=false;service.abortGame(service.current().sessionId().toString(),"RECOVERY");assertEquals(GameState.IDLE,service.current().state());}

    @Test void 参加上限Policyを8へ差替えるとApplication変更なしで8人まで扱える(){
        for(int i=0;i<8;i++)online.add(new OnlinePlayerView(UUID.randomUUID(),"X"+i));var service=service(List.of(),new ParticipantPolicy(8));online.stream().limit(8).forEach(p->service.addParticipant(p.uuid()));assertEquals(8,service.current().participants().size());assertEquals(8,service.participantLimit());service.prepareGame("IDLE","map-a");assertEquals(8,service.current().participantCountAtStart());
    }

    @Test void 標準Policyでは1人から4人まで開始でき5人目を拒否する(){for(int count=1;count<=4;count++){online.clear();for(int i=0;i<count;i++)online.add(new OnlinePlayerView(UUID.randomUUID(),"P"+i));var service=service();online.forEach(p->service.addParticipant(p.uuid()));assertTrue(service.startView("map-a").ready());service.prepareGame("IDLE","map-a");assertEquals(count,service.current().participantCountAtStart());}online.clear();for(int i=0;i<5;i++)online.add(new OnlinePlayerView(UUID.randomUUID(),"Q"+i));var service=service();online.stream().limit(4).forEach(p->service.addParticipant(p.uuid()));assertThrows(DomainException.class,()->service.addParticipant(online.get(4).uuid()));assertEquals(4,service.participantLimit());}

    @Test void ACTIVE中のquit即joinも参加情報と開始時人数を維持する(){var service=service();service.addParticipant(alice);service.prepareGame("IDLE","map-a");service.activateGame(service.current().sessionId().toString(),Instant.now());try(var dispatcher=new ParticipantConnectionDispatcher(service,failure->fail(failure))){dispatcher.disconnected(alice);dispatcher.connected(alice,"Alice2");}assertEquals(GameState.ACTIVE,service.current().state());assertEquals(1,service.current().participantCountAtStart());assertEquals(1,service.current().participants().size());assertTrue(service.current().participants().getFirst().connected());assertEquals("Alice2",service.current().participants().getFirst().lastKnownName());}

    @Test void ACTIVE再接続は全Step成功後だけconnected確定しcommitする(){List<String>events=new ArrayList<>();ReconnectStep inventory=new ReconnectStep("inventory",events),runtime=new ReconnectStep("runtime",events);var service=service(List.of(inventory,runtime),ParticipantPolicy.standard());service.addParticipant(alice);service.prepareGame("IDLE","map-a");service.activateGame(service.current().sessionId().toString(),Instant.now());service.playerDisconnected(alice);service.playerConnected(alice,"Alice2");assertTrue(service.current().participants().getFirst().connected());assertEquals(List.of("connect-inventory","connect-runtime","commit-inventory","commit-runtime"),events);}

    @Test void ACTIVE再接続Runtime失敗は逆順rollbackしconnectedを確定しない(){List<String>events=new ArrayList<>();ReconnectStep inventory=new ReconnectStep("inventory",events),runtime=new ReconnectStep("runtime",events);runtime.failConnect=true;var service=service(List.of(inventory,runtime),ParticipantPolicy.standard());service.addParticipant(alice);service.prepareGame("IDLE","map-a");service.activateGame(service.current().sessionId().toString(),Instant.now());service.playerDisconnected(alice);assertThrows(RuntimeException.class,()->service.playerConnected(alice,"Alice2"));assertFalse(service.current().participants().getFirst().connected());assertEquals(List.of("connect-inventory","connect-runtime","rollback-runtime","rollback-inventory"),events);}

    @Test void ACTIVE再接続rollback失敗はRECOVERINGへ隔離する(){List<String>events=new ArrayList<>();ReconnectStep inventory=new ReconnectStep("inventory",events),runtime=new ReconnectStep("runtime",events);runtime.failConnect=true;inventory.failRollback=true;var service=service(List.of(inventory,runtime),ParticipantPolicy.standard());service.addParticipant(alice);service.prepareGame("IDLE","map-a");service.activateGame(service.current().sessionId().toString(),Instant.now());service.playerDisconnected(alice);assertThrows(RuntimeException.class,()->service.playerConnected(alice,"Alice"));assertEquals(GameState.RECOVERING,service.current().state());assertFalse(service.current().participants().getFirst().connected());assertTrue(auditCodes.contains("PARTICIPANT_RECONNECT_RECOVERY_REQUIRED"));}

    @Test void participant再接続だけRuntimeへ戻しpendingCleanupはロビー復帰後に消化する(){ConnectionStep step=new ConnectionStep();var service=service(List.of(step),ParticipantPolicy.standard());service.addParticipant(alice);service.prepareGame("IDLE","map-a");service.activateGame(service.current().sessionId().toString(),Instant.now());service.playerDisconnected(alice);service.playerConnected(alice,"Alice");assertEquals(List.of(alice),step.runtimeConnections);service.playerDisconnected(alice);service.abortGame(service.current().sessionId().toString(),"ADMIN");assertTrue(service.current().pendingCleanup().contains(alice));service.playerConnected(alice,"Alice");assertEquals(List.of(alice),step.lobbyConnections);assertFalse(service.current().pendingCleanup().contains(alice));UUID unrelated=UUID.randomUUID();service.playerConnected(unrelated,"Other");assertEquals(1,step.runtimeConnections.size());}

    @Test void pendingCleanup中Playerは新Sessionへ追加できない(){GameSession initial=GameSession.idleWithPending(Set.of(alice));var service=new DefaultGameApplicationService(sessions,initial,()->List.copyOf(online),maps,settings,List.of(),null);DomainException failure=assertThrows(DomainException.class,()->service.addParticipant(alice));assertEquals("PLAYER_CLEANUP_REQUIRED",failure.code());}

    @Test void Paper直前raceのcleanup結果をactiveSession_pendingへ反映する(){UUID bob=UUID.randomUUID();online.add(new OnlinePlayerView(bob,"Bob"));CleanupResultStep step=new CleanupResultStep(Set.of(alice),Set.of(bob));var service=service(List.of(step),ParticipantPolicy.standard());service.addParticipant(alice);service.addParticipant(bob);service.prepareGame("IDLE","map-a");service.abortGame(service.current().sessionId().toString(),"ADMIN");assertEquals(Set.of(alice),service.current().pendingCleanup());}

    @Test void cleanupOrderによりEntity回収をWorld回収より先に実行する(){List<Integer>events=new ArrayList<>();GameLifecycleStep world=new OrderedCleanupStep(100,events),entity=new OrderedCleanupStep(20,events),player=new OrderedCleanupStep(0,events);var service=service(List.of(world,entity,player),ParticipantPolicy.standard());service.addParticipant(alice);service.prepareGame("IDLE","map-a");service.abortGame(service.current().sessionId().toString(),"ADMIN");assertEquals(List.of(0,20,100),events);}

    @Test void FinalCore通知はACTIVEからCLEARへ一度だけ遷移し共通RecoveryでIDLEへ進む(){ClearStep step=new ClearStep();var service=service(List.of(step),ParticipantPolicy.standard());service.addParticipant(alice);service.prepareGame("IDLE","map-a");service.activateGame(service.current().sessionId().toString(),Instant.now());UUID id=service.current().sessionId();service.completeGame(id);service.completeGame(id);assertEquals(GameState.CLEAR,service.current().state());assertEquals(1,step.presented);service.recoverClearGame(id);assertEquals(GameState.IDLE,service.current().state());assertEquals(1,step.cleaned);assertTrue(auditCodes.contains("GAME_CLEAR_STARTED"));}

    @Test void CLEAR表示失敗でもRecoveryを継続できる(){ClearStep step=new ClearStep();step.failPresentation=true;var service=service(List.of(step),ParticipantPolicy.standard());service.addParticipant(alice);service.prepareGame("IDLE","map-a");service.activateGame(service.current().sessionId().toString(),Instant.now());UUID id=service.current().sessionId();assertDoesNotThrow(()->service.completeGame(id));assertEquals(GameState.CLEAR,service.current().state());service.recoverClearGame(id);assertEquals(GameState.IDLE,service.current().state());assertTrue(auditCodes.contains("GAME_CLEAR_PRESENTATION_FAILED"));}

    @Test void CLEAR_cleanup失敗はRECOVERINGを維持し管理中止と同じpipelineで再試行する(){RecordingStep step=new RecordingStep(false,true);var service=service(List.of(step),ParticipantPolicy.standard());service.addParticipant(alice);service.prepareGame("IDLE","map-a");service.activateGame(service.current().sessionId().toString(),Instant.now());UUID id=service.current().sessionId();service.completeGame(id);assertThrows(RuntimeException.class,()->service.recoverClearGame(id));assertEquals(GameState.RECOVERING,service.current().state());step.failCleanup=false;service.abortGame(id.toString(),"RECOVERY");assertEquals(GameState.IDLE,service.current().state());}

    @Test void 死亡とRespawnはACTIVEのCurrentParticipantかつCurrentRuntimeだけに限定する(){DeathStep step=new DeathStep("run-a");var service=service(List.of(step),ParticipantPolicy.standard());service.addParticipant(alice);service.prepareGame("IDLE","map-a");service.activateGame(service.current().sessionId().toString(),Instant.now());assertTrue(service.playerDied(alice,"run-a"));assertFalse(service.playerDied(alice,"other"));assertFalse(service.playerDied(UUID.randomUUID(),"run-a"));assertTrue(service.playerRespawned(alice));assertEquals(List.of(alice),step.respawned);UUID id=service.current().sessionId();service.completeGame(id);assertFalse(service.playerRespawned(alice));}

    @Test void 切断はmembershipと開始人数を維持しUI解除hookへ通知する(){DisconnectStep step=new DisconnectStep();var service=service(List.of(step),ParticipantPolicy.standard());service.addParticipant(alice);service.prepareGame("IDLE","map-a");service.activateGame(service.current().sessionId().toString(),Instant.now());service.playerDisconnected(alice);assertEquals(1,service.current().participants().size());assertEquals(1,service.current().participantCountAtStart());assertFalse(service.current().participants().getFirst().connected());assertEquals(List.of(alice),step.disconnected);}

    private DefaultGameApplicationService service(){return service(List.of(),ParticipantPolicy.standard());}
    private DefaultGameApplicationService service(List<GameLifecycleStep> steps,ParticipantPolicy policy){return new DefaultGameApplicationService(sessions,GameSession.idle(),()->List.copyOf(online),maps,settings,steps,new AuditSink(){public void record(String trace,String category,String code,String operation){auditCodes.add(code);}public boolean healthy(){return true;}},policy);}
    private static MapProfile completeMap(){Map<String,List<BlockPoint>> p=new HashMap<>();for(String k:MapProfile.REQUIRED_SINGLE_POINTS)p.put(k,List.of(point()));p.put("normalCoreCandidates",List.of(point(),point(),point()));p.put("shopPoints",List.of(point()));Map<String,List<Cuboid>> a=new HashMap<>();for(String k:MapProfile.REQUIRED_AREA_COUNTS.keySet())a.put(k,List.of(new Cuboid(0,0,0,1,1,1)));return new MapProfile(new MapProfileId("map-a"),"Map A",true,"original",p,a,Instant.now());}
    private static BlockPoint point(){return new BlockPoint(0,64,0,0,0);}
    private static final class MemoryActive implements ActiveSessionRepository{GameSession value;public Optional<GameSession> load(){return Optional.ofNullable(value);}public void save(GameSession value){this.value=value;}}
    private static final class MemorySettings implements GameLaunchSettingsRepository{GameLaunchSettings value=GameLaunchSettings.defaults();public GameLaunchSettings load(String ignored){return value;}public void save(String ignored,GameLaunchSettings value){this.value=value;}}
    private static final class RecordingStep implements GameLifecycleStep{boolean failPrepare,failCleanup;int resources;RecordingStep(boolean failPrepare,boolean failCleanup){this.failPrepare=failPrepare;this.failCleanup=failCleanup;}public void prepare(GameSession ignored){resources++;if(failPrepare)throw new RuntimeException("prepare");}public void rollbackPreparation(GameSession ignored){if(failCleanup)throw new RuntimeException("rollback");resources--;}public void cleanup(GameSession ignored){if(failCleanup)throw new RuntimeException("cleanup");resources=0;}}
    private static final class CommitFailureStep implements GameLifecycleStep{boolean failCommit=true;public void preparationCommitted(GameSession ignored){if(failCommit)throw new RuntimeException("commit");}}
    private static final class ReconnectStep implements GameLifecycleStep{final String name;final List<String>events;boolean failConnect,failRollback;ReconnectStep(String name,List<String>events){this.name=name;this.events=events;}public void participantConnected(GameSession ignored,UUID id){events.add("connect-"+name);if(failConnect)throw new RuntimeException("connect");}public void rollbackParticipantConnection(GameSession ignored,UUID id){events.add("rollback-"+name);if(failRollback)throw new RuntimeException("rollback");}public void participantConnectionCommitted(GameSession ignored,UUID id){events.add("commit-"+name);}}
    private static final class ConnectionStep implements GameLifecycleStep{final List<UUID>runtimeConnections=new ArrayList<>(),lobbyConnections=new ArrayList<>();public void prepare(GameSession ignored){}public void rollbackPreparation(GameSession ignored){}public void cleanup(GameSession ignored){}public void participantConnected(GameSession ignored,UUID id){runtimeConnections.add(id);}public boolean pendingCleanupConnected(GameSession ignored,UUID id){lobbyConnections.add(id);return true;}}
    private record CleanupResultStep(Set<UUID>pending,Set<UUID>completed)implements GameLifecycleStep{public Set<UUID>pendingCleanupPlayers(GameSession ignored){return pending;}public Set<UUID>completedCleanupPlayers(GameSession ignored){return completed;}}
    private record OrderedCleanupStep(int cleanupOrder,List<Integer>events)implements GameLifecycleStep{public void cleanup(GameSession ignored){events.add(cleanupOrder);}}
    private static final class ClearStep implements GameLifecycleStep{int presented,cleaned;boolean failPresentation;public void clearStarted(GameSession ignored){presented++;if(failPresentation)throw new RuntimeException("title");}public void cleanup(GameSession ignored){cleaned++;}}
    private static final class DeathStep implements GameLifecycleStep{final String world;final List<UUID>respawned=new ArrayList<>();DeathStep(String world){this.world=world;}public boolean ownsRuntime(GameSession ignored,String candidate){return world.equals(candidate);}public void participantRespawned(GameSession ignored,UUID id){respawned.add(id);}}
    private static final class DisconnectStep implements GameLifecycleStep{final List<UUID>disconnected=new ArrayList<>();public void participantDisconnected(GameSession ignored,UUID id){disconnected.add(id);}}
}
