package org.tron.core.store;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.db.TronStoreWithRevoking;

@Slf4j
@Component
public class SlashStore extends TronStoreWithRevoking<BytesCapsule> {

  @Autowired
  public SlashStore(@Value("slash") String dbName) {
    super(dbName);
  }

  @Override
  public BytesCapsule get(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);
    return ArrayUtils.isEmpty(value) ? null : new BytesCapsule(value);
  }

  public long getWitnessMissCount(byte[] address) {
    BytesCapsule bytesCapsule = get(buildMissCountKey(address));
    if (bytesCapsule == null) {
      return 0;
    } else {
      return ByteArray.toLong(bytesCapsule.getData());
    }
  }

  public void setWitnessMissCount(byte[] address, long value) {
    put(buildMissCountKey(address), new BytesCapsule(ByteArray.fromLong(value)));
  }

  public void deleteWitnessMissCount(byte[] address) {
    if (has(buildMissCountKey(address))) {
      delete(buildMissCountKey(address));
    }
  }

  private byte[] buildMissCountKey(byte[] address) {
    return ("miss-count-" + Hex.toHexString(address)).getBytes();
  }
}
