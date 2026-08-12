package com.ryanjei.orushio.pve.http;

import java.util.Map;

public interface Diagnostics {
    Map<String, Object> snapshot();
}
