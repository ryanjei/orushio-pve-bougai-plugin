package com.ryanjei.orushio.pve.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum GameState {
    IDLE, RECRUITING, PREPARING, ACTIVE, PAUSED, CLEAR, ABORTING, RECOVERING, MAP_SETUP;

    private static final Map<GameState, Set<GameState>> ALLOWED = Map.of(
        IDLE, EnumSet.of(RECRUITING, PREPARING, MAP_SETUP),
        RECRUITING, EnumSet.of(PREPARING, IDLE),
        PREPARING, EnumSet.of(ACTIVE, ABORTING),
        ACTIVE, EnumSet.of(PAUSED, CLEAR, ABORTING),
        PAUSED, EnumSet.of(ACTIVE, ABORTING),
        CLEAR, EnumSet.of(RECOVERING),
        ABORTING, EnumSet.of(RECOVERING),
        RECOVERING, EnumSet.of(IDLE),
        MAP_SETUP, EnumSet.of(IDLE)
    );

    public boolean canTransitionTo(GameState next) {
        return ALLOWED.get(this).contains(next);
    }
}
