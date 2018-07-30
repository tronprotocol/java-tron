package org.tron.core.db;

import java.util.Objects;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.BytesCapsule;

@Component
public class AccountContractIndexStore extends TronStoreWithRevoking<BytesCapsule> {

  @Autowired
  public AccountContractIndexStore(@Value("account-contract-index") String dbName) {
    super(dbName);
  }

  @Override
  public BytesCapsule get(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);
    if (Objects.nonNull(value)) {
      return new BytesCapsule(value);
    }
    return null;
  }

  public void put(BytesCapsule normalAccountAddress, BytesCapsule contractAddress) {
    put(normalAccountAddress.getData(), contractAddress);
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