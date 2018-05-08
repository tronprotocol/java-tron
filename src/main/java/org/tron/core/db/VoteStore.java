package org.tron.core.db;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.AccountCapsule;

@Component
public class VoteStore extends TronStoreWithRevoking<AccountCapsule> {

  @Autowired
  public VoteStore(@Qualifier("vote") String dbName) {
    super(dbName);
  }

  private static VoteStore instance;

  public static void destroy() {
    instance = null;
  }

  /**
   * create fun.
   *
   * @param dbName the name of database
   */
  public static VoteStore create(String dbName) {
    if (instance == null) {
      synchronized (VoteStore.class) {
        if (instance == null) {
          instance = new VoteStore(dbName);
        }
      }
    }
    return instance;
  }

  @Override
  public AccountCapsule get(byte[] key) {
    byte[] value = dbSource.getData(key);
    return ArrayUtils.isEmpty(value) ? null : new AccountCapsule(value);
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
  public void put(byte[] key, AccountCapsule item) {
    super.put(key, item);
    if (indexHelper != null) {
      indexHelper.update(item.getInstance());
    }
  }

  /**
   * get all accounts.
   */
  public List<AccountCapsule> getAllVotes() {
    return dbSource
        .allValues()
        .stream()
        .map(bytes -> new AccountCapsule(bytes))
        .collect(Collectors.toList());
  }
}