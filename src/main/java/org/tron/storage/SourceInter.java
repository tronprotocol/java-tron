package org.tron.storage;


public interface SourceInter<K, V> {


  void putData(K key, V val);


<<<<<<< HEAD
    /**
     * get a value by key
     *
     * @return value of null if the key in the source
     */
    V getData(K key);
=======
  V getData(K key);
>>>>>>> a84aa0f4221b66bba458a8c1fd581686fae1075b


  void deleteData(K key);


  boolean flush();

}
