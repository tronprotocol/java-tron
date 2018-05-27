package org.tron.core.db;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.VotesCapsule;

@Component
public class VotesStore extends TronStoreWithRevoking<VotesCapsule> {

  @Autowired
  public VotesStore(@Value("votes") String dbName) {
    super(dbName);
  }

  @Override
  public VotesCapsule get(byte[] key) {
    byte[] value = dbSource.getData(key);
    return ArrayUtils.isEmpty(value) ? null : new VotesCapsule(value);
  }

  /**
   * isVoterExist fun.
   *
   * @param key the address of Voter Account
   */
  @Override
  public boolean has(byte[] key) {
    byte[] account = dbSource.getData(key);
    return null != account;
  }

  @Override
  public void put(byte[] key, VotesCapsule item) {
    super.put(key, item);
  }

  /**
   * get all votes.
   */
  public List<VotesCapsule> getAllVotes() {
    return dbSource
        .allValues()
        .stream()
        .map(bytes -> new VotesCapsule(bytes))
        .collect(Collectors.toList());
  }
}