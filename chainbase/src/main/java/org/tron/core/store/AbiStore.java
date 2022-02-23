package org.tron.core.store;

import com.google.common.collect.Streams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.AbiCapsule;
import org.tron.core.db.TronStoreWithRevoking;

import java.util.Objects;

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

  public void put(byte[] key, byte[] value) {
    if (Objects.isNull(key) || Objects.isNull(value)) {
      return;
    }

    revokingDB.put(key, value);
  }

  public long getTotalABIs() {
    return Streams.stream(revokingDB.iterator()).count();
  }
}
