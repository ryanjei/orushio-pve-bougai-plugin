package com.ryanjei.orushio.pve.persistence;

import com.ryanjei.orushio.pve.domain.GameSession;
import java.util.Optional;

public interface ActiveSessionRepository {
    Optional<GameSession> load();
    void save(GameSession session);
}
