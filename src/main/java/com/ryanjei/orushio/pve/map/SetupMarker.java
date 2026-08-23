package com.ryanjei.orushio.pve.map;
import java.util.*;
public record SetupMarker(UUID markerId,SetupMarkerType markerType,BlockPoint position,boolean enabled){public SetupMarker{Objects.requireNonNull(markerId);Objects.requireNonNull(markerType);Objects.requireNonNull(position);}public SetupMarker withEnabled(boolean value){return new SetupMarker(markerId,markerType,position,value);}}
