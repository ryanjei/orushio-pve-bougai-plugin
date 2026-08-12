package com.ryanjei.orushio.pve.persistence;

import java.util.Map;

public interface ConfigRepository {
    Map<String, String> loadOrCreate(Map<String, String> defaults);
    void save(Map<String, String> values);
}
