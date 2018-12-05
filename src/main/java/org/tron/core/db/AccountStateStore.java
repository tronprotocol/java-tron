package org.tron.core.db;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.db2.common.DB;
import org.tron.core.trie.AccountCallBack;
import org.tron.core.trie.TrieImpl;

@Slf4j
@Component
public class AccountStateStore extends TronStoreWithRevoking<BytesCapsule> implements
    DB<byte[], BytesCapsule> {

  @Autowired
  private DynamicPropertiesStore dynamicPropertiesStore;

  @Autowired
  private AccountStateStore(@Value("accountState") String dbName) {
    super(dbName);
  }

  public AccountCapsule getAccount(byte[] key) {
    long latestNumber = dynamicPropertiesStore.getLatestBlockHeaderNumber();
    byte[] rootHash = AccountCallBack.rootHashCache.getIfPresent(latestNumber);
    return getAccount(key, rootHash);
  }

  public AccountCapsule getAccount(byte[] key, byte[] rootHash) {
    TrieImpl trie = new TrieImpl(this, rootHash);
    byte[] value = trie.get(key);
    return ArrayUtils.isEmpty(value) ? null : new AccountCapsule(value);
  }

  @Override
  public boolean isEmpty() {
    return super.size() <= 0;
  }

  @Override
  public void remove(byte[] bytes) {
    super.delete(bytes);
  }

  @Override
  public BytesCapsule get(byte[] key) {
    return super.getUnchecked(key);
  }

  @Override
  public void put(byte[] key, BytesCapsule item) {
    logger.info("put key: {}", ByteUtil.toHexString(key));
    super.put(key, item);
  }
}
