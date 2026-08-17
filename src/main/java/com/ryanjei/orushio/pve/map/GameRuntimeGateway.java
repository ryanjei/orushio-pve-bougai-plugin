package com.ryanjei.orushio.pve.map;
import com.ryanjei.orushio.pve.domain.Participant;import java.util.*;
public interface GameRuntimeGateway{
 void prepare(TemporaryWorldManager.OwnedWorld world,RuntimeMap map,List<Participant> participants);
 void returnParticipantsAndUnload(TemporaryWorldManager.OwnedWorld world,List<Participant> participants);
 void reconnectParticipant(TemporaryWorldManager.OwnedWorld world,RuntimeMap map,UUID playerId);
 void returnPendingToLobby(UUID playerId);
}
