package org.tron.core.store;

import java.util.Objects;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.db.TronStoreWithRevoking;

@Component
public class NullifierStore extends TronStoreWithRevoking<BytesCapsule> {

  @Autowired
  public NullifierStore(@Value("nullifier") String dbName) {
    super(dbName, BytesCapsule.class);
  }

  public void put(BytesCapsule bytesCapsule) {
    put(bytesCapsule.getData(), new BytesCapsule(bytesCapsule.getData()));
  }

  @Override
  public BytesCapsule get(byte[] key) {
    return getNonEmpty(key);

  }

  @Override
  public boolean has(byte[] key) {
    BytesCapsule value = get(key);
    return Objects.nonNull(value);
  }
}