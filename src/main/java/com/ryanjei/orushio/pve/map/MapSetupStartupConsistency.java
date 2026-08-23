package com.ryanjei.orushio.pve.map;

import com.ryanjei.orushio.pve.domain.GameSession;
import com.ryanjei.orushio.pve.domain.GameState;
import com.ryanjei.orushio.pve.logging.AuditSink;
import com.ryanjei.orushio.pve.persistence.ActiveSessionRepository;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Reconciles the persisted game state with setup artifacts before startup recovery mutates them. */
public final class MapSetupStartupConsistency {
    private MapSetupStartupConsistency() { }

    public record Result(GameSession session, boolean repaired, boolean recoveryRequired, String warning) { }

    public static Result inspectAndRepair(
            GameSession session, TemporaryWorldManager worlds,
            ActiveSessionRepository sessions, MapSetupEvidenceRepository evidenceRepository, AuditSink audit) {
        Objects.requireNonNull(session);
        List<TemporaryWorldManager.OwnedWorld> setupWorlds = worlds.setupWorlds();
        MapSetupEvidenceRepository.Evidence evidence;
        try { evidence=evidenceRepository.load().orElse(null); }
        catch(RuntimeException failure){String warning="Map Setup証跡を安全に読み込めません。復旧が必要です。";worlds.requireRecovery(warning);audit.record(UUID.randomUUID().toString(),"SYSTEM","MAP_SETUP_EVIDENCE_INVALID","PLUGIN_ENABLE");return new Result(session,false,true,warning);}
        if(evidence!=null&&!matchesEvidence(evidence,setupWorlds)){
            String warning="Map Setup証跡と一時ワールドの所有情報が一致しません。安全な復旧が必要です。";
            if(setupWorlds.isEmpty())worlds.requireRecovery(warning);else worlds.requireRecovery(setupWorlds.getFirst(),warning);
            audit.record(UUID.randomUUID().toString(),"SYSTEM","MAP_SETUP_EVIDENCE_MISMATCH","PLUGIN_ENABLE");
            return new Result(session,false,true,warning);
        }
        if (session.state() == GameState.MAP_SETUP) {
            // Draft is intentionally process-local in the Phase 3 contract. After a process restart,
            // absence of every owned setup world proves that no recoverable setup artifact remains.
            if (evidence==null && setupWorlds.isEmpty()) {
                GameSession repaired = GameSession.idleWithPending(session.pendingCleanup());
                sessions.save(repaired);
                audit.record(UUID.randomUUID().toString(), "SYSTEM", "ORPHAN_MAP_SETUP_REPAIRED", "PLUGIN_ENABLE");
                return new Result(repaired, true, false,
                        "実体のないマップセットアップ状態を安全に待機状態へ復旧しました。");
            }
            String warning = "マップセットアップの生成物が残っています。安全な復旧が必要です。";
            if(evidence!=null&&!evidence.sessionId().equals(session.sessionId().toString()))warning="ゲーム状態とMap Setup証跡のsessionIdが一致しません。安全な復旧が必要です。";
            if(setupWorlds.isEmpty())worlds.requireRecovery(warning);else worlds.requireRecovery(setupWorlds.getFirst(), warning);
            audit.record(UUID.randomUUID().toString(), "SYSTEM", "MAP_SETUP_RECOVERY_REQUIRED", "PLUGIN_ENABLE");
            return new Result(session, false, true, warning);
        }
        if (evidence!=null || !setupWorlds.isEmpty()) {
            String warning = "ゲーム状態と一致しないマップセットアップ生成物があります。安全な復旧が必要です。";
            if(setupWorlds.isEmpty())worlds.requireRecovery(warning);else worlds.requireRecovery(setupWorlds.getFirst(), warning);
            audit.record(UUID.randomUUID().toString(), "SYSTEM", "ORPHAN_SETUP_WORLD_DETECTED", "PLUGIN_ENABLE");
            return new Result(session, false, true, warning);
        }
        return new Result(session, false, false, "");
    }
    private static boolean matchesEvidence(MapSetupEvidenceRepository.Evidence evidence,List<TemporaryWorldManager.OwnedWorld> worlds){return worlds.stream().anyMatch(world->world.worldName().equals(evidence.worldName())&&world.mapId().value().equals(evidence.mapId())&&world.ownershipId().equals(evidence.ownershipId())&&world.sessionId().equals(evidence.sessionId()));}
}
