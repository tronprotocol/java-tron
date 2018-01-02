
package org.tron.trie;

import org.tron.storage.SourceInter;

public interface Trie<V> extends SourceInter<byte[], V> {

    byte[] getRootHash();

    void setRoot(byte[] root);

    /**
     * Recursively deleteData all nodes from root
     */
    void clear();
}
