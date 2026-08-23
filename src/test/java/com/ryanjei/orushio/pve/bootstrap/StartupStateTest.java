package com.ryanjei.orushio.pve.bootstrap;

import static org.junit.jupiter.api.Assertions.*;
import com.ryanjei.orushio.pve.application.DefaultGameApplicationService;
import com.ryanjei.orushio.pve.domain.GameSession;
import com.ryanjei.orushio.pve.persistence.YamlActiveSessionRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StartupStateTest {
    @TempDir Path temp;
    @Test void 正常なactiveSessionは通常モードで再読込み後も受付状態を維持する() {
        Path path=temp.resolve("sessions/active.yml");var repository=new YamlActiveSessionRepository(path);var service=new DefaultGameApplicationService(repository);
        service.startRecruiting("IDLE");StartupState startup=StartupState.load(temp);
        assertTrue(startup.sessionLoaded());assertFalse(startup.recoveryRequired());assertFalse(startup.diagnosticMode());assertEquals("RECRUITING",startup.session().orElseThrow().state().name());
        assertEquals("RECRUITING",new DefaultGameApplicationService(repository).current().state().name());
    }
    @Test void 破損activeSessionは変更せず診断モードになる() throws Exception {
        Path path=temp.resolve("sessions/active.yml"), backup=temp.resolve("sessions/active.yml.bak");Files.createDirectories(path.getParent());Files.writeString(path,"broken");Files.writeString(backup,"also-broken");String current=Files.readString(path),bak=Files.readString(backup);
        StartupState startup=StartupState.load(temp);assertTrue(startup.diagnosticMode());assertFalse(startup.sessionLoaded());assertTrue(startup.recoveryRequired());assertTrue(startup.warnings().stream().anyMatch(v->v.contains("復旧")));assertEquals(current,Files.readString(path));assertEquals(bak,Files.readString(backup));
        assertFalse(Files.readString(path).contains("IDLE"));
    }
    @Test void 未知schemaのactiveSessionは変更せず診断モードになる() throws Exception {
        Path path=temp.resolve("sessions/active.yml");Files.createDirectories(path.getParent());String value="schemaVersion: \"99\"\nsessionId: \"unchanged\"\n";Files.writeString(path,value);
        StartupState startup=StartupState.load(temp);assertTrue(startup.diagnosticMode());assertTrue(startup.recoveryRequired());assertEquals(value,Files.readString(path));
    }
    @Test void activeSession不在は正常なIDLE初期状態として扱う(){StartupState startup=StartupState.load(temp);assertFalse(startup.diagnosticMode());assertTrue(startup.sessionLoaded());assertTrue(startup.session().isEmpty());}
    @Test void 正常schemaの未完了SessionはHardDiagnosticと分離したRecovery状態になる(){Path path=temp.resolve("sessions/active.yml");var repository=new YamlActiveSessionRepository(path);for(var state:java.util.List.of(com.ryanjei.orushio.pve.domain.GameState.PREPARING,com.ryanjei.orushio.pve.domain.GameState.ACTIVE,com.ryanjei.orushio.pve.domain.GameState.ABORTING,com.ryanjei.orushio.pve.domain.GameState.RECOVERING)){GameSession session=session(state);repository.save(session);StartupState startup=StartupState.load(temp);assertFalse(startup.diagnosticMode());assertTrue(startup.recoverableGameSession());assertTrue(startup.recoveryRequired());assertEquals(state,startup.session().orElseThrow().state());assertTrue(startup.warnings().stream().anyMatch(value->value.contains("復旧")));}}
    @Test void 再起動時の全未完了状態をRECOVERINGへ収束する(){for(var state:new com.ryanjei.orushio.pve.domain.GameState[]{com.ryanjei.orushio.pve.domain.GameState.PREPARING,com.ryanjei.orushio.pve.domain.GameState.ACTIVE,com.ryanjei.orushio.pve.domain.GameState.PAUSED,com.ryanjei.orushio.pve.domain.GameState.CLEAR,com.ryanjei.orushio.pve.domain.GameState.ABORTING,com.ryanjei.orushio.pve.domain.GameState.RECOVERING})assertEquals(com.ryanjei.orushio.pve.domain.GameState.RECOVERING,StartupState.recoverySessionAfterRestart(recoverySession(state)).state());GameSession idle=GameSession.idle();assertSame(idle,StartupState.recoverySessionAfterRestart(idle));}
    @Test void 再起動Recoveryは全参加者をoffline化し識別情報と開始人数を維持する(){java.util.UUID connected=java.util.UUID.randomUUID(),offline=java.util.UUID.randomUUID();java.time.Instant now=java.time.Instant.now();GameSession active=new GameSession(java.util.UUID.randomUUID(),com.ryanjei.orushio.pve.domain.GameState.ACTIVE,java.util.List.of(new com.ryanjei.orushio.pve.domain.Participant(connected,"接続者",true),new com.ryanjei.orushio.pve.domain.Participant(offline,"切断者",false)),now,"map-a",2,60,2,1.0,now,now.plusSeconds(60),java.util.Set.of());GameSession recovered=StartupState.recoverySessionAfterRestart(active);assertEquals(com.ryanjei.orushio.pve.domain.GameState.RECOVERING,recovered.state());assertTrue(recovered.participants().stream().noneMatch(com.ryanjei.orushio.pve.domain.Participant::connected));assertEquals(java.util.List.of(connected,offline),recovered.participants().stream().map(com.ryanjei.orushio.pve.domain.Participant::playerUuid).toList());assertEquals(java.util.List.of("接続者","切断者"),recovered.participants().stream().map(com.ryanjei.orushio.pve.domain.Participant::lastKnownName).toList());assertEquals(2,recovered.participantCountAtStart());}
    private static GameSession session(com.ryanjei.orushio.pve.domain.GameState state){GameSession value=GameSession.idle().transitionedTo(com.ryanjei.orushio.pve.domain.GameState.PREPARING);if(state==com.ryanjei.orushio.pve.domain.GameState.PREPARING)return value;if(state==com.ryanjei.orushio.pve.domain.GameState.ACTIVE)value=value.transitionedTo(state);else value=value.transitionedTo(com.ryanjei.orushio.pve.domain.GameState.ABORTING);return state==com.ryanjei.orushio.pve.domain.GameState.RECOVERING?value.transitionedTo(state):value;}
    private static GameSession recoverySession(com.ryanjei.orushio.pve.domain.GameState state){GameSession value=GameSession.idle().transitionedTo(com.ryanjei.orushio.pve.domain.GameState.PREPARING);if(state==com.ryanjei.orushio.pve.domain.GameState.PREPARING)return value;value=value.transitionedTo(com.ryanjei.orushio.pve.domain.GameState.ACTIVE);if(state==com.ryanjei.orushio.pve.domain.GameState.ACTIVE)return value;if(state==com.ryanjei.orushio.pve.domain.GameState.PAUSED)return value.transitionedTo(state);if(state==com.ryanjei.orushio.pve.domain.GameState.CLEAR)return value.transitionedTo(state);value=value.transitionedTo(com.ryanjei.orushio.pve.domain.GameState.ABORTING);return state==com.ryanjei.orushio.pve.domain.GameState.ABORTING?value:value.transitionedTo(com.ryanjei.orushio.pve.domain.GameState.RECOVERING);}
}
