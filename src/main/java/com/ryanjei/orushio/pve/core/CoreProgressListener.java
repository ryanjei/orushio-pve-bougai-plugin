package com.ryanjei.orushio.pve.core;
import java.util.UUID;
public interface CoreProgressListener{default void normalCoreDestroyed(UUID sessionId,int destroyed,int required){}default void finalCoreDestroyed(UUID sessionId){}static CoreProgressListener none(){return new CoreProgressListener(){};}}
