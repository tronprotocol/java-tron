package org.tron.core.store;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.MarketOrderCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.exception.ItemNotFoundException;

@Component
public class MarketOrderStore extends TronStoreWithRevoking<MarketOrderCapsule> {

  @Autowired
  protected MarketOrderStore(@Value("market_order") String dbName) {
    super(dbName);
  }

  @Override
  public MarketOrderCapsule get(byte[] key) throws ItemNotFoundException {
    byte[] value = revokingDB.get(key);
    return new MarketOrderCapsule(value);
  }

}