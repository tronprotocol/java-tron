package org.tron.core.state;

import lombok.Getter;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.ethereum.trie.KeyValueMerkleStorage;
import org.hyperledger.besu.ethereum.trie.MerklePatriciaTrie;
import org.hyperledger.besu.ethereum.trie.MerkleStorage;
import org.hyperledger.besu.ethereum.trie.StoredMerklePatriciaTrie;
import org.hyperledger.besu.storage.KeyValueStorage;
import org.hyperledger.besu.storage.RocksDBConfigurationBuilder;
import org.hyperledger.besu.storage.RocksDBKeyValueStorage;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.function.Function;

@Component
public class MPTStoreV2 {

  @Getter
  private final KeyValueStorage keyValueStore;

  @Getter
  private volatile MerkleStorage merkleStorage;

  Function<Bytes, Bytes> valueSerializer = value -> value;
  Function<Bytes, Bytes> valueDeserializer = value -> value;

  public MPTStoreV2() {
    keyValueStore = new RocksDBKeyValueStorage(
        new RocksDBConfigurationBuilder().databaseDir(Paths.get("./world-state-trie-2.0")).build());
    merkleStorage = new KeyValueMerkleStorage(keyValueStore);
  }

  public MerklePatriciaTrie<Bytes, Bytes> getTrie() {
    return new StoredMerklePatriciaTrie<>(merkleStorage::get, valueSerializer, valueDeserializer);
  }

  public MerklePatriciaTrie<Bytes, Bytes> getTrie(byte[] rootHash) {
    return new StoredMerklePatriciaTrie<>(
        merkleStorage::get, Bytes32.wrap(rootHash), valueSerializer, valueDeserializer);
  }
}
