package org.tron.storage;

import java.util.Set;


public interface DbSourceInter<V> extends BatchSourceInter<byte[], V> {


    void setDBName(String name);


    String getDBName();

    void initDB();


    boolean isAlive();


    void closeDB();


    Set<byte[]> allKeys() throws RuntimeException;
}
