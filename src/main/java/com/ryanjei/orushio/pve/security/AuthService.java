package com.ryanjei.orushio.pve.security;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class AuthService {
    public record Session(String id,String csrf){}
    public enum ResultType { SUCCESS, INVALID, RATE_LIMITED }
    public record AuthResult(ResultType type,Session session){public static AuthResult success(Session s){return new AuthResult(ResultType.SUCCESS,s);} public static AuthResult invalid(){return new AuthResult(ResultType.INVALID,null);} public static AuthResult limited(){return new AuthResult(ResultType.RATE_LIMITED,null);}}
    private record Bootstrap(Instant expiresAt){}
    private record Failures(int count,Instant expiresAt){}
    private static final int MAX_FAILURES=5;
    private static final Duration FAILURE_WINDOW=Duration.ofMinutes(1);
    private final SecureRandom random=new SecureRandom(); private final Clock clock;
    private final Map<String,Bootstrap> bootstraps=new ConcurrentHashMap<>(); private final Map<String,Session> sessions=new ConcurrentHashMap<>(); private final Map<String,Failures> failures=new ConcurrentHashMap<>();
    public AuthService(){this(Clock.systemUTC());} public AuthService(Clock clock){this.clock=clock;}
    public String issueBootstrap(Duration lifetime){cleanup();String token=randomToken();bootstraps.put(hash(token),new Bootstrap(clock.instant().plus(lifetime)));return token;}
    public AuthResult exchangeLimited(String token,String clientKey){cleanup();if(isLimited(clientKey))return AuthResult.limited();if(token==null){fail(clientKey);return AuthResult.invalid();}Bootstrap value=bootstraps.remove(hash(token));if(value==null||value.expiresAt().isBefore(clock.instant())){fail(clientKey);return AuthResult.invalid();}failures.remove(clientKey);Session s=new Session(randomToken(),randomToken());sessions.put(hash(s.id()),s);return AuthResult.success(s);}
    public AuthResult authenticateLimited(String id,String clientKey){cleanup();if(isLimited(clientKey))return AuthResult.limited();Session s=id==null?null:sessions.get(hash(id));if(s==null){fail(clientKey);return AuthResult.invalid();}failures.remove(clientKey);return AuthResult.success(s);}
    public Optional<Session> exchange(String token){return Optional.ofNullable(exchangeLimited(token,"default").session());}
    public Optional<Session> authenticate(String id){return Optional.ofNullable(authenticateLimited(id,"default-session").session());}
    public int failureEntryCount(){cleanup();return failures.size();}
    private boolean isLimited(String key){Failures f=failures.get(key);return f!=null&&f.count()>=MAX_FAILURES;}
    private void fail(String key){failures.compute(key,(k,v)->v==null||v.expiresAt().isBefore(clock.instant())?new Failures(1,clock.instant().plus(FAILURE_WINDOW)):new Failures(v.count()+1,v.expiresAt()));}
    private void cleanup(){Instant now=clock.instant();bootstraps.entrySet().removeIf(e->e.getValue().expiresAt().isBefore(now));failures.entrySet().removeIf(e->e.getValue().expiresAt().isBefore(now));}
    private String randomToken(){byte[] b=new byte[32];random.nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);} private static String hash(String v){try{return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(v.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
