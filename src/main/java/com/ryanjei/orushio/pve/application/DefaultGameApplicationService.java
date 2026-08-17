package com.ryanjei.orushio.pve.application;

import com.ryanjei.orushio.pve.domain.*;
import com.ryanjei.orushio.pve.logging.AuditSink;
import com.ryanjei.orushio.pve.map.*;
import com.ryanjei.orushio.pve.persistence.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

public final class DefaultGameApplicationService implements GameApplicationService {
    public static final int STANDARD_MAX_PARTICIPANTS=4;
    private static final AuditSink NO_AUDIT=new AuditSink(){public void record(String a,String b,String c,String d){}public boolean healthy(){return true;}};
    private final ActiveSessionRepository repository;private final Supplier<List<OnlinePlayerView>> onlinePlayers;private final MapProfileRepository maps;private final GameLaunchSettingsRepository settings;private final List<GameLifecycleStep> steps;private final AuditSink audit;private GameSession session;
    public DefaultGameApplicationService(ActiveSessionRepository repository){this(repository,repository.load().orElseGet(GameSession::idle));}
    public DefaultGameApplicationService(ActiveSessionRepository repository,GameSession initialSession){this(repository,initialSession,List::of,null,null,List.of(),NO_AUDIT);}
    public DefaultGameApplicationService(ActiveSessionRepository repository,GameSession initialSession,Supplier<List<OnlinePlayerView>> onlinePlayers,MapProfileRepository maps,GameLaunchSettingsRepository settings,List<GameLifecycleStep> steps,AuditSink audit){this.repository=Objects.requireNonNull(repository);this.session=Objects.requireNonNull(initialSession);this.onlinePlayers=Objects.requireNonNull(onlinePlayers);this.maps=maps;this.settings=settings;this.steps=List.copyOf(steps);this.audit=audit==null?NO_AUDIT:audit;}
    public synchronized GameSession current(){return session;}
    public synchronized OperationResult startRecruiting(String expectedState){expectState(expectedState);save(session.transitionedTo(GameState.RECRUITING));audit("RECRUITING_STARTED");return result();}
    public synchronized OperationResult closeRecruiting(String expectedSessionId){expectSession(expectedSessionId);session.transitionedTo(GameState.IDLE);save(GameSession.idleWithPending(session.pendingCleanup()));audit("RECRUITING_CLOSED");return result();}
    public synchronized OperationResult startMapSetup(String expectedState){expectState(expectedState);save(session.transitionedTo(GameState.MAP_SETUP));return result();}
    public synchronized void validateMapSetup(String expectedSessionId){if(session.state()!=GameState.MAP_SETUP)throw new DomainException("GAME_STATE_CONFLICT","現在はマップセットアップ中ではありません。");expectSession(expectedSessionId);}
    public synchronized OperationResult closeMapSetup(String expectedSessionId){validateMapSetup(expectedSessionId);session.transitionedTo(GameState.IDLE);save(GameSession.idleWithPending(session.pendingCleanup()));return result();}
    public synchronized GameSession addParticipant(UUID playerId){requireEditableParticipants();OnlinePlayerView player=onlinePlayers.get().stream().filter(p->p.uuid().equals(playerId)).findFirst().orElseThrow(()->new DomainException("PLAYER_NOT_ONLINE","オンラインのプレイヤーだけを追加できます。"));if(!session.isParticipant(playerId)&&session.participants().size()>=STANDARD_MAX_PARTICIPANTS)throw new DomainException("PARTICIPANT_LIMIT","ゲーム参加者は1～4人です。");GameSession next=session.withParticipant(playerId,player.name());if(next!=session){save(next);audit("PARTICIPANT_ADDED");}return session;}
    public synchronized GameSession removeParticipant(UUID playerId){requireEditableParticipants();GameSession next=session.withoutParticipant(playerId);if(next.participants().size()!=session.participants().size()){save(next);audit("PARTICIPANT_REMOVED");}return session;}
    public synchronized GameStartView startView(String mapId){MapProfile map=requireMap(mapId);GameLaunchSettings value=requireSettings().load(mapId);Map<UUID,OnlinePlayerView> online=new HashMap<>();onlinePlayers.get().forEach(p->online.put(p.uuid(),p));List<OnlinePlayerView> participants=session.participants().stream().map(p->online.getOrDefault(p.playerUuid(),new OnlinePlayerView(p.playerUuid(),p.lastKnownName()))).toList();List<String> missing=new ArrayList<>();if(!map.enabled())missing.add("マップが有効ではありません。");if(!map.setupComplete())missing.add("マップの必須設定が不足しています。");if(participants.isEmpty())missing.add("ゲーム参加者が選択されていません。");if(session.participants().stream().anyMatch(p->!online.containsKey(p.playerUuid())))missing.add("オフラインのゲーム参加者がいます。");return new GameStartView(mapId,map.displayName(),participants,value,value.resolvedTimeLimitMinutes(),value.resolvedRequiredNormalCores(),value.resolvedEnemyMultiplier(),missing.isEmpty(),List.copyOf(missing));}
    public synchronized GameLaunchSettings saveLaunchSettings(String mapId,GameLaunchSettings value){requireEditableParticipants();requireMap(mapId);requireSettings().save(mapId,value);audit("GAME_SETTINGS_UPDATED");return value;}
    public synchronized OperationResult prepareGame(String expectedState,String mapId){expectState(expectedState);GameStartView view=startView(mapId);if(!view.ready())throw new DomainException("GAME_START_NOT_READY",String.join(" ",view.missing()));GameSession next=session.prepared(mapId,view.timeLimitMinutes(),view.requiredNormalCores(),view.enemyMultiplier());save(next);audit("GAME_START_REQUESTED");return result();}
    public synchronized OperationResult activateGame(String expectedSessionId,Instant now){expectSession(expectedSessionId);if(session.state()!=GameState.PREPARING)conflict();for(GameLifecycleStep step:steps)step.prepare(session);save(session.activated(now));audit("GAME_ACTIVATED");return result();}
    public synchronized OperationResult abortGame(String expectedSessionId,String reason){expectSession(expectedSessionId);if(session.state()!=GameState.ACTIVE&&session.state()!=GameState.PREPARING)conflict();GameSession ending=session.transitionedTo(GameState.ABORTING);save(ending);audit(reason.equals("TIMEOUT")?"GAME_TIMED_OUT":"GAME_ABORT_REQUESTED");Set<UUID> pending=new LinkedHashSet<>(ending.pendingCleanup());ending.participants().stream().filter(p->!p.connected()).map(Participant::playerUuid).forEach(pending::add);try{for(GameLifecycleStep step:steps)step.cleanup(ending);}catch(RuntimeException failure){audit("GAME_CLEANUP_FAILED");throw failure;}save(ending.transitionedTo(GameState.RECOVERING).withPendingCleanup(pending));audit("GAME_CLEANUP_COMPLETED");save(GameSession.idleWithPending(pending));return result();}
    public synchronized boolean expireIfNeeded(Instant now){if(session.state()!=GameState.ACTIVE||session.endsAt().isEmpty()||now.isBefore(session.endsAt().get()))return false;abortGame(session.sessionId().toString(),"TIMEOUT");return true;}
    public synchronized void playerConnected(UUID playerId,String name){if(!session.isParticipant(playerId))return;save(session.withConnection(playerId,name,true));}
    public synchronized void playerDisconnected(UUID playerId){if(!session.isParticipant(playerId))return;save(session.withConnection(playerId,null,false));}
    private void requireEditableParticipants(){if(session.state()!=GameState.IDLE&&session.state()!=GameState.RECRUITING)conflict();}
    private MapProfile requireMap(String id){if(maps==null)throw new UnsupportedOperationException();return maps.find(new MapProfileId(id)).orElseThrow(()->new IllegalArgumentException("マップが見つかりません。"));}
    private GameLaunchSettingsRepository requireSettings(){if(settings==null)throw new UnsupportedOperationException();return settings;}
    private void expectState(String expected){if(!session.state().name().equals(expected))conflict();}
    private void expectSession(String expected){if(!session.sessionId().toString().equals(expected))throw new DomainException("SESSION_MISMATCH","画面のセッションが現在のセッションと一致しません。");}
    private void conflict(){throw new DomainException("GAME_STATE_CONFLICT","画面の状態が最新ではありません。再読み込みしてください。");}
    private void save(GameSession next){repository.save(next);session=next;}
    private OperationResult result(){return new OperationResult(UUID.randomUUID().toString(),session.sessionId().toString(),session.state().name());}
    private void audit(String code){audit.record(UUID.randomUUID().toString(),"ADMIN",code,"GAME_LIFECYCLE");}
}
