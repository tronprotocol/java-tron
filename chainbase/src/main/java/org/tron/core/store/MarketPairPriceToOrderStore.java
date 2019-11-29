package org.tron.core.store;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.MarketOrderIdListCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.exception.ItemNotFoundException;

@Component
public class MarketPairPriceToOrderStore extends TronStoreWithRevoking<MarketOrderIdListCapsule> {

  @Autowired
  protected MarketPairPriceToOrderStore(@Value("market_pair_price_to_order") String dbName) {
    super(dbName);
  }

  @Override
  public MarketOrderIdListCapsule get(byte[] key) throws ItemNotFoundException {
    byte[] value = revokingDB.get(key);
    return new MarketOrderIdListCapsule(value);
  }

}