package com.ryanjei.orushio.pve.paper;

import com.ryanjei.orushio.pve.economy.ResourceMaterialValidator;
import org.bukkit.Material;

public final class PaperResourceMaterialValidator implements ResourceMaterialValidator {
    @Override
    public boolean isValidBlockMaterial(String canonicalName) {
        Material material = Material.matchMaterial(canonicalName);
        return material != null && material.isBlock() && material.name().equals(canonicalName);
    }
}
