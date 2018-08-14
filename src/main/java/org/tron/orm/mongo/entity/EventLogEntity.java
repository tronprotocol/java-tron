package org.tron.orm.mongo.entity;

import com.alibaba.fastjson.JSONArray;
import java.io.Serializable;
import java.util.List;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * MongoDB collection eventLog
 */
@Document
public class EventLogEntity implements Serializable {

  private static final long serialVersionUID = -70777625567836430L;

  @Field(value = "block_number")
  private long blockNumber;

  @Field(value = "block_timestamp")
  private long blockTimestamp;

  @Field(value = "contract_address")
  private String contractAddress;

  @Field(value = "event_name")
  private String entryName;

  @Field(value = "result")
  private JSONArray resultJsonArray;

  @Field(value = "transaction_id")
  private String transactionId;

  public EventLogEntity(long blockNumber, long blockTimestamp, String contractAddress,
      String entryName, JSONArray resultJsonArray, String transactionId) {
    this.blockNumber = blockNumber;
    this.blockTimestamp = blockTimestamp;
    this.contractAddress = contractAddress;
    this.entryName = entryName;
    this.resultJsonArray = resultJsonArray;
    this.transactionId = transactionId;
  }

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

  public String getContractAddress() {
    return contractAddress;
  }

  public void setContractAddress(String contractAddress) {
    this.contractAddress = contractAddress;
  }

  public String getEntryName() {
    return entryName;
  }

  public void setEntryName(String entryName) {
    this.entryName = entryName;
  }

  public JSONArray getResultJsonArray() {
    return resultJsonArray;
  }

  public void setResultJsonArray(JSONArray resultJsonArray) {
    this.resultJsonArray = resultJsonArray;
  }
}
