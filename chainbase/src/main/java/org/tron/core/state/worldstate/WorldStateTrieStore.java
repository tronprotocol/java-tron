package org.tron.core.state.worldstate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.db2.common.DB;

@Slf4j(topic = "State")
@Component
public class WorldStateTrieStore extends TronStoreWithRevoking<BytesCapsule> implements
    DB<byte[], BytesCapsule> {

  @Autowired
  private WorldStateTrieStore(@Value("world-state-trie") String dbName) {
    super(dbName);
  }

  @Override
  public void put(byte[] key, BytesCapsule item) {
    super.put(key, item);
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
  public DB<byte[], BytesCapsule> newInstance() {
    return null;
  }

  @Override
  public void stat() {

  }

}
