package org.tron.core.db;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.capsule.WitnessCapsule;

public class WitnessStore extends TronStoreWithRevoking<WitnessCapsule> {

  private static final Logger logger = LoggerFactory.getLogger("WitnessStore");

  protected WitnessStore(String dbName) {
    super(dbName);
  }

  @Override
  public WitnessCapsule get(byte[] key) {
    byte[] value = dbSource.getData(key);
    return ArrayUtils.isEmpty(value) ? null : new WitnessCapsule(value);
  }

  @Override
  public boolean has(byte[] key) {
    return false;
  }

  private static WitnessStore instance;

  /**
   * create fun.
   *
   * @param dbName the name of database
   */
  public static WitnessStore create(String dbName) {
    if (instance == null) {
      synchronized (UtxoStore.class) {
        if (instance == null) {
          instance = new WitnessStore(dbName);
        }
      }
    }
    return instance;
  }

  /**
   * get all witnesses.
   */
  public List<WitnessCapsule> getAllWitnesses() {
    return dbSource.allValues().stream().map(bytes ->
        new WitnessCapsule(bytes)
    ).collect(Collectors.toList());
  }

}
