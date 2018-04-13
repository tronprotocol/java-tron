package org.tron.core.db;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.BytesCapsule;

@Slf4j
public class WitnessScheduleStore extends TronStoreWithRevoking<BytesCapsule>{
  private static final byte[] ACTIVE_WITNESSES = "active_witnesses".getBytes();
  private static final byte[] CURRENT_SHUFFLED_WITNESSES = "current_shuffled_witnesses".getBytes();

  private WitnessScheduleStore(String dbName) {
    super(dbName);
    try {
      this.getActiveWitnesses();
    } catch (IllegalArgumentException e) {
      List<String> al = new ArrayList<>();
      this.saveActiveWitnesses(al);
    }

    try {
      this.getCurrentShuffledWitnesses();
    } catch (IllegalArgumentException e) {
      List<String> al = new ArrayList<>();
      this.saveCurrentShuffledWitnesses(al);
    }
  }


  @Override
  public void delete(byte[] key) {

  }

  @Override
  public BytesCapsule get(byte[] key) {
    return null;
  }

  @Override
  public boolean has(byte[] key) {
    return false;
  }

  private static WitnessScheduleStore instance;

  public void destroy() {
    instance = null;
  }

  /**
   * create fun.
   *
   * @param dbName the name of database
   */

  public static WitnessScheduleStore create(String dbName) {
    if (instance == null) {
      synchronized (WitnessScheduleStore.class) {
        if (instance == null) {
          instance = new WitnessScheduleStore(dbName);
        }
      }
    }
    return instance;
  }


  public void saveActiveWitnesses(List<String> witnessesAddressList) {
    logger.debug("ActiveWitnesses:" + witnessesAddressList);
    StringBuffer sb = new StringBuffer();
    witnessesAddressList.forEach(address -> sb.append(address).append("&"));
    this.put(ACTIVE_WITNESSES,
        new BytesCapsule(ByteArray.fromString(sb.toString())));
  }

  public List<String> getActiveWitnesses() {
    List<String> witnessesAddressList = new ArrayList<>();
    return Optional.ofNullable(this.dbSource.getData(ACTIVE_WITNESSES))
        .map(ByteArray::toStr)
        .map((value) -> {
          StringTokenizer st = new StringTokenizer(value, "&");
          while (st.hasMoreElements()) {
            String strN = st.nextToken();
            witnessesAddressList.add(strN);
          }
          return witnessesAddressList;
        }).orElseThrow(
            () -> new IllegalArgumentException("not found latest SOLIDIFIED_BLOCK_NUM timestamp"));
  }

  public void saveCurrentShuffledWitnesses(List<String> witnessesAddressList) {
    logger.debug("CurrentShuffledWitnesses:" + witnessesAddressList);
    StringBuffer sb = new StringBuffer();
    witnessesAddressList.forEach(address -> sb.append(address).append("&"));
    this.put(CURRENT_SHUFFLED_WITNESSES,
        new BytesCapsule(ByteArray.fromString(sb.toString())));
  }

  public List<String> getCurrentShuffledWitnesses() {
    List<String> witnessesAddressList = new ArrayList<>();
    return Optional.ofNullable(this.dbSource.getData(CURRENT_SHUFFLED_WITNESSES))
        .map(ByteArray::toStr)
        .map((value) -> {
          StringTokenizer st = new StringTokenizer(value, "&");
          while (st.hasMoreElements()) {
            String strN = st.nextToken();
            witnessesAddressList.add(strN);
          }
          return witnessesAddressList;
        }).orElseThrow(
            () -> new IllegalArgumentException("not found CurrentShuffledWitnesses"));
  }

}
