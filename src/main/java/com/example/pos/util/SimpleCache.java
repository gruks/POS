package com.example.pos.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory cache for frequently accessed data
 * Reduces database queries and improves performance
 */
public class SimpleCache<K, V> {
    
    private final Map<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
    private final long ttlMillis;
    
    public SimpleCache(long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }
    
    /**
     * Get value from cache
     */
    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        
        // Check if expired
        if (System.currentTimeMillis() - entry.timestamp > ttlMillis) {
            cache.remove(key);
            return null;
        }
        
        return entry.value;
    }
    
    /**
     * Put value in cache
     */
    public void put(K key, V value) {
        cache.put(key, new CacheEntry<>(value, System.currentTimeMillis()));
    }
    
    /**
     * Remove value from cache
     */
    public void remove(K key) {
        cache.remove(key);
    }
    
    /**
     * Clear entire cache
     */
    public void clear() {
        cache.clear();
    }
    
    /**
     * Get cache size
     */
    public int size() {
        return cache.size();
    }
    
    /**
     * Clean expired entries
     */
    public void cleanExpired() {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> 
            now - entry.getValue().timestamp > ttlMillis
        );
    }
    
    private static class CacheEntry<V> {
        final V value;
        final long timestamp;
        
        CacheEntry(V value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
    }
}
