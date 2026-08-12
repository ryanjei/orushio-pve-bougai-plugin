package com.ryanjei.orushio.pve.domain;

public record MapProfileId(String value) {
    public MapProfileId {
        if (value == null || !value.matches("[a-z0-9-]{1,40}")) {
            throw new IllegalArgumentException("mapIdの形式が正しくありません。");
        }
    }
}
