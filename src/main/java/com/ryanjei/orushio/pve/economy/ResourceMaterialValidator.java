package com.ryanjei.orushio.pve.economy;

@FunctionalInterface
public interface ResourceMaterialValidator {
    boolean isValidBlockMaterial(String canonicalName);
}
