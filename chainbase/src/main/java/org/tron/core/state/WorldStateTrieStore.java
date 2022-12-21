package org.tron.core.state;

import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.storage.rocksdb.RocksDbDataSourceImpl;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.db2.common.DB;
import org.tron.core.db2.common.RocksDB;

@Slf4j(topic = "State")
@Component
public class WorldStateTrieStore extends TronStoreWithRevoking<BytesCapsule> implements
    DB<byte[], BytesCapsule> {

  @Autowired
  private WorldStateTrieStore(@Value("world-state-trie") String dbName) {
    super(new RocksDB(new RocksDbDataSourceImpl(calDbParentPath(),
        dbName, CommonParameter.getInstance().getRocksDBCustomSettings())));
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

  private static String calDbParentPath() {
    String stateGenesis = CommonParameter.getInstance().getStorage()
        .getStateGenesisDirectory();
    if (Paths.get(stateGenesis).isAbsolute()) {
      return stateGenesis;
    } else {
      return Paths.get(CommonParameter.getInstance().getOutputDirectory(),
          stateGenesis).toString();
    }
  }

}
