package com.ryanjei.orushio.pve.logging;

public interface AuditSink {
    void record(String traceId, String category, String code, String operation);
    boolean healthy();
}
