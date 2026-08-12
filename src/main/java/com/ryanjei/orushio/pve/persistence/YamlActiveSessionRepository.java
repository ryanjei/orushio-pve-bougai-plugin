package com.ryanjei.orushio.pve.persistence;

import com.ryanjei.orushio.pve.domain.GameSession;
import com.ryanjei.orushio.pve.domain.GameState;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class YamlActiveSessionRepository implements ActiveSessionRepository {
    private final AtomicYamlStore store;
    public YamlActiveSessionRepository(Path path) { store = new AtomicYamlStore(path); }
    public Optional<GameSession> load() {
        return store.read().map(v -> new GameSession(UUID.fromString(v.get("sessionId")), GameState.valueOf(v.get("state")), List.of(), Instant.parse(v.get("createdAt"))));
    }
    public void save(GameSession session) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("schemaVersion", "1");
        values.put("sessionId", session.sessionId().toString());
        values.put("state", session.state().name());
        values.put("createdAt", session.createdAt().toString());
        store.write(values);
    }
}
