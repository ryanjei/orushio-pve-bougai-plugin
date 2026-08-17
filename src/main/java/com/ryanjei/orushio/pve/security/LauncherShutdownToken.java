package com.ryanjei.orushio.pve.security;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class LauncherShutdownToken {
    private final SecureRandom random=new SecureRandom();
    private String expected;

    public synchronized String issue(){if(expected!=null)throw new IllegalStateException("shutdown tokenは発行済みです。");byte[] bytes=new byte[32];random.nextBytes(bytes);expected=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);return expected;}
    public synchronized boolean consume(String candidate){if(expected==null||candidate==null)return false;boolean valid=MessageDigest.isEqual(expected.getBytes(java.nio.charset.StandardCharsets.US_ASCII),candidate.getBytes(java.nio.charset.StandardCharsets.US_ASCII));if(valid)expected=null;return valid;}
}
