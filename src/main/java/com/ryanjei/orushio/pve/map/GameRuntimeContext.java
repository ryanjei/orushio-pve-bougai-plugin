package com.ryanjei.orushio.pve.map;
import java.util.*;
public record GameRuntimeContext(UUID sessionId,String worldName,RuntimeMap map){public GameRuntimeContext{Objects.requireNonNull(sessionId);if(worldName==null||worldName.isBlank())throw new IllegalArgumentException("Runtime World名が必要です。");Objects.requireNonNull(map);}}
