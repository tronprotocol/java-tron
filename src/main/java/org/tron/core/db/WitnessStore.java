package org.tron.core.db;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.utils.ByteArray;

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

  public boolean countvoteWitness(ByteString voteAddress, int countAdd) {
    logger.info("voteAddress is {},voteAddCount is {}", voteAddress, countAdd);
    int count = 0;
    byte[] value = dbSource.getData(voteAddress.toByteArray());
    if (null != value) {
      count = ByteArray.toInt(value);
    }

    logger.info("voteAddress pre-voteCount is {}", count);
    count += countAdd;
    dbSource.putData(voteAddress.toByteArray(), ByteArray.fromInt(count));
    logger.info("voteAddress after-voteCount is {}", count);
    return true;
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
