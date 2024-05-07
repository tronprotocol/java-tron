package io.bitquery.tron;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import io.bitquery.protos.EvmMessage;
import io.bitquery.protos.EvmMessage.Trace;
import io.bitquery.protos.TronMessage.RewardWithdraw;
import org.tron.common.utils.ByteArray;
import org.tron.core.actuator.TransactionFactory;
import org.tron.core.capsule.TransactionCapsule;
import io.bitquery.protos.TronMessage.Argument;
import io.bitquery.protos.TronMessage.CallValue;
import io.bitquery.protos.TronMessage.InternalTransaction;
import io.bitquery.protos.TronMessage.Contract;
import io.bitquery.protos.TronMessage.Receipt;
import io.bitquery.protos.TronMessage.TransactionHeader;
import io.bitquery.protos.TronMessage.TransactionResult;
import io.bitquery.protos.TronMessage.Transaction;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TransactionMessageBuilder {

     private Transaction.Builder messageBuilder;

     public TransactionMessageBuilder() {
          this.messageBuilder = Transaction.newBuilder();
     }

     public void buildTxStartMessage(TransactionCapsule tx, int txIndex) {
          TransactionHeader header = getTxStartTxHeader(tx, txIndex);
          List<Contract> contracts = getTxStartTxContract(tx);

          this.messageBuilder.setHeader(header).addAllContracts(contracts).build();
     }

     public void buildTxEndMessage(TransactionInfo txInfo, List<EvmMessage.Log> log) {
          TransactionHeader mergedTxHeader = getTxEndTxHeader(txInfo);
          Contract mergedTxContract = getTxEndTxContract(txInfo, log);

          TransactionResult result = getTransactionResult(txInfo);
          Receipt receipt = getTransactionReceipt(txInfo);

          this.messageBuilder
                  .setHeader(mergedTxHeader)
                  .setContracts(0, mergedTxContract)
                  .setResult(result)
                  .setReceipt(receipt)
                  .build();
     }

     public void setBroadcastedTime(long time) {
          this.messageBuilder.getHeaderBuilder().setTime(time);
     }

     public Transaction getMessage() {
          return messageBuilder.build();
     }

     public void addTrace(Trace trace) {
          Contract newContract = this.messageBuilder.getContracts(0).toBuilder().setTrace(trace).build();
          this.messageBuilder.setContracts(0, newContract);
     }

    public void addRemovedFlagToLogs() {
         List<EvmMessage.Log.Builder> logs = messageBuilder.getContractsBuilder(0).getLogsBuilderList();

         for (EvmMessage.Log.Builder log : logs) {
              log.getLogHeaderBuilder().setRemoved(true);
         }
    }

     private TransactionHeader getTxEndTxHeader(TransactionInfo txInfo) {
          TransactionHeader mergedTxHeader = messageBuilder.getHeader().toBuilder()
                  .setHash(txInfo.getId())
                  .setFee(txInfo.getFee())
                  .build();

          return mergedTxHeader;
     }

     private Contract getTxEndTxContract(TransactionInfo txInfo, List<EvmMessage.Log> log) {
          List<InternalTransaction> internalTransactions = getInternalTransactions(txInfo);
          RewardWithdraw rw = getRewardWithdraw(txInfo);

          ByteString address = ByteString.copyFrom(
                  io.bitquery.streaming.common.utils.ByteArray.addressWithout41(txInfo.getContractAddress().toByteArray())
          );

          Contract mergedTxContract = messageBuilder.getContracts(0).toBuilder()
                  .setAddress(address)
                  .addAllExecutionResults(txInfo.getContractResultList())
                  .addAllInternalTransactions(internalTransactions)
                  .addAllLogs(log)
                  .setRewardWithdraw(rw)
                  .build();

          return mergedTxContract;
     }

     private TransactionHeader getTxStartTxHeader(TransactionCapsule txCap, int index) {
          byte[] feePayer = io.bitquery.streaming.common.utils.ByteArray.addressWithout41(txCap.getOwnerAddress());

          // convert milliseconds to nanoseconds
          long timestamp = txCap.getTimestamp() * 1000000;
          long expiration = txCap.getExpiration() * 1000000;

          TransactionHeader header = TransactionHeader.newBuilder()
                  .setIndex(index)
                  .setExpiration(expiration)
                  .setData(ByteString.copyFrom(txCap.getData()))
                  .setFeeLimit(txCap.getFeeLimit())
                  .setTimestamp(timestamp)
                  .addAllSignatures(txCap.getInstance().getSignatureList())
                  .setFeePayer(ByteString.copyFrom(feePayer))
                  .build();

          return header;
     }

     private List<Contract> getTxStartTxContract(TransactionCapsule txCap) {
          List<Contract> contracts = new ArrayList<>();

          Protocol.Transaction.Contract txContract = txCap.getInstance().getRawData().getContract(0);

          String type = txContract.getType().name();
          String typeUrl = txContract.getParameter().getTypeUrl();
          List<Argument> arguments = getArguments(txContract);

          Contract contract = Contract.newBuilder()
                  .setType(type)
                  .setTypeUrl(typeUrl)
                  .addAllArguments(arguments)
                  .build();

          contracts.add(contract);

          return contracts;
     }

     private List<Argument> getArguments(Protocol.Transaction.Contract txContract) {
          List<Argument> arguments = new ArrayList();

          Class clazz = TransactionFactory.getContract(txContract.getType());
          Any contractParameter = txContract.getParameter();

          Message contractArguments = null;
          try {
               contractArguments = contractParameter.unpack(clazz);
          } catch (IOException e) {
               e.printStackTrace();
          }

          Map<Descriptors.FieldDescriptor, Object> fields = contractArguments.getAllFields();

          for (Map.Entry<Descriptors.FieldDescriptor, Object> field : fields.entrySet()){
               String keyName = field.getKey().getName();
               Object value = field.getValue();

               Argument.Builder argument = Argument.newBuilder().setName(keyName);

               if(value instanceof ByteString){
                    String decodedValue = ByteArray.toHexString(((ByteString) value).toByteArray());
                    decodedValue = io.bitquery.streaming.common.utils.ByteArray.stringAddressWithout41(decodedValue);

                    argument.setString(decodedValue);
               } else if (value instanceof Long) {
                    argument.setUInt((Long) value);
               }

               arguments.add(argument.build());
          }

          return arguments;
     }

     private TransactionResult getTransactionResult(TransactionInfo txInfo) {
          boolean success = txInfo.getResult().toString() == "SUCESS";

          TransactionResult result = TransactionResult.newBuilder()
                  .setStatus(txInfo.getResult().toString())
                  .setSuccess(success)
                  .setMessage(txInfo.getResMessage().toStringUtf8())
                  .build();

          return result;
     }
     private Receipt getTransactionReceipt(TransactionInfo txInfo) {
          Receipt receipt = Receipt.newBuilder()
                  .setResult(txInfo.getReceipt().getResult().toString())
                  .setEnergyPenaltyTotal(txInfo.getReceipt().getEnergyPenaltyTotal())
                  .setEnergyFee(txInfo.getReceipt().getEnergyFee())
                  .setEnergyUsageTotal(txInfo.getReceipt().getEnergyUsageTotal())
                  .setOriginEnergyUsage(txInfo.getReceipt().getOriginEnergyUsage())
                  .setNetUsage(txInfo.getReceipt().getNetUsage())
                  .setNetFee(txInfo.getReceipt().getNetFee())
                  .build();

          return receipt;
     }

     private List<InternalTransaction> getInternalTransactions(TransactionInfo txInfo) {
          List<InternalTransaction> internalTransactions = new ArrayList();

          int index = 0;
          for (Protocol.InternalTransaction txInternalTx : txInfo.getInternalTransactionsList()) {
               List<CallValue> callValues = getCallValues(txInternalTx);

               ByteString callerAddress = ByteString.copyFrom(
                       io.bitquery.streaming.common.utils.ByteArray.addressWithout41(txInternalTx.getCallerAddress().toByteArray())
               );

               ByteString transferToAddress = ByteString.copyFrom(
                       io.bitquery.streaming.common.utils.ByteArray.addressWithout41(txInternalTx.getTransferToAddress().toByteArray())
               );

               InternalTransaction internalTx = InternalTransaction.newBuilder()
                       .setCallerAddress(callerAddress)
                       .setNote(txInternalTx.getNote().toStringUtf8())
                       .setTransferToAddress(transferToAddress)
                       .addAllCallValues(callValues)
                       .setHash(txInternalTx.getHash())
                       .setIndex(index)
                       .build();

               internalTransactions.add(internalTx);

               index++;
          }

          return internalTransactions;
     }

     private List<CallValue> getCallValues(Protocol.InternalTransaction internalTx) {
          List<CallValue> callValues = new ArrayList();

          for (Protocol.InternalTransaction.CallValueInfo callValueInfo : internalTx.getCallValueInfoList()){
               CallValue callValue = CallValue.newBuilder()
                       .setCallValue(callValueInfo.getCallValue())
                       .setTokenId(callValueInfo.getTokenId())
                       .build();

               callValues.add(callValue);
          }

          return callValues;
     }

     private RewardWithdraw getRewardWithdraw(TransactionInfo txInfo) {
          RewardWithdraw.Builder rw = RewardWithdraw.newBuilder();

          long amount = txInfo.getWithdrawAmount();
          Contract contract = messageBuilder.getContracts(0);

          if (amount <= 0 && contract.getType() != "WithdrawBalanceContract") {
               return rw.build();
          }

          ByteString receiver = ByteString.copyFrom(ByteArray.fromHexString(contract.getArguments(0).getString()));

          rw.setAmount(amount).setReceiver(receiver);

          return rw.build();
     }
}
