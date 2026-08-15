package com.ryanjei.orushio.pve.application;

public final class ServerAdministrationException extends RuntimeException {
    private final String code;

    public ServerAdministrationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() { return code; }
}
