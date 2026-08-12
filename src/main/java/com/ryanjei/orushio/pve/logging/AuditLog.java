package com.ryanjei.orushio.pve.logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class AuditLog {
    private final Path directory;
    public AuditLog(Path directory) { this.directory = directory; }
    public synchronized String record(String category, String message) {
        String traceId = UUID.randomUUID().toString();
        String safe = message.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
        String line = "{\"at\":\"" + OffsetDateTime.now() + "\",\"category\":\"" + category + "\",\"message\":\"" + safe + "\",\"traceId\":\"" + traceId + "\"}\n";
        try { Files.createDirectories(directory); Files.writeString(directory.resolve("audit-" + LocalDate.now() + ".jsonl"), line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND); }
        catch (IOException ignored) { }
        return traceId;
    }
}
