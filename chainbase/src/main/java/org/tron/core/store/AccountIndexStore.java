package org.tron.core.store;

import com.google.protobuf.ByteString;
import java.util.Objects;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.state.worldstate.StateType;
import org.tron.core.state.worldstate.WorldStateCallBackUtils;

@Component
public class AccountIndexStore extends TronStoreWithRevoking<BytesCapsule> {

  @Autowired
  private WorldStateCallBackUtils worldStateCallBackUtils;

  @Autowired
  public AccountIndexStore(@Value("account-index") String dbName) {
    super(dbName);
  }

  public void put(AccountCapsule accountCapsule) {
    byte[] key = accountCapsule.getAccountName().toByteArray();
    BytesCapsule value = new BytesCapsule(
        accountCapsule.getAddress().toByteArray());
    put(key, value);
    worldStateCallBackUtils.callBack(StateType.AccountIndex, key, value);
  }

  public byte[] get(ByteString name) {
    BytesCapsule bytesCapsule = get(name.toByteArray());
    if (Objects.nonNull(bytesCapsule)) {
      return bytesCapsule.getData();
    }
    return null;
  }

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
    return !ArrayUtils.isEmpty(value);
  }
}