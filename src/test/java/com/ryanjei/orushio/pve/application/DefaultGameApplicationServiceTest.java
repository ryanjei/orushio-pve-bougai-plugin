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
    private static final class MemoryRepository implements ActiveSessionRepository {
        GameSession session;
        public Optional<GameSession> load() { return Optional.ofNullable(session); }
        public void save(GameSession value) { session = value; }
    }
}
