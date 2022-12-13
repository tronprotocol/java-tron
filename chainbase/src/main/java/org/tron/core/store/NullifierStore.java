package org.tron.core.store;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.state.StateType;
import org.tron.core.state.WorldStateCallBackUtils;

@Component
public class NullifierStore extends TronStoreWithRevoking<BytesCapsule> {

  @Autowired
  private WorldStateCallBackUtils worldStateCallBackUtils;

  @Autowired
  public NullifierStore(@Value("nullifier") String dbName) {
    super(dbName);
  }

  public void put(BytesCapsule bytesCapsule) {
    put(bytesCapsule.getData(), new BytesCapsule(bytesCapsule.getData()));
    worldStateCallBackUtils.callBack(StateType.Nullifier, bytesCapsule.getData(), bytesCapsule);
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