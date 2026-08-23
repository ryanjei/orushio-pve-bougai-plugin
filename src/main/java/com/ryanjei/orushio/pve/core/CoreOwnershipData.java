package com.ryanjei.orushio.pve.core;
import java.util.*;
public final class CoreOwnershipData{
 private CoreOwnershipData(){}
 public static Optional<CoreOwnership>decode(Byte marker,String sessionId,String coreId,String type,String role,String runtimeWorld){if(marker==null||marker!=(byte)1||sessionId==null||coreId==null||type==null||role==null||runtimeWorld==null||runtimeWorld.isBlank())return Optional.empty();try{return Optional.of(new CoreOwnership(UUID.fromString(sessionId),UUID.fromString(coreId),CoreType.valueOf(type),CoreEntityRole.valueOf(role),runtimeWorld));}catch(RuntimeException invalid){return Optional.empty();}}
}
