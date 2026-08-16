package com.ryanjei.orushio.pve.application;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class DefaultServerAdministrationServiceTest {
    @Test void オンラインプレイヤー0人と複数人を取得できる() {
        FakeGateway gateway = new FakeGateway();
        var service = new DefaultServerAdministrationService(gateway);
        assertTrue(service.onlinePlayers().isEmpty());
        gateway.online.add(new OnlinePlayerView(UUID.randomUUID(), "zeta"));
        gateway.online.add(new OnlinePlayerView(UUID.randomUUID(), "Alpha"));
        assertEquals(List.of("Alpha", "zeta"), service.onlinePlayers().stream().map(OnlinePlayerView::name).toList());
    }

    @Test void ホワイトリストONとOFFをPaper側の確認値で返す() {
        FakeGateway gateway = new FakeGateway();
        var service = new DefaultServerAdministrationService(gateway);
        assertFalse(service.whitelistEnabled());
        assertTrue(service.setWhitelistEnabled(true));
        assertFalse(service.setWhitelistEnabled(false));
    }

    @Test void オンラインプレイヤーをセットアップ管理者にできる() {
        FakeGateway gateway = new FakeGateway();
        UUID playerId = UUID.randomUUID();
        gateway.online.add(new OnlinePlayerView(playerId, "Player_1", false));
        OnlinePlayerView updated = new DefaultServerAdministrationService(gateway).grantSetupAdministrator(playerId);
        assertTrue(updated.setupAdministrator());
        assertEquals(playerId, updated.uuid());
    }

    @Test void 追加と削除が即座に一覧へ反映される() {
        FakeGateway gateway = new FakeGateway();
        var service = new DefaultServerAdministrationService(gateway);
        service.addWhitelistedPlayer("Player_1");
        assertEquals(List.of("Player_1"), service.whitelistedPlayers().stream().map(WhitelistEntryView::name).toList());
        service.removeWhitelistedPlayer("player_1");
        assertTrue(service.whitelistedPlayers().isEmpty());
    }

    @Test void 重複追加と存在しない削除を拒否する() {
        FakeGateway gateway = new FakeGateway();
        var service = new DefaultServerAdministrationService(gateway);
        service.addWhitelistedPlayer("Player_1");
        assertEquals("PLAYER_ALREADY_WHITELISTED", assertThrows(ServerAdministrationException.class, () -> service.addWhitelistedPlayer("player_1")).code());
        assertEquals("PLAYER_NOT_WHITELISTED", assertThrows(ServerAdministrationException.class, () -> service.removeWhitelistedPlayer("Missing")).code());
    }

    @Test void 不正なプレイヤー名をPaperへ渡さない() {
        FakeGateway gateway = new FakeGateway();
        var service = new DefaultServerAdministrationService(gateway);
        assertEquals("INVALID_PLAYER_NAME", assertThrows(ServerAdministrationException.class, () -> service.addWhitelistedPlayer("a/b")).code());
        assertTrue(gateway.whitelist.isEmpty());
    }

    private static final class FakeGateway implements ServerAdministrationGateway {
        final List<OnlinePlayerView> online = new ArrayList<>();
        final List<WhitelistEntryView> whitelist = new ArrayList<>();
        boolean enabled;
        public List<OnlinePlayerView> onlinePlayers(){return List.copyOf(online);}
        public OnlinePlayerView grantSetupAdministrator(UUID playerId){
            for (int index=0;index<online.size();index++) if (online.get(index).uuid().equals(playerId)) { var updated=new OnlinePlayerView(playerId,online.get(index).name(),true);online.set(index,updated);return updated; }
            throw new ServerAdministrationException("PLAYER_NOT_ONLINE","指定されたプレイヤーは現在オンラインではありません。");
        }
        public boolean whitelistEnabled(){return enabled;}
        public void setWhitelistEnabled(boolean value){enabled=value;}
        public List<WhitelistEntryView> whitelistedPlayers(){return List.copyOf(whitelist);}
        public WhitelistEntryView addWhitelistedPlayer(String name){var entry=new WhitelistEntryView(UUID.randomUUID(),name);whitelist.add(entry);return entry;}
        public boolean removeWhitelistedPlayer(String name){return whitelist.removeIf(entry->entry.name().equalsIgnoreCase(name));}
    }
}
