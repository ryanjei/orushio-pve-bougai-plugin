package com.ryanjei.orushio.pve.core;
import java.util.*;
public record CoreOwnership(UUID sessionId,UUID coreId,CoreType type,CoreEntityRole role,String runtimeWorld){public CoreOwnership{Objects.requireNonNull(sessionId);Objects.requireNonNull(coreId);Objects.requireNonNull(type);Objects.requireNonNull(role);if(runtimeWorld==null||runtimeWorld.isBlank())throw new IllegalArgumentException("Runtime Worldが必要です。");}}
