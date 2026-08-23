package com.ryanjei.orushio.pve.core;
import com.ryanjei.orushio.pve.map.BlockPoint;import java.util.*;
public record CoreProgressView(int destroyedNormal,int requiredNormal,boolean gateOpen,List<CoreHealth> cores){public CoreProgressView{cores=List.copyOf(cores);}public record CoreHealth(UUID coreId,CoreType type,BlockPoint position,double hp,double maxHp,boolean destroyed){}}
