package com.ryanjei.orushio.pve.map;

import static org.junit.jupiter.api.Assertions.*;
import com.ryanjei.orushio.pve.domain.*;
import com.ryanjei.orushio.pve.logging.AuditSink;
import com.ryanjei.orushio.pve.persistence.ActiveSessionRepository;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MapSetupStartupConsistencyTest {
    @TempDir Path temp;

    @Test void MAP_SETUPで生成物が全てなければ待機状態へ自己修復する() {
        MemorySessions sessions=new MemorySessions(mapSetup());RecordingAudit audit=new RecordingAudit();
        var result=MapSetupStartupConsistency.inspectAndRepair(sessions.value,manager(),sessions,evidence(),audit);
        assertTrue(result.repaired());assertFalse(result.recoveryRequired());assertEquals(GameState.IDLE,result.session().state());
        assertEquals(GameState.IDLE,sessions.value.state());assertTrue(audit.codes.contains("ORPHAN_MAP_SETUP_REPAIRED"));
    }

    @Test void MAP_SETUPでownership付きsetupWorldがあれば保存も削除もしない()throws Exception{
        TemporaryWorldManager worlds=manager();MapProfile profile=profile();GameSession setup=mapSetup();var owned=worlds.create(profile,"setup",setup.sessionId().toString());MapSetupEvidenceRepository evidence=evidence();evidence.save(new MapSetupEvidenceRepository.Evidence(setup.sessionId().toString(),profile.mapId().value(),UUID.randomUUID(),owned.worldName(),owned.ownershipId()));
        MemorySessions sessions=new MemorySessions(setup);
        var result=MapSetupStartupConsistency.inspectAndRepair(setup,worlds,sessions,evidence,new RecordingAudit());
        assertFalse(result.repaired());assertTrue(result.recoveryRequired());assertSame(setup,sessions.value);assertTrue(Files.exists(owned.directory()));assertTrue(worlds.recoveryRequired());
        worlds.prepareStartupRecovery("",false,true);worlds.recoverPreparedWorlds();assertTrue(Files.exists(owned.directory()));
    }

    @Test void 待機状態にorphanSetupWorldがあれば新規作成を禁止し自動削除しない()throws Exception{
        TemporaryWorldManager worlds=manager();var owned=worlds.create(profile(),"setup",UUID.randomUUID().toString());GameSession idle=GameSession.idle();MemorySessions sessions=new MemorySessions(idle);
        var result=MapSetupStartupConsistency.inspectAndRepair(idle,worlds,sessions,evidence(),new RecordingAudit());
        assertTrue(result.recoveryRequired());assertEquals(GameState.IDLE,sessions.value.state());assertTrue(Files.exists(owned.directory()));
        assertThrows(MapIoException.class,()->worlds.create(profile(),"run",UUID.randomUUID().toString()));
    }

    @Test void 通常のPhase4状態とrunWorldはMapSetup整合性検査の対象外()throws Exception{
        for(GameState state:List.of(GameState.IDLE,GameState.PREPARING,GameState.ACTIVE,GameState.RECOVERING)){
            TemporaryWorldManager worlds=manager();GameSession session=game(state);MemorySessions sessions=new MemorySessions(session);
            if(state!=GameState.IDLE)worlds.create(profile(),"run",session.sessionId().toString());
            var result=MapSetupStartupConsistency.inspectAndRepair(session,worlds,sessions,evidence(),new RecordingAudit());
            assertFalse(result.repaired());assertFalse(result.recoveryRequired());assertSame(session,sessions.value);
        }
    }

    @Test void MAP_SETUPで保護対象Draft証跡だけが残る場合も自動修復しない(){GameSession setup=mapSetup();MapSetupEvidenceRepository evidence=evidence();evidence.save(new MapSetupEvidenceRepository.Evidence(setup.sessionId().toString(),"map-a",UUID.randomUUID(),"orushio_setup_missing","owner"));MemorySessions sessions=new MemorySessions(setup);TemporaryWorldManager worlds=manager();var result=MapSetupStartupConsistency.inspectAndRepair(setup,worlds,sessions,evidence,new RecordingAudit());assertFalse(result.repaired());assertTrue(result.recoveryRequired());assertEquals(GameState.MAP_SETUP,sessions.value.state());assertTrue(worlds.recoveryRequired());}

    private TemporaryWorldManager manager(){return new TemporaryWorldManager(temp.resolve("maps"),temp.resolve("worlds"));}
    private MapSetupEvidenceRepository evidence(){return new MapSetupEvidenceRepository(temp.resolve("sessions/map-setup.yml"));}
    private MapProfile profile()throws Exception{Path template=temp.resolve("maps/map-a/template");Files.createDirectories(template);Files.writeString(template.resolve("level.dat"),"original");return MapProfileTest.empty("map-a");}
    private static GameSession mapSetup(){return new GameSession(UUID.randomUUID(),GameState.MAP_SETUP,List.of(),Instant.now());}
    private static GameSession game(GameState state){Instant now=Instant.now();if(state==GameState.IDLE)return GameSession.idle();GameSession value=new GameSession(UUID.randomUUID(),GameState.PREPARING,List.of(),now,"map-a",0,60,2,1,null,null,Set.of());if(state==GameState.PREPARING)return value;if(state==GameState.ACTIVE)value=new GameSession(value.sessionId(),GameState.ACTIVE,List.of(),now,"map-a",0,60,2,1,now,now.plusSeconds(60),Set.of());else value=value.transitionedTo(GameState.ABORTING).transitionedTo(GameState.RECOVERING);return state==GameState.RECOVERING?value:value;}
    private static final class MemorySessions implements ActiveSessionRepository{GameSession value;MemorySessions(GameSession value){this.value=value;}public Optional<GameSession>load(){return Optional.ofNullable(value);}public void save(GameSession value){this.value=value;}}
    private static final class RecordingAudit implements AuditSink{final List<String>codes=new ArrayList<>();public void record(String a,String b,String code,String d){codes.add(code);}public boolean healthy(){return true;}}
}
