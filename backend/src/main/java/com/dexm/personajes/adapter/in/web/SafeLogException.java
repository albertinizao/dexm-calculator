package com.dexm.personajes.adapter.in.web;

/** Keeps original frames while removing exception messages and nested payloads from logs. */
public final class SafeLogException {
    private SafeLogException() {}
    public static Throwable sanitize(Throwable source) { return sanitize(source, new java.util.IdentityHashMap<>()); }
    private static Throwable sanitize(Throwable source, java.util.IdentityHashMap<Throwable, Throwable> seen) {
        if (source == null) return null;
        if (seen.containsKey(source)) return seen.get(source);
        RuntimeException safe = new RuntimeException(source.getClass().getName());
        seen.put(source, safe);
        safe.setStackTrace(source.getStackTrace());
        Throwable cause = source.getCause();
        if (cause != null && cause != source) safe.initCause(sanitize(cause, seen));
        for (Throwable suppressed : source.getSuppressed()) {
            if (suppressed != source) safe.addSuppressed(sanitize(suppressed, seen));
        }
        return safe;
    }
}
