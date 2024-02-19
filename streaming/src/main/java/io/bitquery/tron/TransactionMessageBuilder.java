package io.bitquery.tron;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import io.bitquery.protos.EvmMessage.Trace;
import org.tron.common.utils.ByteArray;
import org.tron.core.actuator.TransactionFactory;
import org.tron.core.capsule.TransactionCapsule;
import io.bitquery.protos.TronMessage.CancelUnfreezeV2Amount;
import io.bitquery.protos.TronMessage.Staking;
import io.bitquery.protos.TronMessage.Argument;
import io.bitquery.protos.TronMessage.CallValue;
import io.bitquery.protos.TronMessage.InternalTransaction;
import io.bitquery.protos.TronMessage.Contract;
import io.bitquery.protos.TronMessage.Log;
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

     public void buildTxStartMessage(TransactionCapsule tx) {
          TransactionHeader header = getTxStartTxHeader(tx);
          List<Contract> contracts = getTxStartTxContract(tx);

          this.messageBuilder.setHeader(header).addAllContracts(contracts).build();
     }

     public void buildTxEndMessage(TransactionInfo txInfo) {
          TransactionHeader mergedTxHeader = getBlockEndTxHeader(txInfo);
          Contract mergedTxContract = getBlockEndTxContract(txInfo);

          List<Log> logs = getLogs(txInfo);

          TransactionResult result = getTransactionResult(txInfo);
          Receipt receipt = getTransactionReceipt(txInfo);
          Staking staking = getStaking(txInfo);

          this.messageBuilder
                  .setHeader(mergedTxHeader)
                  .setContracts(0, mergedTxContract)
                  .setResult(result)
                  .setReceipt(receipt)
                  .addAllLogs(logs)
                  .setStaking(staking)
                  .build();
     }

     public Transaction getMessage() {
          return messageBuilder.build();
     }

     public void addTrace(Trace trace) {
          Contract newContract = this.messageBuilder.getContracts(0).toBuilder().setTrace(trace).build();
          this.messageBuilder.setContracts(0, newContract);
     }

     private TransactionHeader getBlockEndTxHeader(TransactionInfo txInfo) {
          TransactionHeader mergedTxHeader = messageBuilder.getHeader().toBuilder()
                  .setId(txInfo.getId())
                  .setFee(txInfo.getFee())
                  .build();

          return mergedTxHeader;
     }

     private Contract getBlockEndTxContract(TransactionInfo txInfo) {
          List<InternalTransaction> internalTransactions = getInternalTransactions(txInfo);

          Contract mergedTxContract = messageBuilder.getContracts(0).toBuilder()
                  .setAddress(txInfo.getContractAddress())
                  .addAllExecutionResults(txInfo.getContractResultList())
                  .addAllInternalTransactions(internalTransactions)
                  .build();

          return mergedTxContract;
     }

     private TransactionHeader getTxStartTxHeader(TransactionCapsule txCap) {
          //TODO: add index
          TransactionHeader header = TransactionHeader.newBuilder()
//                  .setIndex(index)
                  .setExpiration(txCap.getExpiration())
                  .setData(ByteString.copyFrom(txCap.getData()))
                  .setFeeLimit(txCap.getFeeLimit())
                  .setTimestamp(txCap.getTimestamp())
                  .addAllSignatures(txCap.getInstance().getSignatureList())
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
                    argument.setString(decodedValue);
               } else if (value instanceof Long) {
                    argument.setUInt((Long) value);
               }

               arguments.add(argument.build());
          }

          return arguments;
     }

     private TransactionResult getTransactionResult(TransactionInfo txInfo) {
          TransactionResult result = TransactionResult.newBuilder()
                  .setStatus(txInfo.getResult().toString())
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

     private List<Log> getLogs(TransactionInfo txInfo) {
          List<Log> logs = new ArrayList();

          int index = 0;
          for (TransactionInfo.Log txInfoLog : txInfo.getLogList()) {
               Log log = Log.newBuilder()
                       .setAddress(txInfoLog.getAddress())
                       .setData(txInfoLog.getData())
                       .addAllTopics(txInfoLog.getTopicsList())
                       .setIndex(index)
                       .build();

               logs.add(log);

               index++;
          }

          return logs;
     }

     private List<InternalTransaction> getInternalTransactions(TransactionInfo txInfo) {
          List<InternalTransaction> internalTransactions = new ArrayList();

          int index = 0;
          for (Protocol.InternalTransaction txInternalTx : txInfo.getInternalTransactionsList()) {
               List<CallValue> callValues = getCallValues(txInternalTx);

               InternalTransaction internalTx = InternalTransaction.newBuilder()
                       .setCallerAddress(txInternalTx.getCallerAddress())
                       .setNote(txInternalTx.getNote().toStringUtf8())
                       .setTransferToAddress(txInternalTx.getTransferToAddress())
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

     private Staking getStaking(TransactionInfo txInfo) {
          Map<String, Long> cancelUnfreeze = txInfo.getCancelUnfreezeV2AmountMap();

          Staking.Builder staking = Staking.newBuilder();

          staking.setWithdrawAmount(txInfo.getWithdrawAmount())
                  .setUnfreezeAmount(txInfo.getUnfreezeAmount())
                  .setWithdrawExpireAmount(txInfo.getWithdrawExpireAmount());

          for (Map.Entry<String, Long> cu : cancelUnfreeze.entrySet()){
               CancelUnfreezeV2Amount cancelUnfreezeV2Amount = CancelUnfreezeV2Amount.newBuilder()
                       .setKey(cu.getKey())
                       .setValue(cu.getValue())
                       .build();

               staking.addCancelUnfreezeV2Amounts(cancelUnfreezeV2Amount);
          }

          return staking.build();
     }
}
