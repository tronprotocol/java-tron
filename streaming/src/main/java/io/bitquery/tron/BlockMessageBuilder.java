package io.bitquery.tron;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import evm_messages.BlockMessageOuterClass;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.core.actuator.TransactionFactory;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.protos.streaming.TronMessage.CancelUnfreezeV2Amount;
import org.tron.protos.streaming.TronMessage.Staking;
import org.tron.protos.streaming.TronMessage.Argument;
import org.tron.protos.streaming.TronMessage.CallValue;
import org.tron.protos.streaming.TronMessage.InternalTransaction;
import org.tron.protos.streaming.TronMessage.Contract;
import org.tron.protos.streaming.TronMessage.Log;
import org.tron.protos.streaming.TronMessage.Receipt;
import org.tron.protos.streaming.TronMessage.TransactionHeader;
import org.tron.protos.streaming.TronMessage.TransactionResult;
import org.tron.protos.streaming.TronMessage.Transaction;
import org.tron.protos.streaming.TronMessage.Witness;
import org.tron.protos.streaming.TronMessage.BlockHeader;
import org.tron.protos.streaming.TronMessage.BlockMessage;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j(topic = "streaming")
public class BlockMessageBuilder {

    private BlockCapsule block;

    private BlockMessage.Builder messageBuilder;

    public BlockMessageBuilder() {
        this.messageBuilder = BlockMessage.newBuilder();
    }

    public void buildBlockStartMessage(BlockCapsule block) {
        setBlockHeader(block);
        setBlockWitness(block);
        setBlockStartTransactions(block);
    }

    public void buildBlockEndMessage(BlockCapsule block) {
        this.block = block;

        setBlockEndTransactions();
    }

   public BlockMessage getMessage() {
       return messageBuilder.build();
   }

    private void setBlockHeader() {
        BlockHeader header = BlockHeader.newBuilder()
                .setNumber(block.getNum())
                .setHash(block.getBlockId().getByteString())
                .setTimestamp(block.getTimeStamp())
                .setParentHash(block.getParentBlockId().getByteString())
                .setVersion(block.getInstance().getBlockHeader().getRawData().getVersion())
                .setTxTrieRoot(block.getInstance().getBlockHeader().getRawData().getTxTrieRoot())
                .setAccountStateRoot(block.getInstance().getBlockHeader().getRawData().getAccountStateRoot())
                .build();

        this.messageBuilder.setHeader(header).build();
    }

    private void setBlockWitness() {
        Witness witness = Witness.newBuilder()
                .setAddress(block.getWitnessAddress())
                .setId(block.getInstance().getBlockHeader().getRawData().getWitnessId())
                .setSignature(block.getInstance().getBlockHeader().getWitnessSignature())
                .build();

        this.messageBuilder.setWitness(witness).build();
    }

    private void setBlockStartTransactions() {
        List<TransactionCapsule> txsCap = block.getTransactions();
        int index = 0;

        for (TransactionCapsule txCap : txsCap) {
            TransactionHeader header = getBlockStartTxHeader(txCap, index);
            List<Contract> contracts = getBlockStartTxContract(txCap);

            Transaction tx = Transaction.newBuilder()
                    .setHeader(header)
                    .addAllContracts(contracts)
                    .build();

            this.messageBuilder.addTransactions(tx).build();

            index++;
        }
    }

    private TransactionHeader getBlockStartTxHeader(TransactionCapsule txCap, int index) {
        TransactionHeader header = TransactionHeader.newBuilder()
                .setIndex(index)
                .setExpiration(txCap.getExpiration())
                .setData(ByteString.copyFrom(txCap.getData()))
                .setFeeLimit(txCap.getFeeLimit())
                .setTimestamp(txCap.getTimestamp())
                .addAllSignatures(txCap.getInstance().getSignatureList())
                .build();

        return header;
    }

    private List<Contract> getBlockStartTxContract(TransactionCapsule txCap) {
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

    private void setBlockEndTransactions() {
        List<TransactionInfo> txsInfo = block.getResult().getInstance().getTransactioninfoList();
        int index = 0;

        for (TransactionInfo txInfo : txsInfo) {
            TransactionHeader mergedTxHeader = getBlockEndTxHeader(txInfo, index);
            Contract mergedTxContract = getBlockEndTxContract(txInfo, index);

            TransactionResult result = getTransactionResult(txInfo);
            Receipt receipt = getTransactionReceipt(txInfo);
            List<Log> logs = getLogs(txInfo);
            List<InternalTransaction> internalTransactions = getInternalTransactions(txInfo);
            Staking staking = getStaking(txInfo);

            Transaction mergedTx = messageBuilder.getTransactions(index).toBuilder()
                    .setHeader(mergedTxHeader)
                    .setContracts(0, mergedTxContract)
                    .setResult(result)
                    .setReceipt(receipt)
                    .addAllLogs(logs)
                    .addAllInternalTransactions(internalTransactions)
                    .setStaking(staking)
                    .build();

            this.messageBuilder.setTransactions(index, mergedTx);

            index++;
        }
    }

    private TransactionHeader getBlockEndTxHeader(TransactionInfo txInfo, int txIndex) {
        TransactionHeader mergedTxHeader = messageBuilder.getTransactions(txIndex).getHeader().toBuilder()
                .setId(txInfo.getId())
                .setFee(txInfo.getFee())
                .build();

        return mergedTxHeader;
    }

    private Contract getBlockEndTxContract(TransactionInfo txInfo, int txIndex) {
        Contract mergedTxContract = messageBuilder.getTransactions(txIndex).getContracts(0).toBuilder()
                .setAddress(txInfo.getContractAddress())
                .addAllExecutionResults(txInfo.getContractResultList())
                .build();

        return mergedTxContract;
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

    public void appendTransaction(Transaction tx) {

    }
}
