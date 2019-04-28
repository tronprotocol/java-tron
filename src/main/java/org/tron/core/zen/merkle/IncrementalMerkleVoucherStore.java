package org.tron.core.zen.merkle;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.db.TronStoreWithRevoking;

@Component
public class IncrementalMerkleVoucherStore
    extends TronStoreWithRevoking<IncrementalMerkleVoucherCapsule> {

  @Autowired
  public IncrementalMerkleVoucherStore(@Value("IncrementalMerkleVoucher") String dbName) {
    super(dbName);
  }

  @Override
  public IncrementalMerkleVoucherCapsule get(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);
    return ArrayUtils.isEmpty(value) ? null : new IncrementalMerkleVoucherCapsule(value);
  }
}
