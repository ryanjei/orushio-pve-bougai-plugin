package com.ryanjei.orushio.pve.map;
import java.util.*;
public interface GameRuntimeContextResolver{Optional<GameRuntimeContext> resolve(UUID sessionId);}
