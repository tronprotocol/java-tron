package org.tron.core.db;

import static org.tron.core.db.fast.FastSyncStoreConstant.TrieEnum.VOTES;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.VotesCapsule;
import org.tron.core.db.fast.callback.FastSyncCallBack;
import org.tron.core.db.fast.storetrie.VotesStoreTrie;

@Component
public class VotesStore extends TronStoreWithRevoking<VotesCapsule> {

  @Autowired
  private FastSyncCallBack fastSyncCallBack;

  @Autowired
  private VotesStoreTrie votesStoreTrie;

  @Autowired
  public VotesStore(@Value("votes") String dbName) {
    super(dbName);
  }

  @Override
  public VotesCapsule get(byte[] key) {
    byte[] value = getValue(key);
    return ArrayUtils.isEmpty(value) ? null : new VotesCapsule(value);
  }

  @Override
  public void put(byte[] key, VotesCapsule item) {
    super.put(key, item);
    fastSyncCallBack.callBack(key, item.getData(), VOTES);
  }

  @Override
  public void delete(byte[] key) {
    super.delete(key);
    fastSyncCallBack.delete(key, VOTES);
  }

  public byte[] getValue(byte[] key) {
    byte[] value = votesStoreTrie.getValue(key);
    if (ArrayUtils.isEmpty(value)) {
      value = revokingDB.getUnchecked(key);
    }
    return value;
  }

  @Override
  public void close() {
    super.close();
    votesStoreTrie.close();
  }
}