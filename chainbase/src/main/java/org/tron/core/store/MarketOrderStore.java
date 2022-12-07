package org.tron.core.store;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.MarketOrderCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.state.worldstate.StateType;
import org.tron.core.state.worldstate.WorldStateCallBackUtils;
import org.tron.core.exception.ItemNotFoundException;

@Component
public class MarketOrderStore extends TronStoreWithRevoking<MarketOrderCapsule> {

  @Autowired
  private WorldStateCallBackUtils worldStateCallBackUtils;

  @Autowired
  protected MarketOrderStore(@Value("market_order") String dbName) {
    super(dbName);
  }

  @Override
  public MarketOrderCapsule get(byte[] key) throws ItemNotFoundException {
    byte[] value = revokingDB.get(key);
    return new MarketOrderCapsule(value);
  }

  @Override
  public void put(byte[] key, MarketOrderCapsule item) {
    super.put(key, item);
    worldStateCallBackUtils.callBack(StateType.MarketOrder, key, item);
  }

}