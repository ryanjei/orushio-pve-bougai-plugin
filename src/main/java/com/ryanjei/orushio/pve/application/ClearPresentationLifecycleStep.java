package com.ryanjei.orushio.pve.application;
import com.ryanjei.orushio.pve.domain.GameSession;
import java.util.Objects;
public final class ClearPresentationLifecycleStep implements GameLifecycleStep{
 private final ClearPresentationGateway gateway;
 public ClearPresentationLifecycleStep(ClearPresentationGateway gateway){this.gateway=Objects.requireNonNull(gateway);}
 public void clearStarted(GameSession session){gateway.showClear(session);}
}
