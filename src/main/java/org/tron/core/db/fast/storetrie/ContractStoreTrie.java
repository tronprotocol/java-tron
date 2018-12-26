package org.tron.core.db.fast.storetrie;

import static org.tron.core.db.fast.FastSyncStoreConstant.CONTRACT_STORE_KEY;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteUtil;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.capsule.utils.RLP;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.db.common.WrappedByteArray;
import org.tron.core.db.fast.TrieService;
import org.tron.core.db2.common.DB;
import org.tron.core.trie.TrieImpl;

@Slf4j
@Component
public class ContractStoreTrie extends TronStoreWithRevoking<BytesCapsule> implements
    DB<byte[], BytesCapsule> {

  private Cache<WrappedByteArray, BytesCapsule> cache = CacheBuilder.newBuilder()
      .initialCapacity(1000).maximumSize(1000).expireAfterAccess(5, TimeUnit.MINUTES).build();

  @Autowired
  private TrieService trieService;

  @Autowired
  private ContractStoreTrie(@Value("contractTrie") String dbName) {
    super(dbName);
  }

  public byte[] getValue(byte[] key) {
    TrieImpl trie = trieService.getChildTrie(RLP.encodeString(CONTRACT_STORE_KEY), this);
    return trie.get(RLP.encodeElement(key));
  }

  @Override
  public boolean isEmpty() {
    return super.size() <= 0;
  }

  @Override
  public void remove(byte[] bytes) {
    cache.invalidate(WrappedByteArray.of(bytes));
    super.delete(bytes);
  }

  @Override
  public BytesCapsule get(byte[] key) {
    BytesCapsule bytesCapsule = cache.getIfPresent(WrappedByteArray.of(key));
    return bytesCapsule != null ? bytesCapsule : super.getUnchecked(key);
  }

  @Override
  public void put(byte[] key, BytesCapsule item) {
    super.put(key, item);
    cache.put(WrappedByteArray.of(key), item);
  }
}
