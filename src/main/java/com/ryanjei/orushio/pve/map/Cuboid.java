package com.ryanjei.orushio.pve.map;

public record Cuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    public Cuboid {
        if (minX > maxX || minY > maxY || minZ > maxZ) throw new IllegalArgumentException("範囲の最小・最大座標が正しくありません。");
    }
    public static Cuboid between(BlockPoint a, BlockPoint b) { return new Cuboid(Math.min(a.x(),b.x()),Math.min(a.y(),b.y()),Math.min(a.z(),b.z()),Math.max(a.x(),b.x()),Math.max(a.y(),b.y()),Math.max(a.z(),b.z())); }
}
