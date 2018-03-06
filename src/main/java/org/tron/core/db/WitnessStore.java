package org.tron.core.db;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.protos.Protocal.Witness;

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

  public Witness getWitness(ByteString voteAddress) {
    logger.info("voteAddress is {} ", voteAddress);

    try {
      byte[] value = dbSource.getData(voteAddress.toByteArray());
      if (null == value) {
        return null;
      }
      return Witness.parseFrom(value);
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    return null;
  }

  public void putWitness(Witness witness) {
    logger.info("voteAddress is {} ", witness.getAddress());

    dbSource.putData(witness.getAddress().toByteArray(), witness.toByteArray());
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
