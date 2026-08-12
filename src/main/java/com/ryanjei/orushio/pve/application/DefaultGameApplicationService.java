package com.ryanjei.orushio.pve.application;

import com.ryanjei.orushio.pve.domain.DomainException;
import com.ryanjei.orushio.pve.domain.GameSession;
import com.ryanjei.orushio.pve.domain.GameState;
import com.ryanjei.orushio.pve.persistence.ActiveSessionRepository;
import java.util.UUID;

public final class DefaultGameApplicationService implements GameApplicationService {
    private final ActiveSessionRepository repository;
    private GameSession session;

    public DefaultGameApplicationService(ActiveSessionRepository repository) {
        this.repository = repository;
        this.session = repository.load().orElseGet(GameSession::idle);
    }

    public DefaultGameApplicationService(ActiveSessionRepository repository, GameSession initialSession) {
        this.repository = repository;
        this.session = java.util.Objects.requireNonNull(initialSession);
    }

    public synchronized GameSession current() { return session; }

    public synchronized OperationResult startRecruiting(String expectedState) {
        if (!session.state().name().equals(expectedState)) conflict();
        GameSession next = session.transitionedTo(GameState.RECRUITING);
        repository.save(next);
        session = next;
        return result();
    }

    public synchronized OperationResult closeRecruiting(String expectedSessionId) {
        if (!session.sessionId().toString().equals(expectedSessionId)) {
            throw new DomainException("SESSION_MISMATCH", "画面のセッションが現在のセッションと一致しません。");
        }
        session.transitionedTo(GameState.IDLE); // Domainの許可遷移を必ず検証する。
        GameSession next = GameSession.newIdle();
        repository.save(next);
        session = next;
        return result();
    }

    private void conflict() { throw new DomainException("GAME_STATE_CONFLICT", "画面の状態が最新ではありません。再読み込みしてください。"); }
    private OperationResult result() { return new OperationResult(UUID.randomUUID().toString(), session.sessionId().toString(), session.state().name()); }
}
