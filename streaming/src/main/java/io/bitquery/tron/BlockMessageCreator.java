package io.bitquery.tron;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import evm_messages.BlockMessageOuterClass.Trace;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.core.actuator.TransactionFactory;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.EvmTraceCapsuleI;
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
public class BlockMessageCreator {

    private BlockCapsule newBlock; // TODO:

    private BlockMessage.Builder blockMessage;

    @Getter
    private EVMBuilder evmBuilder;

    public BlockMessageCreator(BlockCapsule newBlock) {
        this.newBlock = newBlock;
        this.blockMessage = BlockMessage.newBuilder();
    }

    public void create() {
        logger.info("Creating block protobuf message, Num: {}, ID: {}", this.newBlock.getNum(), this.newBlock.getBlockId());

        setBlock();
        setTransactions();
    }

    public BlockMessage getBlockMessage() {
        return this.blockMessage.build();
    }

    private void setBlock() {
        BlockHeader header = setBlockHeader();
        Witness witness = setBlockWitness();

        this.blockMessage.setHeader(header).setWitness(witness).build();
    }

    private BlockHeader setBlockHeader() {
        BlockHeader header = BlockHeader.newBuilder()
                .setNumber(newBlock.getNum())
                .setHash(newBlock.getBlockId().getByteString())
                .setTimestamp(newBlock.getTimeStamp())
                .setParentHash(newBlock.getParentBlockId().getByteString())
                .setVersion(newBlock.getInstance().getBlockHeader().getRawData().getVersion())
                .setTxTrieRoot(newBlock.getInstance().getBlockHeader().getRawData().getTxTrieRoot())
                .setAccountStateRoot(newBlock.getInstance().getBlockHeader().getRawData().getAccountStateRoot())
                .build();

        return header;
    }

    private Witness setBlockWitness() {
        Witness witness = Witness.newBuilder()
                .setAddress(newBlock.getWitnessAddress())
                .setId(newBlock.getInstance().getBlockHeader().getRawData().getWitnessId())
                .setSignature(newBlock.getInstance().getBlockHeader().getWitnessSignature())
                .build();

        return witness;
    }

    private void setTransactions() {
        List<TransactionInfo> txsInfo = newBlock.getResult().getInstance().getTransactioninfoList();

        int index = 0;
        for (TransactionInfo txInfo : txsInfo) {
            TransactionCapsule txCap = newBlock.getTransactions().get(index);
            EvmTraceCapsuleI evmTraceCapsule = txCap.getTrxTrace().getTransactionContext().getEvmTraceCapsule();

            TransactionHeader header = getTransactionHeader(txInfo, txCap, index);
            TransactionResult result = getTransactionResult(txInfo);
            Receipt receipt = getTransactionReceipt(txInfo);
            List<Log> logs = getLogs(txInfo);
            List<Contract> contracts = getContracts(txInfo, txCap);
            List<InternalTransaction> internalTransactions = getInternalTransactions(txInfo);
            Staking staking = getStaking(txInfo);

            Transaction tx = Transaction.newBuilder()
                    .setHeader(header)
                    .setResult(result)
                    .setReceipt(receipt)
                    .addAllLogs(logs)
                    .addAllContracts(contracts)
                    .addAllInternalTransactions(internalTransactions)
                    .setStaking(staking)
                    .build();

            if (evmTraceCapsule != null) {
                tx = tx.toBuilder().setTrace(evmTraceCapsule.getInstance()).build();
            }

            this.blockMessage.addTransactions(tx).build();

            index++;
        }
    }

    private TransactionHeader getTransactionHeader(TransactionInfo txInfo, TransactionCapsule txCap, int index) {
        TransactionHeader header = TransactionHeader.newBuilder()
                .setId(txInfo.getId())
                .setFee(txInfo.getFee())
                .setIndex(index)
                .setExpiration(txCap.getExpiration())
                .setData(ByteString.copyFrom(txCap.getData()))
                .setFeeLimit(txCap.getFeeLimit())
                .setTimestamp(txCap.getTimestamp())
                .addAllSignatures(txCap.getInstance().getSignatureList())
                .build();

        return header;
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

    private List<Contract> getContracts(TransactionInfo txInfo, TransactionCapsule txCap) {
        List<Contract> contracts = new ArrayList();

        Protocol.Transaction.Contract txContract = txCap.getInstance().getRawData().getContract(0);

        ByteString address = txInfo.getContractAddress();
        String type = txContract.getType().name();
        String typeUrl = txContract.getParameter().getTypeUrl();
        List<Argument> arguments = getArguments(txContract);

        Contract contract = Contract.newBuilder()
                .setAddress(address)
                .addAllExecutionResults(txInfo.getContractResultList())
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

    private List<InternalTransaction> getInternalTransactions(TransactionInfo txInfo) {
        List<InternalTransaction> internalTransactions = new ArrayList();

        int index = 0;
        for (Protocol.InternalTransaction txInternalTx : txInfo.getInternalTransactionsList()) {
            List<CallValue> callValues = getCallvalues(txInternalTx);

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

    private List<CallValue> getCallvalues(Protocol.InternalTransaction internalTx) {
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
