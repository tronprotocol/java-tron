package org.tron.core.store;

import com.google.common.collect.Streams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.ExchangeCapsule;
import org.tron.core.capsule.MakerOrderIdListCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.exception.ItemNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.tron.protos.Protocol.MakerOrderIdList;

@Component
public class MakerPairPriceToOrderStore extends TronStoreWithRevoking<MakerOrderIdListCapsule> {

  @Autowired
  protected MakerPairPriceToOrderStore(@Value("maker_pair_price_to_order") String dbName) {
    super(dbName);
  }

  @Override
  public MakerOrderIdListCapsule get(byte[] key) throws ItemNotFoundException {
    byte[] value = revokingDB.get(key);
    return new MakerOrderIdListCapsule(value);
  }

}