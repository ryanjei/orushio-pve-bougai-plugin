package com.ryanjei.orushio.pve.bootstrap;

import com.ryanjei.orushio.pve.persistence.YamlConfigRepository;
import java.nio.file.Path;
import java.util.*;

public record RuntimeConfiguration(int port,boolean configLoaded,boolean diagnosticMode,List<String> warnings,String traceId) {
    public static RuntimeConfiguration load(Path dataFolder){List<String>warnings=new ArrayList<>();String trace=UUID.randomUUID().toString();int port=8765;boolean loaded=true;try{Map<String,String> system=new YamlConfigRepository(dataFolder.resolve("config.yml")).loadOrCreate(Map.of("schemaVersion","1","httpHost","127.0.0.1","httpPort","8765"));port=parsePort(system.get("httpPort"));}catch(Exception e){loaded=false;warnings.add("システム設定を読み込めないため、安全な既定ポートで診断モードを起動しました。");}try{new YamlConfigRepository(dataFolder.resolve("game-config.yml")).loadOrCreate(Map.of("schemaVersion","1","maxParticipants","4"));}catch(Exception e){loaded=false;warnings.add("ゲーム設定を読み込めないため、診断モードを起動しました。");}return new RuntimeConfiguration(port,loaded,!loaded,List.copyOf(warnings),trace);}
    private static int parsePort(String value){try{int p=Integer.parseInt(value);if(p<1024||p>65535)throw new IllegalArgumentException();return p;}catch(Exception e){throw new IllegalArgumentException("port");}}
}
