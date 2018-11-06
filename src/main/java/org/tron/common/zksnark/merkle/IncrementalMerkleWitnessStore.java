package org.tron.common.zksnark.merkle;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.db.TronStoreWithRevoking;

@Component
public class IncrementalMerkleWitnessStore extends
    TronStoreWithRevoking<IncrementalMerkleWitnessCapsule> {

  @Autowired
  public IncrementalMerkleWitnessStore(@Value("IncrementalMerkleWitness") String dbName) {
    super(dbName);
  }

  @Override
  public IncrementalMerkleWitnessCapsule get(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);
    return ArrayUtils.isEmpty(value) ? null : new IncrementalMerkleWitnessCapsule(value);
  }
}