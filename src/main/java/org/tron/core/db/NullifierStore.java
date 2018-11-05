package org.tron.core.db;

import com.google.protobuf.ByteString;
import java.util.Objects;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BytesCapsule;

@Component
public class NullifierStore extends TronStoreWithRevoking<BytesCapsule> {

  @Autowired
  public NullifierStore(@Value("nullifier") String dbName) {
    super(dbName);
  }

  public void put(BytesCapsule bytesCapsule) {
    put(bytesCapsule.getData(), new BytesCapsule(bytesCapsule.getData()));
  }

  // public byte[] get(byte[] key) {
  //   BytesCapsule bytesCapsule = this.get(key);
  //   if (Objects.nonNull(bytesCapsule)) {
  //     return bytesCapsule.getData();
  //   }
  //   return null;
  // }

  @Override
  public BytesCapsule get(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);
    if (ArrayUtils.isEmpty(value)) {
      return null;
    }
    return new BytesCapsule(value);
  }

  @Override
  public boolean has(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);
    if (ArrayUtils.isEmpty(value)) {
      return false;
    }
    return true;
  }
}