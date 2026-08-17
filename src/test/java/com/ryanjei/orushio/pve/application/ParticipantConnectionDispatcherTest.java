package com.ryanjei.orushio.pve.application;

import com.ryanjei.orushio.pve.domain.GameSession;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ParticipantConnectionDispatcherTest {
    @Test void quit直後のjoinは古いquitに上書きされない(){var games=new RecordingGames();UUID id=UUID.randomUUID();try(var dispatcher=new ParticipantConnectionDispatcher(games,failure->fail(failure))){dispatcher.disconnected(id);dispatcher.connected(id,"Alice");}assertTrue(games.connected);assertEquals("Alice",games.name);}
    @Test void join直後のquitは最終的にofflineになる(){var games=new RecordingGames();UUID id=UUID.randomUUID();try(var dispatcher=new ParticipantConnectionDispatcher(games,failure->fail(failure))){dispatcher.connected(id,"Alice");dispatcher.disconnected(id);}assertFalse(games.connected);}
    @Test void closeはpendingイベントをflushしてから終了する(){var games=new RecordingGames();games.delay=true;UUID id=UUID.randomUUID();var dispatcher=new ParticipantConnectionDispatcher(games,failure->fail(failure));dispatcher.connected(id,"Alice");dispatcher.close();assertTrue(games.connected);assertEquals(1,games.events);}
    private static final class RecordingGames implements GameApplicationService{volatile boolean connected,delay;volatile String name;volatile int events;public synchronized void playerConnected(UUID id,String name){pause();this.name=name;connected=true;events++;}public synchronized void playerDisconnected(UUID id){pause();connected=false;events++;}private void pause(){if(delay)try{Thread.sleep(100);}catch(InterruptedException e){Thread.currentThread().interrupt();}}public GameSession current(){return GameSession.idle();}public OperationResult startRecruiting(String s){throw new UnsupportedOperationException();}public OperationResult closeRecruiting(String s){throw new UnsupportedOperationException();}public OperationResult startMapSetup(String s){throw new UnsupportedOperationException();}public void validateMapSetup(String s){}public OperationResult closeMapSetup(String s){throw new UnsupportedOperationException();}public OperationResult activateGame(String s, Instant i){throw new UnsupportedOperationException();}}
}
