package org.tron.core.capsule;

import lombok.extern.slf4j.Slf4j;
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

  public TransactionResultCapsule() {
    this.transactionResult = Transaction.Result.newBuilder().build();
  }


  public TransactionResultCapsule(Transaction.Result.code code, long fee) {
    Transaction.Result ret = Transaction.Result.newBuilder().setRet(code).setFee(fee).build();
  }

  public void setStatus(long fee, Transaction.Result.code code) {
    this.transactionResult = this.transactionResult.toBuilder()
        .setFee(fee)
        .setRet(code).build();
  }

  public void setFee(long fee) {
    this.transactionResult = this.transactionResult.toBuilder().setFee(fee).build();
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
