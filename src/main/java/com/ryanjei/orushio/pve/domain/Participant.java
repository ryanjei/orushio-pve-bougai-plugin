package com.ryanjei.orushio.pve.domain;

import java.util.Objects;
import java.util.UUID;

public record Participant(UUID playerUuid, String lastKnownName, boolean connected) {
    public Participant {
        Objects.requireNonNull(playerUuid);
        Objects.requireNonNull(lastKnownName);
    }
}
