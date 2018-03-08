package org.tron.core.db;

import com.google.protobuf.ByteString;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.WitnessCapsule;

public class WitnessStore extends TronDatabase<WitnessCapsule> {

  private static final Logger logger = LoggerFactory.getLogger("WitnessStore");

  protected WitnessStore(String dbName) {
    super(dbName);
  }

  @Override
  public void put(byte[] key, WitnessCapsule item) {

  }

  @Override
  public void delete(byte[] key) {

  }

  @Override
  public WitnessCapsule get(byte[] key) {
    return null;
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

  /**
   * get all witnesses.
   */
  public List<WitnessCapsule> getAllWitnesses() {
    return dbSource.allKeys().stream()
        .map(key -> get(key))
        .collect(Collectors.toList());
  }

}
