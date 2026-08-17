package com.ryanjei.orushio.pve.persistence;

import com.ryanjei.orushio.pve.domain.*;
import java.nio.file.Path;
import java.util.*;

public final class YamlGameLaunchSettingsRepository implements GameLaunchSettingsRepository{
    private final Path mapsRoot;public YamlGameLaunchSettingsRepository(Path mapsRoot){this.mapsRoot=mapsRoot.toAbsolutePath().normalize();}
    public GameLaunchSettings load(String mapId){return store(mapId).read().map(v->{requireSchemaVersionOne(v);return new GameLaunchSettings(optionalInt(v,"timeLimitMinutes"),optionalInt(v,"requiredNormalCores"),optionalDouble(v,"enemyMultiplier"));}).orElseGet(GameLaunchSettings::defaults);}
    public void save(String mapId,GameLaunchSettings settings){Map<String,String> values=new LinkedHashMap<>();values.put("schemaVersion","1");settings.timeLimitMinutes().ifPresent(v->values.put("timeLimitMinutes",v.toString()));settings.requiredNormalCores().ifPresent(v->values.put("requiredNormalCores",v.toString()));settings.enemyMultiplier().ifPresent(v->values.put("enemyMultiplier",v.toString()));store(mapId).write(values);}
    private AtomicYamlStore store(String mapId){if(!mapId.matches("[a-z0-9-]{1,40}"))throw new IllegalArgumentException("mapIdが不正です。");Path directory=mapsRoot.resolve(mapId).normalize();if(!directory.getParent().equals(mapsRoot))throw new IllegalArgumentException("mapIdが不正です。");return new AtomicYamlStore(directory.resolve("game-settings.yml"));}
    private static Optional<Integer> optionalInt(Map<String,String> values,String key){String value=values.get(key);return value==null||value.isBlank()?Optional.empty():Optional.of(Integer.parseInt(value));}
    private static Optional<Double> optionalDouble(Map<String,String> values,String key){String value=values.get(key);return value==null||value.isBlank()?Optional.empty():Optional.of(Double.parseDouble(value));}
    private static void requireSchemaVersionOne(Map<String,String> values){if(!"1".equals(values.get("schemaVersion")))throw new RepositoryException("game-settingsのschemaVersionが未対応です。");}
}
