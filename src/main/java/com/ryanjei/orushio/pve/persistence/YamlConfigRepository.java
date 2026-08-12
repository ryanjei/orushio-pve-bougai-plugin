package com.ryanjei.orushio.pve.persistence;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class YamlConfigRepository implements ConfigRepository {
    private final AtomicYamlStore store;
    public YamlConfigRepository(Path path) { store = new AtomicYamlStore(path); }
    public Map<String, String> loadOrCreate(Map<String, String> defaults) {
        return store.read().orElseGet(() -> { Map<String, String> copy = new LinkedHashMap<>(defaults); store.write(copy); return copy; });
    }
    public void save(Map<String, String> values) { store.write(values); }
}
