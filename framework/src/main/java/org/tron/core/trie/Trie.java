package org.tron.core.trie;

/**
 *
 */
public interface Trie<V> {

  byte[] getRootHash();

  void setRoot(byte[] root);

  /**
   * Recursively delete all nodes from root
   */
  void clear();

  void put(byte[] key, V val);

  V get(byte[] key);

  void delete(byte[] key);

  boolean flush();
}
