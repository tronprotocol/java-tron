package org.tron.core.store;

import com.google.common.collect.Streams;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.ExchangeCapsule;
import org.tron.core.capsule.MakerOrderCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.exception.ItemNotFoundException;

@Component
public class MakerOrderStore extends TronStoreWithRevoking<MakerOrderCapsule> {

  @Autowired
  protected MakerOrderStore(@Value("maker_order") String dbName) {
    super(dbName);
  }

  @Override
  public MakerOrderCapsule get(byte[] key) throws ItemNotFoundException {
    byte[] value = revokingDB.get(key);
    return new MakerOrderCapsule(value);
  }

}