package org.tron.core.db;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.common.utils.StringUtil;
import org.tron.core.capsule.WitnessCapsule;

@Slf4j
public class WitnessStore extends TronStoreWithRevoking<WitnessCapsule> {

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
    byte[] account = dbSource.getData(key);
    if (account == null) {
      //For debugging
      String readableWitnessAddress = StringUtil.createReadableString(account);
      List<String> allReadableWitnessAddress = StringUtil
          .getAddressStringListFromByteArray(dbSource.allKeys());
      logger.warn("address is {},witness is {},allWitness : ", key, readableWitnessAddress,
          allReadableWitnessAddress);
    }
    return null != account;
  }

  private static WitnessStore instance;

  public static void destory() {
    instance = null;
  }

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
