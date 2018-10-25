package org.tron.core.capsule;

import static org.tron.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.tron.common.runtime.vm.LogInfo;
import org.tron.common.runtime.vm.program.InternalTransaction;
import org.tron.common.runtime.vm.program.ProgramResult;
import org.tron.core.db.TransactionTrace;
import org.tron.core.exception.BadItemException;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionInfo.Log;
import org.tron.protos.Protocol.TransactionInfo.code;

@Slf4j
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

  public long getFee() {
    return transactionInfo.getFee();
  }

  public void setId(byte[] id) {
    this.transactionInfo = this.transactionInfo.toBuilder()
        .setId(ByteString.copyFrom(id)).build();
  }

  public byte[] getId() {
    return transactionInfo.getId().toByteArray();
  }


  public void setUnfreezeAmount(long amount) {
    this.transactionInfo = this.transactionInfo.toBuilder().setUnfreezeAmount(amount).build();
  }

  public long getUnfreezeAmount() {
    return transactionInfo.getUnfreezeAmount();
  }

  public void setWithdrawAmount(long amount) {
    this.transactionInfo = this.transactionInfo.toBuilder().setWithdrawAmount(amount).build();
  }

  public long getWithdrawAmount() {
    return transactionInfo.getWithdrawAmount();
  }

  public void setFee(long fee) {
    this.transactionInfo = this.transactionInfo.toBuilder().setFee(fee).build();
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

  public static TransactionInfoCapsule buildInstance(TransactionCapsule trxCap, BlockCapsule block,
      TransactionTrace trace) {

    TransactionInfo.Builder builder = TransactionInfo.newBuilder();
    ReceiptCapsule traceReceipt = trace.getReceipt();
    builder.setResult(code.SUCESS);
    if (StringUtils.isNoneEmpty(trace.getRuntimeError()) || Objects
        .nonNull(trace.getRuntimeResult().getException())) {
      builder.setResult(code.FAILED);
      builder.setResMessage(ByteString.copyFromUtf8(trace.getRuntimeError()));
    }
    builder.setId(ByteString.copyFrom(trxCap.getTransactionId().getBytes()));
    ProgramResult programResult = trace.getRuntimeResult();
    long fee =
        programResult.getRet().getFee() + traceReceipt.getEnergyFee() + traceReceipt.getNetFee();
    ByteString contractResult = ByteString.copyFrom(programResult.getHReturn());
    ByteString ContractAddress = ByteString.copyFrom(programResult.getContractAddress());

    builder.setFee(fee);
    builder.addContractResult(contractResult);
    builder.setContractAddress(ContractAddress);
    builder.setUnfreezeAmount(programResult.getRet().getUnfreezeAmount());
    builder.setWithdrawAmount(programResult.getRet().getWithdrawAmount());

    List<Log> logList = new ArrayList<>();
    programResult.getLogInfoList().forEach(
        logInfo -> {
          logList.add(LogInfo.buildLog(logInfo));
        }
    );
    builder.addAllLog(logList);

    if (Objects.nonNull(block)) {
      builder.setBlockNumber(block.getInstance().getBlockHeader().getRawData().getNumber());
      builder.setBlockTimeStamp(block.getInstance().getBlockHeader().getRawData().getTimestamp());
    }

    builder.setReceipt(traceReceipt.getReceipt());

    if (null != programResult.getInternalTransactions()) {
      for (InternalTransaction internalTransaction : programResult
          .getInternalTransactions()) {
        Protocol.InternalTransaction.Builder internalTrxBuilder = Protocol.InternalTransaction
            .newBuilder();
        // set hash
        internalTrxBuilder.setHash(ByteString.copyFrom(internalTransaction.getHash()));
        // set caller
        internalTrxBuilder.setCallerAddress(ByteString.copyFrom(internalTransaction.getSender()));
        // set TransferTo
        internalTrxBuilder
            .setTransferToAddress(ByteString.copyFrom(internalTransaction.getTransferToAddress()));
        //TODO: for loop below in future for Tokens if we design involve token in
        Protocol.InternalTransaction.CallValueInfo.Builder callValueInfoBuilder =
            Protocol.InternalTransaction.CallValueInfo.newBuilder();
        callValueInfoBuilder.setCallValue(internalTransaction.getValue());
        // trx will not be set token name
        callValueInfoBuilder.setTokenName(ByteString.copyFrom(EMPTY_BYTE_ARRAY));
        // Just one transferBuilder for now.
        internalTrxBuilder.addCallValueInfo(callValueInfoBuilder);
        // Token for loop end here
        internalTrxBuilder.setNote(ByteString.copyFrom(internalTransaction.getNote().getBytes()));
        internalTrxBuilder.setRejected(internalTransaction.isRejected());
        builder.addInternalTransactions(internalTrxBuilder);
      }
    }

    return new TransactionInfoCapsule(builder.build());
  }
}