package org.tron.core.store;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.MarketPriceCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.exception.ItemNotFoundException;

@Component
public class MarketPriceStore extends TronStoreWithRevoking<MarketPriceCapsule> {

  @Autowired
  protected MarketPriceStore(@Value("market_price") String dbName) {
    super(dbName);
  }

  @Override
  public MarketPriceCapsule get(byte[] key) throws ItemNotFoundException {
    byte[] value = revokingDB.get(key);
    return new MarketPriceCapsule(value);
  }

}