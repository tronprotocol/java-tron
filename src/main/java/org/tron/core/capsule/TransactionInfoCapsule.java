package org.tron.core.capsule;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.exception.BadItemException;
import org.tron.protos.Protocol.TransactionInfo;

@Slf4j
public class TransactionInfoCapsule implements ProtoCapsule<TransactionInfo> {

  private TransactionInfo transactionInfoCapsule;

  /**
   * constructor TransactionCapsule.
   */
  public TransactionInfoCapsule(TransactionInfo trxRet) {
    this.transactionInfoCapsule = trxRet;
  }

  public TransactionInfoCapsule(byte[] data) throws BadItemException {
    try {
      this.transactionInfoCapsule = TransactionInfo.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      throw new BadItemException("TransactionInfoCapsule proto data parse exception");
    }
  }

  public TransactionInfoCapsule() {
    this.transactionInfoCapsule = TransactionInfo.newBuilder().build();
  }

  public long getFee() {
    return transactionInfoCapsule.getFee();
  }

  public void setFee(long fee) {
    this.transactionInfoCapsule = this.transactionInfoCapsule.toBuilder().setFee(fee).build();
  }

  public void addFee(long fee) {
    this.transactionInfoCapsule = this.transactionInfoCapsule.toBuilder()
        .setFee(this.transactionInfoCapsule.getFee() + fee).build();
  }

  public long getBlockNumber() {
    return transactionInfoCapsule.getBlockNumber();
  }

  public void setBlockNumber(long num) {
    this.transactionInfoCapsule = this.transactionInfoCapsule.toBuilder().setBlockNumber(num)
        .build();
  }

  public long getBlockTimeStamp() {
    return transactionInfoCapsule.getBlockTimeStamp();
  }

  public void setBlockTimeStamp(long time) {
    this.transactionInfoCapsule = this.transactionInfoCapsule.toBuilder().setBlockTimeStamp(time)
        .build();
  }

  @Override
  public byte[] getData() {
    return this.transactionInfoCapsule.toByteArray();
  }

  @Override
  public TransactionInfo getInstance() {
    return this.transactionInfoCapsule;
  }
}