package org.tron.core.store;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.IncrementalMerkleTreeCapsule;
import org.tron.core.db.TronStoreWithRevoking;

@Slf4j(topic = "DB")
@Component
public class IncrementalMerkleTreeStore
    extends TronStoreWithRevoking<IncrementalMerkleTreeCapsule> {

  @Autowired
  public IncrementalMerkleTreeStore(@Value("IncrementalMerkleTree") String dbName) {
    super(dbName, IncrementalMerkleTreeCapsule.class);
  }

  @Override
  public IncrementalMerkleTreeCapsule get(byte[] key) {
    return getNonEmpty(key);
  }

  public boolean contain(byte[] key) {
    IncrementalMerkleTreeCapsule value = get(key);
    return Objects.nonNull(value);
  }

}
