package com.ryanjei.orushio.pve.paper;

import com.ryanjei.orushio.pve.application.GameThreadExecutor;
import com.ryanjei.orushio.pve.application.OnlinePlayerView;
import com.ryanjei.orushio.pve.application.PlayerQuery;
import org.bukkit.Bukkit;
import java.time.Duration;
import java.util.List;

public final class PaperPlayerQuery implements PlayerQuery {
    private final GameThreadExecutor executor;
    public PaperPlayerQuery(GameThreadExecutor executor) { this.executor = executor; }
    public List<OnlinePlayerView> onlinePlayers() {
        try { return executor.execute(() -> Bukkit.getOnlinePlayers().stream().map(p -> new OnlinePlayerView(p.getUniqueId(), p.getName())).toList(), Duration.ofSeconds(3)); }
        catch (Exception e) { throw new IllegalStateException("オンラインプレイヤーを取得できません。", e); }
    }
}
