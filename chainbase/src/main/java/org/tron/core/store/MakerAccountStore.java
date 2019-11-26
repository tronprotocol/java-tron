package org.tron.core.store;

import com.google.common.collect.Streams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.ExchangeCapsule;
import org.tron.core.capsule.MakerAccountOrderCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.exception.ItemNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MakerAccountStore extends TronStoreWithRevoking<MakerAccountOrderCapsule> {

  @Autowired
  protected MakerAccountStore(@Value("maker_account") String dbName) {
    super(dbName);
  }

  @Override
  public MakerAccountOrderCapsule get(byte[] key) throws ItemNotFoundException {
    byte[] value = revokingDB.get(key);
    return new MakerAccountOrderCapsule(value);
  }

}