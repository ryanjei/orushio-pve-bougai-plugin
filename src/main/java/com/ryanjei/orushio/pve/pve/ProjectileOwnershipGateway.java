package com.ryanjei.orushio.pve.pve;

import java.util.*;
public interface ProjectileOwnershipGateway {
    boolean inheritOwnership(UUID projectileId,UUID shooterId);
}
