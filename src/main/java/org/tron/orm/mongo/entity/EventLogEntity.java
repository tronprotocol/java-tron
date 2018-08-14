package org.tron.orm.mongo.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.List;

/**
 * MongoDB collection eventLog 映射的实体类
 */
@Document
public class EventLogEntity implements Serializable {

  private static final long serialVersionUID = -70777625567836430L;

  private long blockNumber;

  private long blockTimestamp;

  private String contractAddressHexString;

  private String entryName;

  private List<Integer> resultJsonArray;

  public static long getSerialVersionUID() {
    return serialVersionUID;
  }

  public long getBlockNumber() {
    return blockNumber;
  }

  public void setBlockNumber(long blockNumber) {
    this.blockNumber = blockNumber;
  }

  public long getBlockTimestamp() {
    return blockTimestamp;
  }

  public void setBlockTimestamp(long blockTimestamp) {
    this.blockTimestamp = blockTimestamp;
  }

  public String getContractAddressHexString() {
    return contractAddressHexString;
  }

  public void setContractAddressHexString(String contractAddressHexString) {
    this.contractAddressHexString = contractAddressHexString;
  }

  public String getEntryName() {
    return entryName;
  }

  public void setEntryName(String entryName) {
    this.entryName = entryName;
  }

  public List<Integer> getResultJsonArray() {
    return resultJsonArray;
  }

  public void setResultJsonArray(List<Integer> resultJsonArray) {
    this.resultJsonArray = resultJsonArray;
  }
}
