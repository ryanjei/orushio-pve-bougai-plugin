package com.ryanjei.orushio.pve.application;
import com.ryanjei.orushio.pve.domain.GameSession;
public interface GameLifecycleStep{default void prepare(GameSession session){}default void cleanup(GameSession session){}}
