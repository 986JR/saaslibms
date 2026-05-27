package com.saas.libms.audit;

public class AuditMetadata {
    private final StringBuilder sb = new StringBuilder("{");
    private boolean hasEntry = false;

    private AuditMetadata() {}

    public static AuditMetadata builder() {
        return new AuditMetadata();
    }

    public AuditMetadata put(String key, String value) {
        if (value == null) return this;
        appendCommaIfNeeded();
        sb.append("\"").append(escape(key)).append("\":\"").append(escape(value)).append("\"");
        return this;
    }

    public AuditMetadata put(String key, Number value) {
        if (value == null) return this;
        appendCommaIfNeeded();
        sb.append("\"").append(escape(key)).append("\":").append(value);
        return this;
    }

    public String build() {
        if (!hasEntry) return null; // no metadata — caller can pass null to AuditLogService
        return sb.append("}").toString();
    }

    private void appendCommaIfNeeded() {
        if (hasEntry) sb.append(",");
        hasEntry = true;
    }

    // Minimal JSON string escaping — handles the most common characters.
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
