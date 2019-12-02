package org.tron.core.store;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.MarketPriceListCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.exception.ItemNotFoundException;

@Component
public class MarketPairToPriceStore extends TronStoreWithRevoking<MarketPriceListCapsule> {

  @Autowired
  protected MarketPairToPriceStore(@Value("market_pair_to_price") String dbName) {
    super(dbName);
  }

  @Override
  public MarketPriceListCapsule get(byte[] key) throws ItemNotFoundException {
    byte[] value = revokingDB.get(key);
    return new MarketPriceListCapsule(value);
  }
}