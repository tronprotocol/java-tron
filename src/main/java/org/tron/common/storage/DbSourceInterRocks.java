package org.tron.common.storage;

import java.util.Set;

public interface DbSourceInterRocks<V> extends BatchSourceInterRocks<byte[], V> {

  String getDBName();

  void setDBName(String name);

  void initDB();

  boolean isAlive();

  void closeDB();

  void resetDb();

  Set<byte[]> allKeys() throws RuntimeException;

  Set<byte[]> allValues() throws RuntimeException;

  long getTotal() throws RuntimeException;

}
