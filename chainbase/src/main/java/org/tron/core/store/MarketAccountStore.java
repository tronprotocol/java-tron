package org.tron.core.store;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.MarketAccountOrderCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.db.accountstate.StateType;
import org.tron.core.db.accountstate.WorldStateCallBackUtils;
import org.tron.core.exception.ItemNotFoundException;

@Component
public class MarketAccountStore extends TronStoreWithRevoking<MarketAccountOrderCapsule> {

  @Autowired
  private WorldStateCallBackUtils worldStateCallBackUtils;

  @Autowired
  protected MarketAccountStore(@Value("market_account") String dbName) {
    super(dbName);
  }

  @Override
  public MarketAccountOrderCapsule get(byte[] key) throws ItemNotFoundException {
    byte[] value = revokingDB.get(key);
    return new MarketAccountOrderCapsule(value);
  }

  @Override
  public void put(byte[] key, MarketAccountOrderCapsule item) {
    super.put(key, item);
    worldStateCallBackUtils.callBack(StateType.MarketAccount, key, item);
  }

}