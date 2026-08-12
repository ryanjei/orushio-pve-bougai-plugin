package com.ryanjei.orushio.pve.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameStateTest {
    @Test void 仕様どおりの遷移だけを許可する() {
        assertTrue(GameState.IDLE.canTransitionTo(GameState.RECRUITING));
        assertTrue(GameState.ACTIVE.canTransitionTo(GameState.CLEAR));
        assertTrue(GameState.RECOVERING.canTransitionTo(GameState.IDLE));
        assertFalse(GameState.IDLE.canTransitionTo(GameState.ACTIVE));
        assertFalse(GameState.MAP_SETUP.canTransitionTo(GameState.RECRUITING));
    }

    @Test void 不正遷移は明示的なDomainErrorになる() {
        GameSession session = GameSession.idle();
        DomainException error = assertThrows(DomainException.class, () -> session.transitionTo(GameState.ACTIVE));
        assertEquals("GAME_STATE_CONFLICT", error.code());
    }
}
