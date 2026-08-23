package com.ryanjei.orushio.pve.core;
import java.util.UUID;
/** Implementations must be idempotent because failed destruction side effects are retried. */
public interface CoreProgressListener{default void normalCoreDestroyed(UUID sessionId,int destroyed,int required){}default void finalCoreDestroyed(UUID sessionId){}static CoreProgressListener none(){return new CoreProgressListener(){};}}
