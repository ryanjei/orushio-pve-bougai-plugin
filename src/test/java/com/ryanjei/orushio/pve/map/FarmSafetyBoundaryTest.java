package com.ryanjei.orushio.pve.map;

import static org.junit.jupiter.api.Assertions.*;import com.ryanjei.orushio.pve.domain.MapProfileId;import java.time.Instant;import java.util.*;import org.junit.jupiter.api.Test;
class FarmSafetyBoundaryTest {
 @Test void farmRegion内外とspawnをRuntimeWorldごとに判定する(){BlockPoint spawn=new BlockPoint(5,64,5,0,0);MapProfile profile=new MapProfile(new MapProfileId("map-a"),"A",false,"template",Map.of("farmSpawn",List.of(spawn)),Map.of("farmRegion",List.of(new Cuboid(0,60,0,10,70,10))),Instant.now());FarmSafetyBoundary boundary=FarmSafetyBoundary.resolve(profile,"opbp_run_a");assertTrue(boundary.contains("opbp_run_a",new BlockPoint(0,60,10,0,0)));assertFalse(boundary.contains("opbp_run_a",new BlockPoint(11,60,10,0,0)));assertFalse(boundary.contains("opbp_run_b",spawn));assertEquals(spawn,boundary.farmSpawn("opbp_run_a"));assertThrows(IllegalArgumentException.class,()->boundary.farmSpawn("opbp_run_b"));assertEquals("map-a",boundary.mapId().value());}
}
