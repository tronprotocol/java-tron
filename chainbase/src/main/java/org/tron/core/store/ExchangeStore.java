package org.tron.core.store;

import com.google.common.collect.Streams;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.ExchangeCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.exception.ItemNotFoundException;

@Component
public class ExchangeStore extends TronStoreWithRevoking<ExchangeCapsule> {

  @Autowired
  protected ExchangeStore(@Value("exchange") String dbName) {
    super(dbName, ExchangeCapsule.class);
  }

  @Override
  public ExchangeCapsule get(byte[] key) throws ItemNotFoundException {
    return revokingDB.get(key);
  }

  /**
   * get all exchanges.
   */
  public List<ExchangeCapsule> getAllExchanges() {
    return Streams.stream(iterator())
        .map(Map.Entry::getValue)
        .sorted(
            (ExchangeCapsule a, ExchangeCapsule b) -> a.getCreateTime() <= b.getCreateTime() ? 1
                : -1)
        .collect(Collectors.toList());
  }
}