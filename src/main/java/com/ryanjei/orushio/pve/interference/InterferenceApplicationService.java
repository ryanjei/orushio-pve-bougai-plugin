package com.ryanjei.orushio.pve.interference;

import com.ryanjei.orushio.pve.domain.*;
import com.ryanjei.orushio.pve.logging.AuditSink;
import com.ryanjei.orushio.pve.map.*;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;

public final class InterferenceApplicationService {
    public static final String NOT_ACTIVE="GAME_NOT_ACTIVE",SESSION_MISMATCH="SESSION_MISMATCH",RUNTIME_UNAVAILABLE="RUNTIME_UNAVAILABLE",NO_TARGET="NO_ELIGIBLE_PARTICIPANT",COOLDOWN="COOLDOWN_ACTIVE";
    private final Supplier<GameSession> games;private final GameRuntimeContextResolver runtimes;private final InterferenceGateway gateway;private final InterferenceSettings settings;private final AuditSink audit;private final Map<Key,Instant>nextAllowed=new HashMap<>();
    public InterferenceApplicationService(Supplier<GameSession> games,GameRuntimeContextResolver runtimes,InterferenceGateway gateway,InterferenceSettings settings,AuditSink audit){this.games=Objects.requireNonNull(games);this.runtimes=Objects.requireNonNull(runtimes);this.gateway=Objects.requireNonNull(gateway);this.settings=Objects.requireNonNull(settings);this.audit=Objects.requireNonNull(audit);}
    public synchronized InterferenceResult trigger(UUID expectedSessionId,InterferenceType type,Instant now){Objects.requireNonNull(expectedSessionId);Objects.requireNonNull(type);Objects.requireNonNull(now);GameSession session=games.get();if(session.state()!=GameState.ACTIVE)return rejected(type,session.participants().size(),NOT_ACTIVE);if(!session.sessionId().equals(expectedSessionId))return rejected(type,session.participants().size(),SESSION_MISMATCH);GameRuntimeContext context=runtimes.resolve(expectedSessionId).filter(value->value.sessionId().equals(expectedSessionId)).orElse(null);if(context==null)return rejected(type,session.participants().size(),RUNTIME_UNAVAILABLE);List<Cuboid>regions=context.map().profile().areas().getOrDefault("finalRegion",List.of());if(regions.size()!=1)return rejected(type,session.participants().size(),RUNTIME_UNAVAILABLE);nextAllowed.keySet().removeIf(value->!value.sessionId().equals(expectedSessionId));Key key=new Key(expectedSessionId,type);Instant allowed=nextAllowed.get(key);if(allowed!=null&&now.isBefore(allowed))return rejected(type,session.participants().size(),COOLDOWN);List<UUID>participants=session.participants().stream().map(Participant::playerUuid).toList();InterferenceGateway.ExecutionResult executed;try{executed=gateway.apply(expectedSessionId,context.worldName(),regions.getFirst(),participants,type,settings);}catch(RuntimeException failure){rejected(type,participants.size(),"EXECUTION_FAILED");throw failure;}if(executed.affectedPlayerCount()==0)return rejected(type,executed.skippedPlayerCount(),NO_TARGET);nextAllowed.put(key,now.plus(settings.cooldown()));audit.record(UUID.randomUUID().toString(),"ADMIN","INTERFERENCE_"+type.name()+"_TRIGGERED","INTERFERENCE_TRIGGER");return new InterferenceResult(type,executed.affectedPlayerCount(),executed.skippedPlayerCount(),"");}
    private InterferenceResult rejected(InterferenceType type,int skipped,String reason){audit.record(UUID.randomUUID().toString(),"ADMIN","INTERFERENCE_REJECTED","INTERFERENCE_"+type.name()+":"+reason);return InterferenceResult.rejected(type,skipped,reason);}
    private record Key(UUID sessionId,InterferenceType type){}
}
