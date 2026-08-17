package com.ryanjei.orushio.pve.bootstrap;

import com.ryanjei.orushio.pve.domain.GameSession;
import com.ryanjei.orushio.pve.domain.GameState;
import com.ryanjei.orushio.pve.persistence.YamlActiveSessionRepository;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record StartupState(
    RuntimeConfiguration configuration,
    boolean sessionLoaded,
    boolean recoveryRequired,
    Optional<GameSession> session,
    List<String> warnings
) {
    public static StartupState load(Path dataFolder) {
        RuntimeConfiguration configuration = RuntimeConfiguration.load(dataFolder);
        List<String> warnings = new ArrayList<>(configuration.warnings());
        boolean sessionLoaded = true;
        boolean recoveryRequired = false;
        Optional<GameSession> session = Optional.empty();
        try {
            session = new YamlActiveSessionRepository(dataFolder.resolve("sessions/active.yml")).load();
            if(session.isPresent()&&java.util.EnumSet.of(GameState.PREPARING,GameState.ACTIVE,GameState.PAUSED,GameState.CLEAR,GameState.ABORTING,GameState.RECOVERING).contains(session.get().state())){
                recoveryRequired=true;
                warnings.add("前回のゲームセッションが終了していません。復旧が完了するまで新しいゲームを開始できません。");
            }
        } catch (RuntimeException e) {
            sessionLoaded = false;
            recoveryRequired = true;
            warnings.add("進行中セッションを読み込めません。ゲーム操作を行わず、保存データの復旧が必要です。");
        }
        return new StartupState(configuration, sessionLoaded, recoveryRequired, session, List.copyOf(warnings));
    }

    public boolean diagnosticMode() {
        return configuration.diagnosticMode() || recoveryRequired;
    }
}
