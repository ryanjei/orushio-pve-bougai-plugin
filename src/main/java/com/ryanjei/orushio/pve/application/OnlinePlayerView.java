package com.ryanjei.orushio.pve.application;

import java.util.UUID;

public record OnlinePlayerView(UUID uuid, String name, boolean setupAdministrator) {
    public OnlinePlayerView(UUID uuid, String name) { this(uuid, name, false); }
}
