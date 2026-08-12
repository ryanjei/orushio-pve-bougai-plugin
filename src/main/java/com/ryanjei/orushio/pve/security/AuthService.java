package com.ryanjei.orushio.pve.security;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class AuthService {
    public record Session(String id, String csrf) {}
    private record Bootstrap(String hash, Instant expiresAt) {}
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Bootstrap> bootstraps = new ConcurrentHashMap<>();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public String issueBootstrap(Duration lifetime) {
        String token = randomToken();
        bootstraps.put(hash(token), new Bootstrap(hash(token), Instant.now().plus(lifetime)));
        return token;
    }

    public Optional<Session> exchange(String token) {
        if (token == null) return Optional.empty();
        Bootstrap value = bootstraps.remove(hash(token));
        if (value == null || value.expiresAt().isBefore(Instant.now())) return Optional.empty();
        Session session = new Session(randomToken(), randomToken());
        sessions.put(hash(session.id()), session);
        return Optional.of(session);
    }

    public Optional<Session> authenticate(String id) { return id == null ? Optional.empty() : Optional.ofNullable(sessions.get(hash(id))); }
    private String randomToken() { byte[] bytes = new byte[32]; random.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private static String hash(String value) {
        try { return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
}
