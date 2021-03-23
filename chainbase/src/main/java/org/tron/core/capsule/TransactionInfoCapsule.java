package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.exception.BadItemException;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionInfo.Log;
import org.tron.protos.Protocol.TransactionInfo.code;

@Slf4j(topic = "capsule")
public class TransactionInfoCapsule implements ProtoCapsule<TransactionInfo> {

  private TransactionInfo transactionInfo;

  /**
   * constructor TransactionCapsule.
   */
  public TransactionInfoCapsule(TransactionInfo trxRet) {
    this.transactionInfo = trxRet;
  }

  public TransactionInfoCapsule(byte[] data) throws BadItemException {
    try {
      this.transactionInfo = TransactionInfo.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      throw new BadItemException("TransactionInfoCapsule proto data parse exception");
    }
  }

  public TransactionInfoCapsule() {
    this.transactionInfo = TransactionInfo.newBuilder().build();
  }


  public long getPackingFee() {
    return transactionInfo.getPackingFee();
  }

  public long getFee() {
    return transactionInfo.getFee();
  }

  public void setFee(long fee) {
    this.transactionInfo = this.transactionInfo.toBuilder().setFee(fee).build();
  }

  public byte[] getId() {
    return transactionInfo.getId().toByteArray();
  }

  public void setId(byte[] id) {
    this.transactionInfo = this.transactionInfo.toBuilder()
        .setId(ByteString.copyFrom(id)).build();
  }

  public long getUnfreezeAmount() {
    return transactionInfo.getUnfreezeAmount();
  }

  public void setUnfreezeAmount(long amount) {
    this.transactionInfo = this.transactionInfo.toBuilder().setUnfreezeAmount(amount).build();
  }

  public long getWithdrawAmount() {
    return transactionInfo.getWithdrawAmount();
  }

  public void setWithdrawAmount(long amount) {
    this.transactionInfo = this.transactionInfo.toBuilder().setWithdrawAmount(amount).build();
  }

  public void setResult(code result) {
    this.transactionInfo = this.transactionInfo.toBuilder().setResult(result).build();
  }

  public void setResMessage(String message) {
    this.transactionInfo = this.transactionInfo.toBuilder()
        .setResMessage(ByteString.copyFromUtf8(message)).build();
  }

  public void addFee(long fee) {
    this.transactionInfo = this.transactionInfo.toBuilder()
        .setFee(this.transactionInfo.getFee() + fee).build();
  }

  public long getBlockNumber() {
    return transactionInfo.getBlockNumber();
  }

  public void setBlockNumber(long num) {
    this.transactionInfo = this.transactionInfo.toBuilder().setBlockNumber(num)
        .build();
  }

  public long getBlockTimeStamp() {
    return transactionInfo.getBlockTimeStamp();
  }

  public void setBlockTimeStamp(long time) {
    this.transactionInfo = this.transactionInfo.toBuilder().setBlockTimeStamp(time)
        .build();
  }

  public void setContractResult(byte[] ret) {
    this.transactionInfo = this.transactionInfo.toBuilder()
        .addContractResult(ByteString.copyFrom(ret))
        .build();
  }

  public void setContractAddress(byte[] contractAddress) {
    this.transactionInfo = this.transactionInfo.toBuilder()
        .setContractAddress(ByteString.copyFrom(contractAddress))
        .build();
  }

  public void setReceipt(ReceiptCapsule receipt) {
    this.transactionInfo = this.transactionInfo.toBuilder()
        .setReceipt(receipt.getReceipt())
        .build();
  }

  public void addAllLog(List<Log> logs) {
    this.transactionInfo = this.transactionInfo.toBuilder()
        .addAllLog(logs)
        .build();
  }

  @Override
  public byte[] getData() {
    return this.transactionInfo.toByteArray();
  }

  @Override
  public TransactionInfo getInstance() {
    return this.transactionInfo;
  }
}