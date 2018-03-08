package org.tron.core.db;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.capsule.WitnessCapsule;

public class WitnessStore extends TronDatabase {

  private static final Logger logger = LoggerFactory.getLogger("WitnessStore");

  protected WitnessStore(String dbName) {
    super(dbName);
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

  public WitnessCapsule getWitness(ByteString voteAddress) {
    logger.info("voteAddress is {} ", voteAddress);

    byte[] value = dbSource.getData(voteAddress.toByteArray());
    if (null == value) {
      return null;
    }
    return new WitnessCapsule(value);
  }

  public void putWitness(WitnessCapsule witnessCapsule) {
    logger.info("voteAddress is {} ", witnessCapsule.getAddress());

    dbSource.putData(witnessCapsule.getAddress().toByteArray(), witnessCapsule.getData());
  }

  @Override
  void add() {

  }

  @Override
  void del() {

  }

  @Override
  void fetch() {

  }
}
