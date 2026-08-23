package com.ryanjei.orushio.pve.application;

import java.util.*;

public interface PlayerInventoryGateway {
    interface RollbackState {}
    enum CleanupResult { CLEANED, OFFLINE }
    Optional<RollbackState> initializeIfOnline(UUID playerId);
    void rollback(UUID playerId,RollbackState state);
    CleanupResult returnToLobbyAndCleanup(UUID playerId);
}
