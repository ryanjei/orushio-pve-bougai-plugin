package com.ryanjei.orushio.pve.persistence;

import com.ryanjei.orushio.pve.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GameLifecyclePersistenceTest {
    @TempDir Path temp;

    @Test void activeSessionの参加者設定時刻pendingCleanupを再起動相当で復元する(){
        UUID player=UUID.randomUUID();Instant active=Instant.parse("2026-08-18T00:00:00Z");
        GameSession value=new GameSession(UUID.randomUUID(),GameState.ACTIVE,List.of(new Participant(player,"日本語名",false)),Instant.now(),"map-a",1,45,3,1.25,active,active.plusSeconds(2700),Set.of(player));
        var first=new YamlActiveSessionRepository(temp.resolve("active.yml"));first.save(value);
        GameSession restored=new YamlActiveSessionRepository(temp.resolve("active.yml")).load().orElseThrow();
        assertEquals(value.sessionId(),restored.sessionId());assertEquals(GameState.ACTIVE,restored.state());assertEquals("日本語名",restored.participants().getFirst().lastKnownName());assertFalse(restored.participants().getFirst().connected());assertEquals(45,restored.timeLimitMinutes());assertEquals(active,restored.activeAt().orElseThrow());assertEquals(Set.of(player),restored.pendingCleanup());
    }

    @Test void マップ別上書きはnull既定値と明示値を保存できる(){
        var repository=new YamlGameLaunchSettingsRepository(temp);
        repository.save("map-a",new GameLaunchSettings(Optional.of(90),Optional.empty(),Optional.of(2.0)));
        var restored=new YamlGameLaunchSettingsRepository(temp).load("map-a");
        assertEquals(90,restored.resolvedTimeLimitMinutes());assertEquals(2,restored.resolvedRequiredNormalCores());assertEquals(2.0,restored.resolvedEnemyMultiplier());
        repository.save("map-a",GameLaunchSettings.defaults());assertEquals(60,repository.load("map-a").resolvedTimeLimitMinutes());
    }
}
