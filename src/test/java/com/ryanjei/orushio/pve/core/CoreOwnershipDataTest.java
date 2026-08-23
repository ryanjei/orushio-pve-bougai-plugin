package com.ryanjei.orushio.pve.core;
import org.junit.jupiter.api.Test;import java.util.*;import static org.junit.jupiter.api.Assertions.*;
class CoreOwnershipDataTest{
 @Test void 正常PDCをownershipへ復元する(){UUID session=UUID.randomUUID(),core=UUID.randomUUID();CoreOwnership value=CoreOwnershipData.decode((byte)1,session.toString(),core.toString(),"NORMAL","HITBOX","run-a").orElseThrow();assertEquals(session,value.sessionId());assertEquals(core,value.coreId());assertEquals(CoreType.NORMAL,value.type());assertEquals(CoreEntityRole.HITBOX,value.role());assertEquals("run-a",value.runtimeWorld());}
 @Test void marker欠損または不正値を拒否する(){String id=UUID.randomUUID().toString();assertTrue(CoreOwnershipData.decode(null,id,id,"NORMAL","HITBOX","run-a").isEmpty());assertTrue(CoreOwnershipData.decode((byte)0,id,id,"NORMAL","HITBOX","run-a").isEmpty());}
 @Test void session_core_type_role_worldの欠損と不正を拒否する(){String id=UUID.randomUUID().toString();assertTrue(CoreOwnershipData.decode((byte)1,"bad",id,"NORMAL","HITBOX","run-a").isEmpty());assertTrue(CoreOwnershipData.decode((byte)1,id,"bad","NORMAL","HITBOX","run-a").isEmpty());assertTrue(CoreOwnershipData.decode((byte)1,id,id,"BAD","HITBOX","run-a").isEmpty());assertTrue(CoreOwnershipData.decode((byte)1,id,id,"NORMAL","BAD","run-a").isEmpty());assertTrue(CoreOwnershipData.decode((byte)1,id,id,"NORMAL","HITBOX"," ").isEmpty());}
}
