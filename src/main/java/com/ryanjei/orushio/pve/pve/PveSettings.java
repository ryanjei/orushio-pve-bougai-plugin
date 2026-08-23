package com.ryanjei.orushio.pve.pve;

import com.ryanjei.orushio.pve.map.Cuboid;
import java.time.Duration;
import java.util.*;

public record PveSettings(List<EnemyZone> enemyZones) {
    public PveSettings {
        enemyZones = List.copyOf(enemyZones == null ? List.of() : enemyZones);
        Set<UUID> ids = new HashSet<>(); Set<Cuboid> regions = new HashSet<>();
        for (EnemyZone zone : enemyZones) {
            if (!ids.add(zone.enemyZoneId())) throw new IllegalArgumentException("enemyZoneIdが重複しています。");
            if (!regions.add(zone.region())) throw new IllegalArgumentException("同一Regionに複数enemyZoneIdを設定できません。");
        }
    }
    public static PveSettings empty() { return new PveSettings(List.of()); }

    public record EnemyZone(UUID enemyZoneId, Cuboid region, Duration spawnInterval, int baseSpawnCount,
                            Map<EnemyType,Integer> mobWeights, double minParticipantDistance,
                            double maxParticipantDistance) {
        public EnemyZone {
            Objects.requireNonNull(enemyZoneId); Objects.requireNonNull(region);
            long sizeX=(long)region.maxX()-region.minX()+1,sizeY=(long)region.maxY()-region.minY()+1,sizeZ=(long)region.maxZ()-region.minZ()+1;if(sizeX>2048||sizeY>512||sizeZ>2048||sizeX*sizeY*sizeZ>100_000_000L)throw new IllegalArgumentException("Enemy Zoneが防御上限を超えます。");
            if (spawnInterval == null || spawnInterval.isZero() || spawnInterval.isNegative() || spawnInterval.compareTo(Duration.ofHours(24)) > 0)
                throw new IllegalArgumentException("spawnIntervalが範囲外です。");
            if (baseSpawnCount < 1 || baseSpawnCount > 64) throw new IllegalArgumentException("baseSpawnCountが範囲外です。");
            EnumMap<EnemyType,Integer> copy = new EnumMap<>(EnemyType.class); copy.putAll(mobWeights == null ? Map.of() : mobWeights);
            int total = 0; for (EnemyType type : EnemyType.values()) { int weight = copy.getOrDefault(type,0); if (weight < 0 || weight > 1_000_000) throw new IllegalArgumentException("mobWeightが範囲外です。"); total = Math.addExact(total,weight); }
            if (total <= 0) throw new IllegalArgumentException("mobWeight合計が必要です。"); mobWeights = Collections.unmodifiableMap(copy);
            if (!Double.isFinite(minParticipantDistance) || !Double.isFinite(maxParticipantDistance) || minParticipantDistance < 0 || maxParticipantDistance <= minParticipantDistance || maxParticipantDistance > 512)
                throw new IllegalArgumentException("Participant距離が範囲外です。");
        }
        public int totalWeight() { return mobWeights.values().stream().mapToInt(Integer::intValue).sum(); }
    }
}
