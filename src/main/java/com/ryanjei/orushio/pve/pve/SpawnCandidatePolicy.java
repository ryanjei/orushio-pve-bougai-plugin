package com.ryanjei.orushio.pve.pve;

public final class SpawnCandidatePolicy {
    private SpawnCandidatePolicy() {}
    public static boolean accepts(boolean runtimeOwned,boolean insideZone,boolean insideFarm,boolean safeFloor,boolean collisionFree,boolean liquidFree,double nearestParticipantDistance,double minimumDistance,double maximumDistance){return runtimeOwned&&insideZone&&!insideFarm&&safeFloor&&collisionFree&&liquidFree&&Double.isFinite(nearestParticipantDistance)&&nearestParticipantDistance>=minimumDistance&&nearestParticipantDistance<=maximumDistance;}
}
