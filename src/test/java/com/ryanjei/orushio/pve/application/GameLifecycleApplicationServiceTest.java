package com.ryanjei.orushio.pve.application;

import com.ryanjei.orushio.pve.domain.*;
import com.ryanjei.orushio.pve.map.*;
import com.ryanjei.orushio.pve.persistence.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GameLifecycleApplicationServiceTest {
    private final UUID alice = UUID.randomUUID();
    private final MemoryActive sessions = new MemoryActive();
    private final MemorySettings settings = new MemorySettings();
    private final List<OnlinePlayerView> online = new ArrayList<>(List.of(new OnlinePlayerView(alice, "Alice")));
    private final MapProfile profile = completeMap();
    private final MapProfileRepository maps = new MapProfileRepository() {
        public List<MapProfile> findAll() { return List.of(profile); }
        public Optional<MapProfile> find(MapProfileId id) { return id.equals(profile.mapId()) ? Optional.of(profile) : Optional.empty(); }
        public void save(MapProfile ignored) { }
        public void delete(MapProfileId ignored) { }
    };

    @Test void online参加者だけを選択し重複追加は冪等で上限を守る() {
        var service = service();
        assertEquals(1, service.addParticipant(alice).participants().size());
        assertEquals(1, service.addParticipant(alice).participants().size());
        assertThrows(DomainException.class, () -> service.addParticipant(UUID.randomUUID()));
        for (int i=0;i<4;i++) online.add(new OnlinePlayerView(UUID.randomUUID(), "P"+i));
        online.stream().skip(1).limit(3).forEach(p -> service.addParticipant(p.uuid()));
        assertThrows(DomainException.class, () -> service.addParticipant(online.get(4).uuid()));
    }

    @Test void startViewは有効マップと接続中参加者を検証する() {
        var service=service();
        assertFalse(service.startView("map-a").ready());
        service.addParticipant(alice);
        assertTrue(service.startView("map-a").ready());
        service.playerDisconnected(alice); online.clear();
        assertFalse(service.startView("map-a").ready());
    }

    @Test void 準備中を明示的に経由して開始時人数とサーバー時刻を固定する() {
        var service=service(); service.addParticipant(alice);
        service.saveLaunchSettings("map-a", new GameLaunchSettings(Optional.of(30), Optional.of(4), Optional.of(1.5)));
        service.prepareGame("IDLE", "map-a");
        assertEquals(GameState.PREPARING,service.current().state());
        assertEquals(1,service.current().participantCountAtStart());
        Instant now=Instant.parse("2026-08-18T00:00:00Z");
        service.activateGame(service.current().sessionId().toString(),now);
        assertEquals(GameState.ACTIVE,service.current().state());
        assertEquals(now.plusSeconds(1800),service.current().endsAt().orElseThrow());
        assertThrows(DomainException.class,()->service.addParticipant(UUID.randomUUID()));
    }

    @Test void 切断者を参加者として保持し中止後にpendingCleanupへ残す() {
        var service=service(); service.addParticipant(alice);service.prepareGame("IDLE","map-a");
        service.playerDisconnected(alice);
        assertEquals(1,service.current().participants().size());assertFalse(service.current().participants().getFirst().connected());
        service.abortGame(service.current().sessionId().toString(),"ADMIN");
        assertEquals(GameState.IDLE,service.current().state());assertTrue(service.current().pendingCleanup().contains(alice));
    }

    @Test void 制限時間前は継続し期限到達後だけ安全中止する() {
        var service=service();service.addParticipant(alice);service.prepareGame("IDLE","map-a");
        Instant now=Instant.parse("2026-08-18T00:00:00Z");service.activateGame(service.current().sessionId().toString(),now);
        assertFalse(service.expireIfNeeded(now.plusSeconds(59*60)));
        assertTrue(service.expireIfNeeded(now.plusSeconds(60*60)));
        assertEquals(GameState.IDLE,service.current().state());
    }

    private DefaultGameApplicationService service(){return new DefaultGameApplicationService(sessions,GameSession.idle(),()->List.copyOf(online),maps,settings,List.of(),null);}
    private static MapProfile completeMap(){Map<String,List<BlockPoint>> p=new HashMap<>();for(String k:MapProfile.REQUIRED_SINGLE_POINTS)p.put(k,List.of(point()));p.put("normalCoreCandidates",List.of(point(),point(),point()));p.put("shopPoints",List.of(point()));Map<String,List<Cuboid>> a=new HashMap<>();for(String k:MapProfile.REQUIRED_AREA_COUNTS.keySet())a.put(k,List.of(new Cuboid(0,0,0,1,1,1)));return new MapProfile(new MapProfileId("map-a"),"Map A",true,"original",p,a,Instant.now());}
    private static BlockPoint point(){return new BlockPoint(0,64,0,0,0);}
    private static final class MemoryActive implements ActiveSessionRepository{GameSession value;public Optional<GameSession> load(){return Optional.ofNullable(value);}public void save(GameSession value){this.value=value;}}
    private static final class MemorySettings implements GameLaunchSettingsRepository{GameLaunchSettings value=GameLaunchSettings.defaults();public GameLaunchSettings load(String ignored){return value;}public void save(String ignored,GameLaunchSettings value){this.value=value;}}
}
