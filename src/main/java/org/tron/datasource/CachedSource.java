package org.tron.datasource;

import java.util.Collection;

/**
 * Source which internally caches underlying Source key-value pairs
 */
public interface CachedSource<Key, Value> extends Source<Key, Value> {

    /**
     * @return The underlying Source
     */
    Source<Key, Value> getSource();

    /**
     * @return Modified entry keys if this is a write cache
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
    interface BytesKey<Value> extends CachedSource<byte[], Value> {
    }
}
