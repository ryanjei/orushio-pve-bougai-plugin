package com.ryanjei.orushio.pve.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class GameSession {
    private final UUID sessionId;
    private final GameState state;
    private final List<Participant> participants;
    private final Instant createdAt;

    public GameSession(UUID sessionId, GameState state, List<Participant> participants, Instant createdAt) {
        this.sessionId = Objects.requireNonNull(sessionId);
        this.state = Objects.requireNonNull(state);
        this.participants = List.copyOf(participants);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static GameSession idle() {
        return new GameSession(UUID.randomUUID(), GameState.IDLE, List.of(), Instant.now());
    }

    public GameSession transitionedTo(GameState next) {
        if (!state.canTransitionTo(next)) {
            throw new DomainException("GAME_STATE_CONFLICT", "現在の状態「" + state + "」から「" + next + "」へ変更できません。");
        }
        return new GameSession(sessionId, next, participants, createdAt);
    }

    public static GameSession newIdle() { return idle(); }

    public UUID sessionId() { return sessionId; }
    public GameState state() { return state; }
    public List<Participant> participants() { return participants; }
    public Instant createdAt() { return createdAt; }
}
