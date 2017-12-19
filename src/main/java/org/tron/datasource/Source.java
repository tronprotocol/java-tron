package org.tron.datasource;


public interface Source<K, V> {

    /**
     * Puts key-value pair into source
     */
    void put(K key, V val);

    /**
     * Gets a value by its key
     * @return value or <null/> if no such key in the source
     */
    V get(K key);

    /**
     * Deletes the key-value pair from the source
     */
    void delete(K key);

    /**
     * If this source has underlying level source then all
     * changes collected in this source are flushed into the
     * underlying source.
     * The implementation may do 'cascading' flush, i.e. call
     * flush() on the underlying Source
     * @return true if any changes we flushed, false if the underlying
     * Source didn't change
     */
    boolean flush();

}
