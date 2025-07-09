package org.tron.common.storage;

import org.tron.common.setting.RocksDbSettings;
import org.tron.common.utils.StorageUtils;

public class OptionsPicker {

  protected org.iq80.leveldb.Options getOptionsByDbNameForLevelDB(String dbName) {
    return StorageUtils.getOptionsByDbName(dbName);
  }

  protected org.rocksdb.Options getOptionsByDbNameForRocksDB(String dbName) {
    return RocksDbSettings.getOptionsByDbName(dbName);
  }
}
