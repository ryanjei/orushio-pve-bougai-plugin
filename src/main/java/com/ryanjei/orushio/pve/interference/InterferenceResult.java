package com.ryanjei.orushio.pve.interference;

import java.util.Objects;

public record InterferenceResult(InterferenceType requestedType,int affectedPlayerCount,int skippedPlayerCount,String rejectedReason){
    public InterferenceResult{Objects.requireNonNull(requestedType);if(affectedPlayerCount<0||skippedPlayerCount<0)throw new IllegalArgumentException("妨害人数が不正です。");rejectedReason=rejectedReason==null?"":rejectedReason;}
    public boolean accepted(){return rejectedReason.isEmpty();}
    public static InterferenceResult rejected(InterferenceType type,int skipped,String reason){return new InterferenceResult(type,0,skipped,reason);}
}
