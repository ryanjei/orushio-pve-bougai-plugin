package com.ryanjei.orushio.pve.application;

import static org.junit.jupiter.api.Assertions.*;
import com.ryanjei.orushio.pve.domain.DomainException;
import com.ryanjei.orushio.pve.domain.GameSession;
import com.ryanjei.orushio.pve.persistence.ActiveSessionRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DefaultGameApplicationServiceTest {
    @Test void 期待状態を検証して受付を開始する() {
        MemoryRepository repository = new MemoryRepository();
        var service = new DefaultGameApplicationService(repository);
        assertEquals("RECRUITING", service.startRecruiting("IDLE").state());
        assertNotNull(repository.session);
        assertThrows(DomainException.class, () -> service.startRecruiting("IDLE"));
    }
    @Test void 受付開始の保存失敗でメモリ状態を進めず再試行できる() {
        FailingRepository repository=new FailingRepository(); var service=new DefaultGameApplicationService(repository);
        var before=service.current(); repository.fail=true;
        assertThrows(RuntimeException.class,()->service.startRecruiting("IDLE"));
        assertEquals("IDLE",service.current().state().name()); assertEquals(before.sessionId(),service.current().sessionId());
        repository.fail=false; assertEquals("RECRUITING",service.startRecruiting("IDLE").state());
    }
    @Test void 受付終了の保存失敗で元セッションを維持し成功時は一度だけ保存する() {
        FailingRepository repository=new FailingRepository(); var service=new DefaultGameApplicationService(repository);
        service.startRecruiting("IDLE"); var before=service.current(); repository.fail=true;
        assertThrows(RuntimeException.class,()->service.closeRecruiting(before.sessionId().toString()));
        assertEquals("RECRUITING",service.current().state().name()); assertEquals(before.sessionId(),service.current().sessionId());
        repository.fail=false; int saves=repository.saves; assertEquals("IDLE",service.closeRecruiting(before.sessionId().toString()).state()); assertEquals(saves+1,repository.saves);
    }
    @Test void sessionId不一致を拒否する(){var service=new DefaultGameApplicationService(new MemoryRepository());service.startRecruiting("IDLE");assertThrows(DomainException.class,()->service.closeRecruiting(java.util.UUID.randomUUID().toString()));}
    private static final class MemoryRepository implements ActiveSessionRepository {
        GameSession session;
        public Optional<GameSession> load() { return Optional.ofNullable(session); }
        public void save(GameSession value) { session = value; }
    }
    private static final class FailingRepository implements ActiveSessionRepository { GameSession session; boolean fail; int saves; public Optional<GameSession> load(){return Optional.ofNullable(session);} public void save(GameSession value){if(fail)throw new RuntimeException("disk");session=value;saves++;} }
}
