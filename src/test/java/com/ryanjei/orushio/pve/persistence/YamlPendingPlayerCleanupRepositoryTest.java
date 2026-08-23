package com.ryanjei.orushio.pve.persistence;

import static org.junit.jupiter.api.Assertions.*;import java.nio.file.*;import java.util.*;import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;
class YamlPendingPlayerCleanupRepositoryTest {
 @TempDir Path temp;
 @Test void sessionId付きentryをAtomic保存し同一組を重複させない(){Path path=temp.resolve("pending-player-cleanup.yml");var repository=new YamlPendingPlayerCleanupRepository(path);var entry=new PendingPlayerCleanupRepository.Entry(UUID.randomUUID(),UUID.randomUUID());repository.add(entry);repository.add(entry);assertEquals(Set.of(entry),repository.findAll());repository.remove(entry);assertTrue(repository.findAll().isEmpty());assertTrue(Files.exists(path.resolveSibling("pending-player-cleanup.yml.bak")));}
 @Test void unknownSchemaとmalformedEntryを拒否して上書きしない()throws Exception{Path path=temp.resolve("pending-player-cleanup.yml");Files.writeString(path,"schemaVersion: \"2\"\nentryCount: \"0\"\n");var repository=new YamlPendingPlayerCleanupRepository(path);assertThrows(RepositoryException.class,repository::findAll);assertThrows(RepositoryException.class,()->repository.add(new PendingPlayerCleanupRepository.Entry(UUID.randomUUID(),UUID.randomUUID())));assertTrue(Files.readString(path).contains("\"2\""));Path malformed=temp.resolve("malformed.yml");Files.writeString(malformed,"schemaVersion: \"1\"\nentryCount: \"1\"\nentry.0.playerUuid: \"bad\"\nentry.0.sessionId: \"bad\"\n");assertThrows(RepositoryException.class,()->new YamlPendingPlayerCleanupRepository(malformed).findAll());}
}
