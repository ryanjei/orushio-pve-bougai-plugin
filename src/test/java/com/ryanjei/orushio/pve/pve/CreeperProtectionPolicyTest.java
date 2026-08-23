package com.ryanjei.orushio.pve.pve;
import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class CreeperProtectionPolicyTest{@Test void CurrentRuntime所有CreeperのBlockDamageだけを抑止する(){assertTrue(CreeperProtectionPolicy.suppressBlockDamage(true,true));assertFalse(CreeperProtectionPolicy.suppressBlockDamage(false,true));assertFalse(CreeperProtectionPolicy.suppressBlockDamage(true,false));}}
