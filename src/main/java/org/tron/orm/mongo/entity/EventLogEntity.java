package org.tron.orm.mongo.entity;

import com.alibaba.fastjson.JSONObject;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.repository.Query;
import java.io.Serializable;

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

    @Field(value = "event_index")
    private int eventIdx;

    @Field(value = "event_name")
    private String entryName;

    @Field(value = "result")
    private JSONObject resultJsonObject;

    @Field(value = "raw")
    private JSONObject rawJsonObject;

    @Field(value = "transaction_id")
    private String transactionId;

    @Field(value = "result_type")
    private JSONObject resultType;

    @Field(value = "resource_Node")
    private String resourceNode;

    @Id
    private String id;


    public EventLogEntity(long blockNumber, long blockTimestamp, String contractAddress, String entryName,
                          JSONObject resultJsonObject, JSONObject rawJsonObject, String transactionId,
                          JSONObject result_type, String resource, int eventindex) {
        this.blockNumber = blockNumber;
        this.blockTimestamp = blockTimestamp;
        this.contractAddress = contractAddress;
        this.entryName = entryName;
        this.resultJsonObject = resultJsonObject;
        this.rawJsonObject = rawJsonObject;
        this.transactionId = transactionId;
        this.resultType = result_type;
        this.resourceNode = resource;
        this.eventIdx = eventindex;
        this.id = this.resourceNode + "-" + this.transactionId + "-" + Integer.toString(this.eventIdx);
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

    public JSONObject getResultJsonObject() {
        return resultJsonObject;
    }

    public void setResultJsonObject(JSONObject resultJsonObject) {
        this.resultJsonObject = resultJsonObject;
    }

    public JSONObject getRawJsonObject() {
        return rawJsonObject;
    }

    public void setRawJsonObject(JSONObject rawJsonObject) {
        this.rawJsonObject = rawJsonObject;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public void setResultType(JSONObject res){ this.resultType = res; }

    public JSONObject getResultType(){ return  this.resultType; }

    public void setResourceNode(String resource) { this.resourceNode = resource ;}

    public String getResourceNode(){ return this.resourceNode; }

    public void setEventIdx(int idx){this.eventIdx = idx;}

    public int getEventIdx(){return this.eventIdx;}
}
