package com.ryanjei.orushio.pve.progression;

import java.util.*;

public record FinalAreaProgressionView(int destroyedNormalCores,int requiredNormalCores,boolean finalAreaUnlocked,boolean finalCoreDestroyed,List<ParticipantProgress> participants){
 public FinalAreaProgressionView{participants=List.copyOf(participants);}
 public static FinalAreaProgressionView idle(){return new FinalAreaProgressionView(0,0,false,false,List.of());}
 public record ParticipantProgress(UUID playerId,String name,boolean finalAreaEntered,boolean checkpointActive){}
}
