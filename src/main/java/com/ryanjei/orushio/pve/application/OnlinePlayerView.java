package com.ryanjei.orushio.pve.application;

import java.util.UUID;

public record OnlinePlayerView(UUID uuid, String name, boolean setupAdministrator,boolean administratorRevocable) {
    public OnlinePlayerView(UUID uuid, String name) { this(uuid, name, false,false); }
    public OnlinePlayerView(UUID uuid,String name,boolean setupAdministrator){this(uuid,name,setupAdministrator,setupAdministrator);}
}
