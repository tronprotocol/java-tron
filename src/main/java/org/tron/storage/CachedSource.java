package org.tron.storage;

import java.util.Collection;

/**
 * SourceInter which internally caches underlying SourceInter key-value pairs
 */
public interface CachedSourceInter<Key, Value> extends SourceInter<Key, Value> {

    /**
     * @return The underlying SourceInter
     */
    SourceInter<Key, Value> getSource();

    /**
     * @return Modified entry allKeys if this is a write cache
     */
    Collection<Key> getModified();

    /**
     * @return indicates the cache has modified entries
     */
    boolean hasModified();


    /**
     * Estimates the size of cached entries in bytes.
     * This value shouldn't be precise size of Java objects
     *
     * @return cache size in bytes
     */
    long estimateCacheSize();

    /**
     * Just a convenient shortcut to the most popular Sources with byte[] key
     */
    interface BytesKey<Value> extends CachedSourceInter<byte[], Value> {
    }
}
