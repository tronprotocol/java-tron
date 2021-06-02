package org.tron.core.store;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.AbiCapsule;
import org.tron.core.db.TronStoreWithRevoking;

@Slf4j(topic = "DB")
@Component
public class AbiStore extends TronStoreWithRevoking<AbiCapsule> {

  @Autowired
  private AbiStore(@Value("abi") String dbName) {
    super(dbName);
  }

  @Override
  public AbiCapsule get(byte[] key) {
    return getUnchecked(key);
  }
}
