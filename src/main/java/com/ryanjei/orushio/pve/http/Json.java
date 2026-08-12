package com.ryanjei.orushio.pve.http;

import java.util.Collection;
import java.util.Map;

final class Json {
    private Json() {}
    static String value(Object value) {
        if (value == null) return "null";
        if (value instanceof String s) return "\"" + escape(s) + "\"";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) return map.entrySet().stream().map(e -> value(e.getKey().toString()) + ":" + value(e.getValue())).collect(java.util.stream.Collectors.joining(",", "{", "}"));
        if (value instanceof Collection<?> collection) return collection.stream().map(Json::value).collect(java.util.stream.Collectors.joining(",", "[", "]"));
        return value(value.toString());
    }
    private static String escape(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r"); }
}
