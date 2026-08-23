package com.ryanjei.orushio.pve.map;

import java.nio.file.Path;
import java.util.*;

public interface MapAdministrationService {
    List<MapProfile> list();
    Optional<String> fixedNextMapId();
    MapProfile importZip(Path zip,String mapId,String displayName);
    MapProfile setEnabled(String mapId,boolean enabled);
    void fixNext(String mapId);
    void delete(String mapId);
    SetupView startSetup(String mapId,UUID administrator,String expectedGameState);
    SetupView setup();
    void selectField(UUID administrator,String field,boolean area);
    default void selectFieldEntry(UUID administrator,String field,boolean area,int index){throw new UnsupportedOperationException();}
    void recordClick(UUID administrator,BlockPoint point,boolean secondaryCorner);
    default void removeSetupEntry(UUID administrator,String field,boolean area,int index){throw new UnsupportedOperationException();}
    default void teleportSetupEntry(UUID administrator,String field,boolean area,int index){throw new UnsupportedOperationException();}
    default void selectSpawnMarker(UUID administrator,String areaId,String markerId){throw new UnsupportedOperationException();}
    default void setSpawnMarkerEnabled(UUID administrator,UUID markerId,boolean enabled){throw new UnsupportedOperationException();}
    default void removeSpawnMarker(UUID administrator,UUID markerId){throw new UnsupportedOperationException();}
    default void teleportSpawnMarker(UUID administrator,UUID markerId){throw new UnsupportedOperationException();}
    default void issueSetupMarker(UUID administrator,SetupMarkerType markerType){throw new UnsupportedOperationException();}
    default void recordSetupMarker(UUID administrator,MapWorldGateway.SetupMarkerToken token,BlockPoint position){throw new UnsupportedOperationException();}
    default boolean ownsSetupWorld(UUID administrator,String worldName){return false;}
    default void setSetupMarkerEnabled(UUID administrator,UUID markerId,boolean enabled){throw new UnsupportedOperationException();}
    default void removeSetupMarker(UUID administrator,UUID markerId){throw new UnsupportedOperationException();}
    default void teleportSetupMarker(UUID administrator,UUID markerId){throw new UnsupportedOperationException();}
    MapProfile saveSetup(String expectedSessionId);
    void discardSetup(String expectedSessionId);
    default void shutdownMapSetupSafely(){}
    default SetupRecoveryView setupRecovery(){return SetupRecoveryView.none();}
    default void discardSetupRecovery(){throw new IllegalStateException("復旧が必要なMap Setupはありません。");}

    record SetupEntry(BlockPoint point,Cuboid region){}
    record MarkerAreaView(String areaId,String label,int markerCount,List<SpawnMarker> markers){}
    record MarkerTypeView(SetupMarkerType markerType,String label,int markerCount,List<SetupMarker> markers){}
    record SetupRecoveryView(boolean recoveryRequired,String mapId,String administrator,String sessionId,String worldName,boolean safeToRecover,String warning){public static SetupRecoveryView none(){return new SetupRecoveryView(false,"","","","",false,"");}}
    record SetupView(boolean active,String mapId,String displayName,String administrator,String selectedField,boolean area,List<String> missing,Map<String,Integer> counts,Map<String,List<SetupEntry>> entries,List<MarkerAreaView> markerAreas,List<MarkerTypeView> setupMarkerTypes,long revision){
        public SetupView(boolean active,String mapId,String displayName,String administrator,String selectedField,boolean area,List<String> missing,Map<String,Integer>counts,Map<String,List<SetupEntry>>entries){this(active,mapId,displayName,administrator,selectedField,area,missing,counts,entries,List.of(),List.of(),0);}
        public SetupView(boolean active,String mapId,String administrator,String selectedField,boolean area,List<String> missing){this(active,mapId,mapId,administrator,selectedField,area,missing,Map.of(),Map.of(),List.of(),List.of(),0);}
        public static SetupView inactive(){return new SetupView(false,"","","","",false,List.of(),Map.of(),Map.of(),List.of(),List.of(),0);}
    }
}
