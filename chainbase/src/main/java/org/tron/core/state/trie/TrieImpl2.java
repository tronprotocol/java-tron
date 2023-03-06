package org.tron.core.state.trie;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.ethereum.trie.KeyValueMerkleStorage;
import org.hyperledger.besu.ethereum.trie.MerklePatriciaTrie;
import org.hyperledger.besu.ethereum.trie.MerkleStorage;
import org.hyperledger.besu.ethereum.trie.Node;
import org.hyperledger.besu.ethereum.trie.RangeStorageEntriesCollector;
import org.hyperledger.besu.ethereum.trie.StoredMerklePatriciaTrie;
import org.hyperledger.besu.ethereum.trie.TrieIterator;
import org.hyperledger.besu.storage.InMemoryKeyValueStorage;
import org.hyperledger.besu.storage.KeyValueStorage;
import org.hyperledger.besu.storage.RocksDBConfiguration;
import org.hyperledger.besu.storage.RocksDBConfigurationBuilder;
import org.hyperledger.besu.storage.RocksDBKeyValueStorage;
import org.tron.core.state.rlp.FastByteComparisons;

/**
 *
 */
@Slf4j(topic = "db")
public class TrieImpl2 implements Trie<Bytes> {

  private final MerklePatriciaTrie<Bytes, Bytes> trie;
  @Getter
  private final MerkleStorage merkleStorage;

  public TrieImpl2() {
    this(Bytes32.ZERO);
  }

  public TrieImpl2(Bytes32 root) {
    this(new KeyValueMerkleStorage(new InMemoryKeyValueStorage()), root);
  }

  public TrieImpl2(String keyValueStore) {
    this(keyValueStore, Bytes32.ZERO);
  }

  public TrieImpl2(String keyValueStore, Bytes32 root) {
    this(createStore(keyValueStore), root);
  }

  public TrieImpl2(KeyValueStorage keyValueStore) {
    this(keyValueStore, Bytes32.ZERO);
  }

  public TrieImpl2(KeyValueStorage keyValueStore, Bytes32 root) {
    this(new KeyValueMerkleStorage(keyValueStore), root);
  }

  public TrieImpl2(MerkleStorage merkleStorage) {
    this(merkleStorage, Bytes32.ZERO);
  }

  public TrieImpl2(MerkleStorage merkleStorage, Bytes32 root) {
    this.merkleStorage = merkleStorage;

    if (root.isZero()) {
      trie = new StoredMerklePatriciaTrie<>(merkleStorage::get,
              Function.identity(),
              Function.identity());
    } else {
      trie = new StoredMerklePatriciaTrie<>(merkleStorage::get, root,
              Function.identity(),
              Function.identity());
    }
  }

  private static KeyValueStorage createStore(String store) {
    try {
      return new RocksDBKeyValueStorage(config(Paths.get(store)));
    } catch (IOException e) {
      logger.error("{}", e);
      return null;
    }
  }

  private static RocksDBConfiguration config(Path store) throws IOException {
    return new RocksDBConfigurationBuilder().databaseDir(store).build();
  }

  public void visitAll(Consumer<Node<Bytes>> nodeConsumer) {
    trie.visitAll(nodeConsumer);
  }

  @Override
  public Bytes get(byte[] key) {
    return trie.get(Bytes.wrap(key)).orElse(null);
  }

  public Bytes get(Bytes key) {
    return trie.get(key).orElse(null);
  }

  @Override
  public void put(byte[] key, Bytes value) {
    trie.put(Bytes.wrap(key), value);
  }

  public void put(Bytes key, Bytes value) {
    trie.put(key, value);
  }

  @Override
  public void delete(byte[] key) {
    trie.remove(Bytes.wrap(key));
  }

  @Override
  public void commit() {
    trie.commit(merkleStorage::put);
  }

  @Override
  public byte[] getRootHash() {
    return trie.getRootHash().toArrayUnsafe();
  }

  public Bytes32 getRootHashByte32() {
    return trie.getRootHash();
  }

  @Override
  public void setRoot(byte[] root) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new RuntimeException("Not implemented yet");
  }

  public  TreeMap<Bytes32, Bytes> entriesFrom(Bytes32 startKeyHash, Bytes32 endKeyHash) {
    final RangeStorageEntriesCollector collector = RangeStorageEntriesCollector.createCollector(
            startKeyHash, endKeyHash, Integer.MAX_VALUE, Integer.MAX_VALUE);
    final TrieIterator<Bytes> visitor = RangeStorageEntriesCollector.createVisitor(collector);
    return (TreeMap<Bytes32, Bytes>)
           this.entriesFrom(root -> RangeStorageEntriesCollector.collectEntries(collector, visitor,
                    root, startKeyHash));
  }

  public Map<Bytes32, Bytes> entriesFrom(final Function<Node<Bytes>, Map<Bytes32, Bytes>> handler) {
    return trie.entriesFrom(handler);
  }

  @Override
  public boolean flush() {
    merkleStorage.commit();
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TrieImpl2 trieImpl1 = (TrieImpl2) o;

    return FastByteComparisons.equalByte(getRootHash(), trieImpl1.getRootHash());

  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(getRootHash());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + trie.getRootHash() + "]";
  }
}
