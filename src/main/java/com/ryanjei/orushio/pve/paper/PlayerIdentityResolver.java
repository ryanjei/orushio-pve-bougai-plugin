package com.ryanjei.orushio.pve.paper;

interface PlayerIdentityResolver {
    ResolvedPlayerIdentity resolve(String playerName) throws Exception;
}
