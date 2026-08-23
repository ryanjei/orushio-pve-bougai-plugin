package com.ryanjei.orushio.pve.pve;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpawnCandidatePolicyTest {
    @Test void Runtime_Zone_Farm_足場_衝突_液体_距離を全て要求する(){assertTrue(ok(true,true,false,true,true,true,10));assertFalse(ok(false,true,false,true,true,true,10));assertFalse(ok(true,false,false,true,true,true,10));assertFalse(ok(true,true,true,true,true,true,10));assertFalse(ok(true,true,false,false,true,true,10));assertFalse(ok(true,true,false,true,false,true,10));assertFalse(ok(true,true,false,true,true,false,10));assertFalse(ok(true,true,false,true,true,true,1));assertFalse(ok(true,true,false,true,true,true,31));assertFalse(ok(true,true,false,true,true,true,Double.POSITIVE_INFINITY));}
    private boolean ok(boolean runtime,boolean zone,boolean farm,boolean floor,boolean collision,boolean liquid,double distance){return SpawnCandidatePolicy.accepts(runtime,zone,farm,floor,collision,liquid,distance,2,30);}
}
