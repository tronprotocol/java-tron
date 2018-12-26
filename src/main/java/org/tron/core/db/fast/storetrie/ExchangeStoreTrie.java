package org.tron.core.db.fast.storetrie;

import static org.tron.core.db.fast.FastSyncStoreConstant.EXCHANGE_STORE_KEY;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteUtil;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.capsule.ExchangeCapsule;
import org.tron.core.capsule.utils.RLP;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.db.common.WrappedByteArray;
import org.tron.core.db.fast.TrieService;
import org.tron.core.db2.common.DB;
import org.tron.core.trie.TrieImpl;
import org.tron.core.trie.TrieImpl.Node;
import org.tron.core.trie.TrieImpl.ScanAction;

@Slf4j
@Component
public class ExchangeStoreTrie extends TronStoreWithRevoking<BytesCapsule> implements
    DB<byte[], BytesCapsule> {

  private Cache<WrappedByteArray, BytesCapsule> cache = CacheBuilder.newBuilder()
      .initialCapacity(1000).maximumSize(1000).expireAfterAccess(5, TimeUnit.MINUTES).build();

  @Autowired
  private TrieService trieService;

  @Autowired
  private ExchangeStoreTrie(@Value("exchangeTrie") String dbName) {
    super(dbName);
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
    logger.info("put key: {}", ByteUtil.toHexString(key));
    super.put(key, item);
    cache.put(WrappedByteArray.of(key), item);
  }

  public byte[] getValue(byte[] key) {
    TrieImpl trie = trieService.getChildTrie(RLP.encodeString(EXCHANGE_STORE_KEY), this);
    return trie.get(RLP.encodeElement(key));
  }

  public List<ExchangeCapsule> getAllExchanges() {
    TrieImpl trie = trieService.getChildTrie(RLP.encodeString(EXCHANGE_STORE_KEY), this);
    return scanAll(trie);
  }

  private List<ExchangeCapsule> scanAll(TrieImpl trie) {
    List<ExchangeCapsule> result = new ArrayList<>();
    trie.scanTree(new ScanAction() {
      @Override
      public void doOnNode(byte[] hash, Node node) {

      }

      @Override
      public void doOnValue(byte[] nodeHash, Node node, byte[] key, byte[] value) {
        if (ArrayUtils.isNotEmpty(value)) {
          result.add(new ExchangeCapsule(value));
        }
      }
    });
    return result;
  }
}
