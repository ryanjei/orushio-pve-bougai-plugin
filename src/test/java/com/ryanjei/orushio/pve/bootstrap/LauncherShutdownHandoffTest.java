package com.ryanjei.orushio.pve.bootstrap;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LauncherShutdownHandoffTest {
    @TempDir Path temp;
    @Test void tokenを一時ファイル経由で原子的に渡しclearできる()throws Exception{Path path=temp.resolve("launcher-shutdown.token");var handoff=new LauncherShutdownHandoff(path);handoff.publish("secret-token");assertEquals("secret-token",Files.readString(path));assertFalse(Files.exists(path.resolveSibling("launcher-shutdown.token.tmp")));handoff.clear();assertFalse(Files.exists(path));}
}
