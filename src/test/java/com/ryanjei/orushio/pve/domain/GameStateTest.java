package com.ryanjei.orushio.pve.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GameStateTest {
    @Test void 仕様どおりの遷移だけを許可する() {
        Map<GameState, Set<GameState>> expected = Map.of(
            GameState.IDLE, Set.of(GameState.RECRUITING, GameState.MAP_SETUP),
            GameState.RECRUITING, Set.of(GameState.PREPARING, GameState.IDLE),
            GameState.PREPARING, Set.of(GameState.ACTIVE, GameState.ABORTING),
            GameState.ACTIVE, Set.of(GameState.PAUSED, GameState.CLEAR, GameState.ABORTING),
            GameState.PAUSED, Set.of(GameState.ACTIVE, GameState.ABORTING),
            GameState.CLEAR, Set.of(GameState.RECOVERING),
            GameState.ABORTING, Set.of(GameState.RECOVERING),
            GameState.RECOVERING, Set.of(GameState.IDLE),
            GameState.MAP_SETUP, Set.of(GameState.IDLE));
        for (GameState from : GameState.values()) for (GameState to : GameState.values())
            assertEquals(expected.get(from).contains(to), from.canTransitionTo(to), from + " -> " + to);
    }

    @Test void 不正遷移は明示的なDomainErrorになる() {
        GameSession session = GameSession.idle();
        DomainException error = assertThrows(DomainException.class, () -> session.transitionedTo(GameState.ACTIVE));
        assertEquals("GAME_STATE_CONFLICT", error.code());
    }
}
