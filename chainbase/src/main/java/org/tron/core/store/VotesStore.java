package org.tron.core.store;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.VotesCapsule;
import org.tron.core.db.TronStoreWithRevoking;

@Component
public class VotesStore extends TronStoreWithRevoking<VotesCapsule> {

  @Autowired
  public VotesStore(@Value("votes") String dbName) {
    super(dbName, VotesCapsule.class);
  }

  @Override
  public VotesCapsule get(byte[] key) {
    return getNonEmpty(key);
  }
}