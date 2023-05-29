package org.tron.core.state.store;

import com.google.common.cache.CacheLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.ethereum.trie.KeyValueMerkleStorage;
import org.hyperledger.besu.ethereum.trie.MerkleTrieException;
import org.hyperledger.besu.storage.KeyValueStorage;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import org.tron.common.cache.CacheManager;
import org.tron.common.cache.CacheType;
import org.tron.common.cache.TronCache;
import org.tron.core.state.StateType;
import org.tron.core.state.annotation.NeedWorldStateTrieStoreCondition;

@Component
@Conditional(NeedWorldStateTrieStoreCondition.class)
public class KeyValueMerkleCacheStorage extends KeyValueMerkleStorage {

  private final Map<StateType, TronCache<Bytes32, Optional<Bytes>>> cache;

  private final List<StateType> cacheTypes = Arrays.asList(
      StateType.DelegatedResource, StateType.DelegatedResourceAccountIndex,
      StateType.StorageRow, StateType.Account,
      StateType.Votes,
      StateType.Code, StateType.Contract);

  @Autowired
  public KeyValueMerkleCacheStorage(@Autowired KeyValueStorage keyValueStorage) {
    super(keyValueStorage);
    cache = Collections.synchronizedMap(new HashMap<>());
    for (StateType stateType : cacheTypes) {
      cache.put(stateType, CacheManager.allocate(CacheType.findByType(
          CacheType.worldStateTrie.type + '.' + stateType.getName()),
          new CacheLoader<Bytes32, Optional<Bytes>>() {
            @Override
            public Optional<Bytes> load(@NotNull Bytes32 key) {
              return get(key);
            }
          }, (key, value) -> Bytes32.SIZE + value.orElse(Bytes.EMPTY).size()));
    }
  }

  @Override
  public Optional<Bytes> get(final Bytes location, final Bytes32 hash) {
    try {
      StateType stateType = parse(location);
      if (stateType != StateType.UNDEFINED) {
        return cache.get(stateType).get(hash);
      }
      return get(hash);
    } catch (ExecutionException e) {
      throw new MerkleTrieException(e.getMessage(), hash, location);
    }
  }


  private Optional<Bytes> get(final Bytes32 hash) {
    return super.get(null, hash);
  }

  @Override
  public void put(final Bytes location, final Bytes32 hash, final Bytes value) {
    super.put(location, hash, value);
    StateType stateType = parse(location);
    if (stateType != StateType.UNDEFINED) {
      cache.get(stateType).put(hash, Optional.of(value));
    }
  }

  private StateType parse(final Bytes location) {
    byte stateTypeLen = Byte.BYTES << 1;
    if (location.size() < stateTypeLen) {
      return StateType.UNDEFINED;
    }
    byte high = location.get(0);
    byte low = location.get(1);
    if ((high & 0xf0) != 0 || (low & 0xf0) != 0) {
      throw new IllegalArgumentException("Invalid path: contains elements larger than a nibble");
    }

    byte type = (byte) (high << 4 | low);
    StateType s = StateType.get(type);
    if (cacheTypes.contains(s)) {
      return s;
    }
    return StateType.UNDEFINED;
  }
}
