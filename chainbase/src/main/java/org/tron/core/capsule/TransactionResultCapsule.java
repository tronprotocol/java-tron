package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.exception.BadItemException;
import org.tron.protos.Protocol.MarketOrderDetail;
import org.tron.protos.Protocol.Transaction.Result;
import org.tron.protos.Protocol.Transaction.Result.contractResult;

@Slf4j(topic = "capsule")
public class TransactionResultCapsule implements ProtoCapsule<Result> {

  private Result transactionResult;

  /**
   * constructor TransactionCapsule.
   */
  public TransactionResultCapsule(Result trxRet) {
    this.transactionResult = trxRet;
  }

  public TransactionResultCapsule(byte[] data) throws BadItemException {
    try {
      this.transactionResult = Result.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      throw new BadItemException("TransactionResult proto data parse exception");
    }
  }

  public TransactionResultCapsule() {
    this.transactionResult = Result.newBuilder().build();
  }

  public TransactionResultCapsule(contractResult code) {
    this.transactionResult = Result.newBuilder().setContractRet(code).build();
  }

  public TransactionResultCapsule(Result.code code, long fee) {
    this.transactionResult = Result.newBuilder().setRet(code).setFee(fee).build();
  }

  public void setStatus(long fee, Result.code code) {
    long oldValue = transactionResult.getFee();
    this.transactionResult = this.transactionResult.toBuilder()
        .setFee(oldValue + fee)
        .setRet(code).build();
  }

  public long getFee() {
    return transactionResult.getFee();
  }

  public void setFee(long fee) {
    this.transactionResult = this.transactionResult.toBuilder().setFee(fee).build();
  }

  public long getUnfreezeAmount() {
    return transactionResult.getUnfreezeAmount();
  }

  public void setUnfreezeAmount(long amount) {
    this.transactionResult = this.transactionResult.toBuilder().setUnfreezeAmount(amount).build();
  }

  public String getAssetIssueID() {
    return transactionResult.getAssetIssueID();
  }

  public void setAssetIssueID(String id) {
    this.transactionResult = this.transactionResult.toBuilder().setAssetIssueID(id).build();
  }

  public long getWithdrawAmount() {
    return transactionResult.getWithdrawAmount();
  }

  public void setWithdrawAmount(long amount) {
    this.transactionResult = this.transactionResult.toBuilder().setWithdrawAmount(amount).build();
  }

  public long getExchangeReceivedAmount() {
    return transactionResult.getExchangeReceivedAmount();
  }

  public void setExchangeReceivedAmount(long amount) {
    this.transactionResult = this.transactionResult.toBuilder().setExchangeReceivedAmount(amount)
        .build();
  }

  public long getExchangeWithdrawAnotherAmount() {
    return transactionResult.getExchangeWithdrawAnotherAmount();
  }

  public void setExchangeWithdrawAnotherAmount(long amount) {
    this.transactionResult = this.transactionResult.toBuilder()
        .setExchangeWithdrawAnotherAmount(amount)
        .build();
  }

  public long getExchangeId() {
    return transactionResult.getExchangeId();
  }

  public void setExchangeId(long id) {
    this.transactionResult = this.transactionResult.toBuilder()
        .setExchangeId(id)
        .build();
  }

  public long getExchangeInjectAnotherAmount() {
    return transactionResult.getExchangeInjectAnotherAmount();
  }

  public void setExchangeInjectAnotherAmount(long amount) {
    this.transactionResult = this.transactionResult.toBuilder()
        .setExchangeInjectAnotherAmount(amount)
        .build();
  }

  public void addFee(long fee) {
    this.transactionResult = this.transactionResult.toBuilder()
        .setFee(this.transactionResult.getFee() + fee).build();
  }

  public void setErrorCode(Result.code code) {
    this.transactionResult = this.transactionResult.toBuilder().setRet(code).build();
  }

  public long getShieldedTransactionFee() {
    return transactionResult.getShieldedTransactionFee();
  }

  public void setShieldedTransactionFee(long fee) {
    this.transactionResult = this.transactionResult.toBuilder().setShieldedTransactionFee(fee)
        .build();
  }

  public ByteString getOrderId() {
    return transactionResult.getOrderId();
  }

  public void setOrderId(ByteString orderId) {
    this.transactionResult = this.transactionResult.toBuilder().setOrderId(orderId)
        .build();
  }

  public List<MarketOrderDetail> getOrderDetailsList() {
    return transactionResult.getOrderDetailsList();
  }

  public void addOrderDetails(MarketOrderDetail orderDetail) {
    this.transactionResult = this.transactionResult.toBuilder().addOrderDetails(orderDetail)
        .build();
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