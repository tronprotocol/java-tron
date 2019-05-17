package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.tron.common.runtime.vm.LogInfo;
import org.tron.common.runtime.vm.program.InternalTransaction;
import org.tron.common.runtime.vm.program.ProgramResult;
import org.tron.core.config.args.Args;
import org.tron.core.db.TransactionTrace;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionResult.Log;
import org.tron.protos.Protocol.TransactionResult.code;
import org.tron.protos.Protocol.TransactionResult;
import org.tron.protos.Protocol.TransactionResultList;

@Slf4j(topic = "capsule")
public class TransactionResultListCapsule implements ProtoCapsule<TransactionResultList> {
  private TransactionResultList transactionResultList;

  public TransactionResultListCapsule(BlockCapsule blockCapsule) {
    transactionResultList = TransactionResultList.newBuilder().build();
    if (Objects.isNull(blockCapsule)) {
      return;
    }
    TransactionResultList.Builder build = transactionResultList.toBuilder().
        setBlockNumber(blockCapsule.getNum()).setBlockTimeStamp(blockCapsule.getTimeStamp());
    transactionResultList = build.build();
  }

  public void addTransactionResult(TransactionResult result) {
    this.transactionResultList = this.transactionResultList.toBuilder().addTransactionresult(result).build();
  }

  public static TransactionResult buildInstance(TransactionCapsule trxCap, BlockCapsule block,
      TransactionTrace trace) {

    TransactionResult.Builder builder = TransactionResult.newBuilder();
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
        programResult.getRet().getFee() + traceReceipt.getEnergyFee()
            + traceReceipt.getNetFee() + traceReceipt.getMultiSignFee();
    ByteString contractResult = ByteString.copyFrom(programResult.getHReturn());
    ByteString ContractAddress = ByteString.copyFrom(programResult.getContractAddress());

    builder.setFee(fee);
    builder.addContractResult(contractResult);
    builder.setContractAddress(ContractAddress);
    builder.setUnfreezeAmount(programResult.getRet().getUnfreezeAmount());
    builder.setAssetIssueID(programResult.getRet().getAssetIssueID());
    builder.setExchangeId(programResult.getRet().getExchangeId());
    builder.setWithdrawAmount(programResult.getRet().getWithdrawAmount());
    builder.setExchangeReceivedAmount(programResult.getRet().getExchangeReceivedAmount());
    builder.setExchangeInjectAnotherAmount(programResult.getRet().getExchangeInjectAnotherAmount());
    builder.setExchangeWithdrawAnotherAmount(
        programResult.getRet().getExchangeWithdrawAnotherAmount());

    List<Log> logList = new ArrayList<>();
    programResult.getLogInfoList().forEach(
        logInfo -> {
          logList.add(LogInfo.buildLog(logInfo));
        }
    );
    builder.addAllLog(logList);
    builder.setReceipt(traceReceipt.getReceipt());

    if (Args.getInstance().isSaveInternalTx() && null != programResult.getInternalTransactions()) {
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
        //TODO: "for loop" below in future for multiple token case, we only have one for now.
        Protocol.InternalTransaction.CallValueInfo.Builder callValueInfoBuilder =
            Protocol.InternalTransaction.CallValueInfo.newBuilder();
        // trx will not be set token name
        callValueInfoBuilder.setCallValue(internalTransaction.getValue());
        // Just one transferBuilder for now.
        internalTrxBuilder.addCallValueInfo(callValueInfoBuilder);
        internalTransaction.getTokenInfo().forEach((tokenId, amount) -> {
          Protocol.InternalTransaction.CallValueInfo.Builder tokenInfoBuilder =
              Protocol.InternalTransaction.CallValueInfo.newBuilder();
          tokenInfoBuilder.setTokenId(tokenId);
          tokenInfoBuilder.setCallValue(amount);
          internalTrxBuilder.addCallValueInfo(tokenInfoBuilder);
        });
        // Token for loop end here
        internalTrxBuilder.setNote(ByteString.copyFrom(internalTransaction.getNote().getBytes()));
        internalTrxBuilder.setRejected(internalTransaction.isRejected());
        builder.addInternalTransactions(internalTrxBuilder);
      }
    }

    return builder.build();
  }

  @Override
  public byte[] getData() {
    return transactionResultList.toByteArray();
  }

  @Override
  public TransactionResultList getInstance() {
    return transactionResultList;
  }
}