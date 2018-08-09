package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.exception.BadItemException;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Result;

@Slf4j
public class TransactionResultCapsule implements ProtoCapsule<Transaction.Result> {

  private Transaction.Result transactionResult;

  /**
   * constructor TransactionCapsule.
   */
  public TransactionResultCapsule(Transaction.Result trxRet) {
    this.transactionResult = trxRet;
  }

  public TransactionResultCapsule(byte[] data) throws BadItemException {
    try {
      this.transactionResult = Transaction.Result.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      throw new BadItemException("TransactionResult proto data parse exception");
    }
  }

  public TransactionResultCapsule() {
    this.transactionResult = Transaction.Result.newBuilder().build();
  }

  public TransactionResultCapsule(Transaction.Result.code code, long fee) {
    this.transactionResult = Transaction.Result.newBuilder().setRet(code).setFee(fee).build();
  }

  public void setStatus(long fee, Transaction.Result.code code) {
    long oldValue = transactionResult.getFee();
    this.transactionResult = this.transactionResult.toBuilder()
        .setFee(oldValue + fee)
        .setRet(code).build();
  }

  public long getFee() {
    return transactionResult.getFee();
  }

  public void setUnfreezeAmount(long amount) {
    this.transactionResult = this.transactionResult.toBuilder().setUnfreezeAmount(amount).build();
  }

  public long getUnfreezeAmount() {
    return transactionResult.getUnfreezeAmount();
  }

  public void setWithdrawAmount(long amount) {
    this.transactionResult = this.transactionResult.toBuilder().setWithdrawAmount(amount).build();
  }

  public long getWithdrawAmount() {
    return transactionResult.getWithdrawAmount();
  }

  public void setFee(long fee) {
    this.transactionResult = this.transactionResult.toBuilder().setFee(fee).build();
  }

  public void addFee(long fee) {
    this.transactionResult = this.transactionResult.toBuilder()
        .setFee(this.transactionResult.getFee() + fee).build();
  }

  public void setErrorCode(Transaction.Result.code code) {
    this.transactionResult = this.transactionResult.toBuilder().setRet(code).build();
  }

  @Override
  public byte[] getData() {
    return this.transactionResult.toByteArray();
  }

  @Override
  public Result getInstance() {
    return this.transactionResult;
  }
}