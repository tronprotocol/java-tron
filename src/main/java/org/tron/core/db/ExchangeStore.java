package org.tron.core.db;

import static org.tron.core.db.fast.FastSyncStoreConstant.TrieEnum.EXCHANGE;

import com.google.common.collect.Streams;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.ExchangeCapsule;
import org.tron.core.db.fast.callback.FastSyncCallBack;
import org.tron.core.db.fast.storetrie.ExchangeStoreTrie;
import org.tron.core.exception.ItemNotFoundException;

@Component
public class ExchangeStore extends TronStoreWithRevoking<ExchangeCapsule> {

  @Autowired
  private FastSyncCallBack fastSyncCallBack;

  @Autowired
  private ExchangeStoreTrie exchangeStoreTrie;

  @Autowired
  protected ExchangeStore(@Value("exchange") String dbName) {
    super(dbName);
  }

  @Override
  public ExchangeCapsule get(byte[] key) {
    byte[] value = getValue(key);
    return ArrayUtils.isEmpty(value) ? null : new ExchangeCapsule(value);
  }


  /**
   * get all exchanges.
   */
  public List<ExchangeCapsule> getAllExchanges() {
    List<ExchangeCapsule> exchangeCapsuleList = exchangeStoreTrie.getAllExchanges();
    if (CollectionUtils.isNotEmpty(exchangeCapsuleList)) {
      return exchangeCapsuleList;
    }
    return Streams.stream(iterator())
        .map(Map.Entry::getValue)
        .sorted(
            (ExchangeCapsule a, ExchangeCapsule b) -> a.getCreateTime() <= b.getCreateTime() ? 1
                : -1)
        .collect(Collectors.toList());
  }

  @Override
  public void put(byte[] key, ExchangeCapsule item) {
    super.put(key, item);
    fastSyncCallBack.callBack(key, item.getData(), EXCHANGE);
  }

  @Override
  public void delete(byte[] key) {
    super.delete(key);
    fastSyncCallBack.delete(key, EXCHANGE);
  }

  public byte[] getValue(byte[] key) {
    byte[] value = exchangeStoreTrie.getValue(key);
    if (ArrayUtils.isEmpty(value)) {
      value = revokingDB.getUnchecked(key);
    }
    return value;
  }

  @Override
  public void close() {
    super.close();
    exchangeStoreTrie.close();
  }
}