package com.ryanjei.orushio.pve.application;

public record ParticipantPolicy(int maxParticipants) {
    public ParticipantPolicy {
        if (maxParticipants < 1 || maxParticipants > 1000) {
            throw new IllegalArgumentException("ゲーム参加者上限が範囲外です。");
        }
    }

    public static ParticipantPolicy standard() {
        return new ParticipantPolicy(4);
    }
}
