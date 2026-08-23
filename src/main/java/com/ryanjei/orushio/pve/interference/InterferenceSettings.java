package com.ryanjei.orushio.pve.interference;

import java.time.Duration;
import java.util.Objects;

public record InterferenceSettings(Duration darknessDuration,Duration levitationDuration,int levitationAmplifier,Duration cooldown){
    public InterferenceSettings{
        Objects.requireNonNull(darknessDuration);Objects.requireNonNull(levitationDuration);Objects.requireNonNull(cooldown);
        if(darknessDuration.isNegative()||darknessDuration.isZero()||levitationDuration.isNegative()||levitationDuration.isZero()||cooldown.isNegative())throw new IllegalArgumentException("妨害時間設定が不正です。");
        if(levitationAmplifier<0||levitationAmplifier>4)throw new IllegalArgumentException("浮遊レベルが範囲外です。");
    }
    public static InterferenceSettings defaults(){return new InterferenceSettings(Duration.ofSeconds(5),Duration.ofSeconds(2),0,Duration.ofSeconds(2));}
}
