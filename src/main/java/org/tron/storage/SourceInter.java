package org.tron.storage;


public interface Source<K, V> {


    void put(K key, V val);


    V get(K key);


    void delete(K key);


    boolean flush();

}
