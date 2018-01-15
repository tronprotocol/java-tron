package org.tron.storage;


public interface SourceInter<K, V> {


    void putData(K key, V val);


    /**
     * get a value by key
     *
     * @return value of null if the key in the source
     */
    V getData(K key);


    void deleteData(K key);


    boolean flush();

}
