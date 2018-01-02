package org.tron.storage;

import java.util.Map;

/**
 * The Source which is capable of batch updates.
 * The semantics of a batch update is up to implementation:
 * it can be just performance optimization or batch update
 * can be atomic or other.
 */
public interface BatchSource<K, V> extends Source<K, V> {

    /**
     * Do batch update
     *
     * @param rows Normally this Map is treated just as a collection
     *             of key-value pairs and shouldn't conform to a normal
     *             Map contract. Though it is up to implementation to
     *             require passing specific Maps
     */
    void updateBatch(Map<K, V> rows);
}
