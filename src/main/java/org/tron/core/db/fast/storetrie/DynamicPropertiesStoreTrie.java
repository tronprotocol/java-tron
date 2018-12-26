package org.tron.core.db.fast.storetrie;

import static org.tron.core.db.fast.FastSyncStoreConstant.DYNAMIC_PROPERTIES_STORE_KEY;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.capsule.utils.RLP;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.db.common.WrappedByteArray;
import org.tron.core.db.fast.TrieService;
import org.tron.core.db2.common.DB;
import org.tron.core.trie.TrieImpl;

@Slf4j
@Component
public class DynamicPropertiesStoreTrie extends TronStoreWithRevoking<BytesCapsule> implements
    DB<byte[], BytesCapsule> {

  private Cache<WrappedByteArray, BytesCapsule> cache = CacheBuilder.newBuilder()
      .initialCapacity(1000).maximumSize(1000).expireAfterAccess(5, TimeUnit.MINUTES).build();

  @Autowired
  private TrieService trieService;

  @Autowired
  private DynamicPropertiesStoreTrie(@Value("propertiesTrie") String dbName) {
    super(dbName);
  }

  public void saveTokenIdNum(byte[] tokenIdNum, long num) {
    this.put(tokenIdNum,
        new BytesCapsule(ByteArray.fromLong(num)));
  }

  public long getTokenIdNum(byte[] tokenIdNum) {
    return getValue(tokenIdNum, "TOKEN_ID_NUM");
  }

  public void saveTotalStoragePool(byte[] totalStoragePool, long trx) {
    this.put(totalStoragePool,
        new BytesCapsule(ByteArray.fromLong(trx)));
  }

  public long getTotalStoragePool(byte[] totalStoragePool) {
    return getValue(totalStoragePool, "TOTAL_STORAGE_POOL");
  }

  public void saveTotalStorageReserved(byte[] totalStorageReserved, long bytes) {
    this.put(totalStorageReserved,
        new BytesCapsule(ByteArray.fromLong(bytes)));
  }

  public long getTotalStorageReserved(byte[] totalStorageReserved) {
    return getValue(totalStorageReserved, "TOTAL_STORAGE_RESERVED");
  }

  public void saveLatestExchangeNum(byte[] latestExchangeNum, long number) {
    this.put(latestExchangeNum, new BytesCapsule(ByteArray.fromLong(number)));
  }

  public long getLatestExchangeNum(byte[] latestExchangeNum) {
    return getValue(latestExchangeNum, "LATEST_EXCHANGE_NUM");
  }

  public void saveTotalNetWeight(byte[] totalNetWeightKey, long totalNetWeight) {
    this.put(totalNetWeightKey,
        new BytesCapsule(ByteArray.fromLong(totalNetWeight)));
  }

  public long getTotalNetWeight(byte[] totalNetWeightKey) {
    return getValue(totalNetWeightKey, "TOTAL_NET_WEIGHT");
  }

  public void saveTotalEnergyWeight(byte[] totalEnergyWeightKey, long totalEnergyWeight) {
    this.put(totalEnergyWeightKey,
        new BytesCapsule(ByteArray.fromLong(totalEnergyWeight)));
  }

  public long getTotalEnergyWeight(byte[] totalEnergyWeightKey) {
    return getValue(totalEnergyWeightKey, "TOTAL_ENERGY_WEIGHT");
  }

  public void saveLatestProposalNum(byte[] latestProposalNum, long number) {
    this.put(latestProposalNum, new BytesCapsule(ByteArray.fromLong(number)));
  }

  public long getLatestProposalNum(byte[] latestProposalNum) {
    return getValue(latestProposalNum, "LATEST_PROPOSAL_NUM");
  }

  public void saveTotalCreateWitnessFee(byte[] totalCreateWitnessCost, long value) {
    this.put(totalCreateWitnessCost,
        new BytesCapsule(ByteArray.fromLong(value)));
  }

  public long getTotalCreateWitnessCost(byte[] totalCreateWitnessCost) {
    return getValue(totalCreateWitnessCost, "TOTAL_CREATE_WITNESS_COST");
  }

  public void saveTotalTransactionCost(byte[] totalTransactionCost, long value) {
    this.put(totalTransactionCost,
        new BytesCapsule(ByteArray.fromLong(value)));
  }

  public long getTotalTransactionCost(byte[] totalTransactionCost) {
    return getValue(totalTransactionCost, "TOTAL_TRANSACTION_COST");
  }

  @Override
  public void put(byte[] key, BytesCapsule item) {
    super.put(key, item);
    cache.put(WrappedByteArray.of(key), item);
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

  public long getValue(byte[] key, String errorReason) {
    TrieImpl trie = trieService.getChildTrie(RLP.encodeString(DYNAMIC_PROPERTIES_STORE_KEY), this);
    byte[] value = trie.get(RLP.encodeElement(key));
    if (ArrayUtils.isEmpty(value)) {
      throw new IllegalArgumentException("not found " + errorReason);
    }
    return ByteArray.toLong(value);
  }
}
