package com.ryanjei.orushio.pve.persistence;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AtomicYamlStore {
    public static final int SCHEMA_VERSION = 1;
    private final Path path;

    public AtomicYamlStore(Path path) { this.path = path; }

    public synchronized Optional<Map<String, String>> read() {
        if (!Files.exists(path)) return Optional.empty();
        try {
            return Optional.of(parse(Files.readAllLines(path, StandardCharsets.UTF_8)));
        } catch (SchemaTooNewException e) {
            throw new RepositoryException("新しいschemaVersionのため上書きせず読込みを停止しました。", e);
        } catch (RuntimeException | IOException primary) {
            Path backup = backupPath();
            if (!Files.exists(backup)) throw new RepositoryException("保存データが破損しており、バックアップもありません。", primary);
            try {
                Map<String, String> recovered = parse(Files.readAllLines(backup, StandardCharsets.UTF_8));
                restoreWithoutRotatingBackup(recovered);
                return Optional.of(recovered);
            } catch (RuntimeException | IOException backupFailure) {
                primary.addSuppressed(backupFailure);
                throw new RepositoryException("保存データとバックアップの両方を読み込めません。", primary);
            }
        }
    }

    public synchronized void write(Map<String, String> values) {
        validateWriteVersion(values);
        try {
            Files.createDirectories(path.getParent());
            rejectNewerExistingSchema();
            Path temp = path.resolveSibling(path.getFileName() + ".tmp");
            writeAndFlush(temp, values);
            if (Files.exists(path)) Files.copy(path, backupPath(), StandardCopyOption.REPLACE_EXISTING);
            move(temp, path);
        } catch (IOException | NumberFormatException e) {
            throw new RepositoryException("保存に失敗しました。", e);
        }
    }

    private void restoreWithoutRotatingBackup(Map<String, String> values) throws IOException {
        Path temp = path.resolveSibling(path.getFileName() + ".restore.tmp");
        try {
            writeAndFlush(temp, values);
            move(temp, path);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private void rejectNewerExistingSchema() throws IOException {
        if (!Files.exists(path)) return;
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        String rawVersion = null;
        for (String line : lines) if (line.trim().startsWith("schemaVersion:")) { rawVersion = unquote(line.substring(line.indexOf(':') + 1).trim()); break; }
        if (rawVersion == null) return; // 破損は通常の保存でバックアップへ退避可能。
        try {
            if (Integer.parseInt(rawVersion) > SCHEMA_VERSION) throw new RepositoryException("新しいschemaVersionのため上書きできません。");
        } catch (NumberFormatException e) { throw new RepositoryException("既存のschemaVersionが不正です。", e); }
    }

    private static void validateWriteVersion(Map<String, String> values) {
        try {
            if (Integer.parseInt(values.getOrDefault("schemaVersion", "-1")) != SCHEMA_VERSION) throw new RepositoryException("未知のschemaVersionは上書きできません。");
        } catch (NumberFormatException e) { throw new RepositoryException("schemaVersionが不正です。", e); }
    }

    private static void writeAndFlush(Path target, Map<String, String> values) throws IOException {
        StringBuilder yaml = new StringBuilder();
        values.forEach((key, value) -> yaml.append(key).append(": ").append(quote(value)).append('\n'));
        Files.writeString(target, yaml, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        try (FileChannel channel = FileChannel.open(target, StandardOpenOption.WRITE)) { channel.force(true); }
    }

    private Map<String, String> parse(List<String> lines) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String line : lines) {
            if (line.isBlank() || line.stripLeading().startsWith("#")) continue;
            int colon = line.indexOf(':');
            if (colon <= 0) throw new RepositoryException("YAML形式が正しくありません。");
            String key = line.substring(0, colon).trim();
            String raw = line.substring(colon + 1).trim();
            values.put(key, unquote(raw));
        }
        int version;
        try { version = Integer.parseInt(values.getOrDefault("schemaVersion", "-1")); }
        catch (NumberFormatException e) { throw new RepositoryException("schemaVersionが不正です。", e); }
        if (version > SCHEMA_VERSION) throw new SchemaTooNewException();
        if (version != SCHEMA_VERSION) throw new RepositoryException("schemaVersionが未対応です。");
        return values;
    }

    private static String quote(String value) { return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""; }
    private static String unquote(String value) {
        if (value.length() < 2 || value.charAt(0) != '"' || value.charAt(value.length() - 1) != '"') throw new RepositoryException("値は引用符で囲む必要があります。");
        return value.substring(1, value.length() - 1).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
    }
    private Path backupPath() { return path.resolveSibling(path.getFileName() + ".bak"); }
    private static void move(Path source, Path target) throws IOException {
        try { Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException e) { Files.move(source, target, StandardCopyOption.REPLACE_EXISTING); }
    }
    private static final class SchemaTooNewException extends RuntimeException {}
}
