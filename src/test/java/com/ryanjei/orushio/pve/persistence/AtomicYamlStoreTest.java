package com.ryanjei.orushio.pve.persistence;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AtomicYamlStoreTest {
    @TempDir Path temp;
    @Test void 保存して再読込みできる() {
        AtomicYamlStore store = new AtomicYamlStore(temp.resolve("config.yml"));
        store.write(values("8765"));
        assertEquals("8765", store.read().orElseThrow().get("port"));
    }
    @Test void 破損した現行版を直前バックアップから復元する() throws Exception {
        Path path = temp.resolve("config.yml"); AtomicYamlStore store = new AtomicYamlStore(path);
        store.write(values("8765")); store.write(values("9000"));
        Files.writeString(path, "broken", StandardCharsets.UTF_8);
        assertEquals("8765", store.read().orElseThrow().get("port"));
        assertEquals("8765", store.read().orElseThrow().get("port"));
    }
    @Test void 未知の新しいschemaはバックアップで上書きしない() throws Exception {
        Path path = temp.resolve("config.yml"); AtomicYamlStore store = new AtomicYamlStore(path);
        store.write(values("8765")); store.write(values("9000"));
        Files.writeString(path, "schemaVersion: \"99\"\nport: \"9999\"\n", StandardCharsets.UTF_8);
        assertThrows(RepositoryException.class, store::read);
        assertTrue(Files.readString(path).contains("\"99\""));
    }
    private static Map<String,String> values(String port) { Map<String,String> v = new LinkedHashMap<>(); v.put("schemaVersion", "1"); v.put("port", port); return v; }
}
