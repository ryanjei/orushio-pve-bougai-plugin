package com.ryanjei.orushio.pve.persistence;

import java.util.*;

public interface PendingPlayerCleanupRepository {
    record Entry(UUID playerUuid, UUID sessionId) {
        public Entry { Objects.requireNonNull(playerUuid); Objects.requireNonNull(sessionId); }
    }
    Set<Entry> findAll();
    default List<Entry> findByPlayer(UUID playerUuid){return findAll().stream().filter(entry->entry.playerUuid().equals(playerUuid)).toList();}
    void add(Entry entry);
    void remove(Entry entry);
}
