package org.tron.common.utils;

import java.util.AbstractMap.SimpleImmutableEntry;

/**
 * A key-value pair
 */
public class Pair<K,V> extends SimpleImmutableEntry<K,V> {

    /**
     * Creates a new key-value pair
     * @param key The key for the pair
     * @param value The value for the pair
     */
    public Pair(K key, V value) {
        super(key, value);
    }
}
