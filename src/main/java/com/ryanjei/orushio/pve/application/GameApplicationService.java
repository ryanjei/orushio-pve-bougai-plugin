package com.ryanjei.orushio.pve.application;

import com.ryanjei.orushio.pve.domain.GameSession;

public interface GameApplicationService {
    GameSession current();
    OperationResult startRecruiting(String expectedState);
    OperationResult closeRecruiting(String expectedSessionId);
    OperationResult startMapSetup(String expectedState);
    void validateMapSetup(String expectedSessionId);
    OperationResult closeMapSetup(String expectedSessionId);
}
