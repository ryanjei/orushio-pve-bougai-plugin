package com.ryanjei.orushio.pve.security;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class LauncherShutdownTokenTest {
    @Test void tokenは推測困難で一回だけ使える(){var token=new LauncherShutdownToken();String value=token.issue();assertTrue(value.length()>=40);assertFalse(token.consume("invalid"));assertTrue(token.consume(value));assertFalse(token.consume(value));}
}
