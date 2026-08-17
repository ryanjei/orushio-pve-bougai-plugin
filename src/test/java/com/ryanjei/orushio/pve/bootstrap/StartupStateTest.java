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
    @Test void 進行中activeSessionは勝手に待機へ戻さず復旧診断モードになる(){Path path=temp.resolve("sessions/active.yml");var repository=new YamlActiveSessionRepository(path);var session=GameSession.idle().transitionedTo(com.ryanjei.orushio.pve.domain.GameState.PREPARING);repository.save(session);StartupState startup=StartupState.load(temp);assertTrue(startup.diagnosticMode());assertTrue(startup.recoveryRequired());assertEquals("PREPARING",startup.session().orElseThrow().state().name());assertTrue(startup.warnings().stream().anyMatch(value->value.contains("復旧")));}
}
