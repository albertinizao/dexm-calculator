package com.dexm.personajes.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;

/** Limits IAP signing-key cache entries to five minutes (well below the one-hour maximum). */
final class FiveMinuteJwkCache implements Cache {
    private static final Duration TTL = Duration.ofMinutes(5);
    private final ConcurrentHashMap<Object, Entry> entries = new ConcurrentHashMap<>();
    private final Clock clock;

    FiveMinuteJwkCache() { this(Clock.systemUTC()); }
    FiveMinuteJwkCache(Clock clock) { this.clock = clock; }
    @Override public String getName() { return "iap-jwk-set"; }
    @Override public Object getNativeCache() { return entries; }
    @Override public ValueWrapper get(Object key) { Object value = value(key); return value == null ? null : new SimpleValueWrapper(value); }
    @Override public <T> T get(Object key, Class<T> type) { Object value = value(key); return value == null ? null : type.cast(value); }
    @Override public <T> T get(Object key, Callable<T> loader) {
        T current = get(key, (Class<T>) Object.class);
        if (current != null) return current;
        try { T loaded = loader.call(); put(key, loaded); return loaded; }
        catch (Exception exception) { throw new ValueRetrievalException(key, loader, exception); }
    }
    @Override public void put(Object key, Object value) { entries.put(key, new Entry(value, clock.instant().plus(TTL))); }
    @Override public void evict(Object key) { entries.remove(key); }
    @Override public void clear() { entries.clear(); }

    private Object value(Object key) {
        Entry entry = entries.get(key);
        if (entry == null) return null;
        if (!entry.expiresAt().isAfter(clock.instant())) { entries.remove(key, entry); return null; }
        return entry.value();
    }
    private record Entry(Object value, Instant expiresAt) { }
}
