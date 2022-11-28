package org.tron.core.state.trie;

/**
 *
 */
public interface Trie<V> {

  byte[] getRootHash();

  void setRoot(byte[] root);

  /**
   * Recursively delete all nodes from root.
   */
  void clear();

  void put(byte[] key, V val);

  V get(byte[] key);

  void delete(byte[] key);

  /**
   * Commits any pending changes to the underlying storage.
   */
  default void commit() {}

  /** Persist accumulated changes to underlying storage. */
  boolean flush();
}
