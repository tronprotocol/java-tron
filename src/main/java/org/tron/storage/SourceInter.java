package org.tron.storage;


public interface SourceInter<K, V> {


    void putData(K key, V val);


    V getData(K key);


    void deleteData(K key);


    boolean flush();

}
