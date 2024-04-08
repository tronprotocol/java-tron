package io.bitquery.tron;

import com.google.protobuf.Message;
import io.bitquery.protos.TronMessage;
import io.bitquery.streaming.StreamingProcessor;
import io.bitquery.streaming.TracerConfig;
import io.bitquery.streaming.blockchain.BlockMessageDescriptor;
import io.bitquery.streaming.blockchain.BroadcastedMessageDescriptor;
import io.bitquery.streaming.common.utils.ByteArray;
import io.bitquery.streaming.messages.Descriptor;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.runtime.vm.DataWord;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.trace.Tracer;
import org.tron.protos.Protocol.TransactionInfo;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

@Slf4j(topic = "tracer")
public class StreamingTracer implements Tracer {
    private TracerConfig config;

    private StreamingProcessor processor;

    private ThreadLocal<BlockMessageBuilder> currentBlock = new ThreadLocal<>();

    private ThreadLocal<TransactionMessageBuilder> currentTransaction = new ThreadLocal<>();

    private ThreadLocal<EvmMessageBuilder> currentTrace = new ThreadLocal<>();

    @Override
    public void init(String configFile) throws Exception {
        this.config = new TracerConfig(configFile);
        this.processor = new StreamingProcessor(config);
    }

    @Override
    public void close() {
        processor.close();
    }

    @Override
    public void blockStart(Object block) {
        try {
            this.currentBlock.set(new BlockMessageBuilder());
            currentBlock.get().buildBlockStartMessage((BlockCapsule) block, config.getChainId());
        } catch (Exception e) {
            logger.error("blockStart failed", e);
        }
    }

    // TODO: may be we need to throw ex from here to stop node
    @Override
    public void blockEnd() {
        try {
            currentBlock.get().buildBlockEndMessage();

            Descriptor descriptor = getDescriptor("blocks");

            processor.process(descriptor, currentBlock.get().getMessage().toByteArray(), config.getKafkaTopicBlocks());

            this.currentBlock.remove();
            this.currentTransaction.remove();
            this.currentTrace.remove();
        } catch (Exception e) {
            logger.error("blockEnd failed", e);
        }
    }

    @Override
    public void transactionStart(Object tx) {
        try {
            this.currentTransaction.set(new TransactionMessageBuilder());
            this.currentTrace.set(new EvmMessageBuilder());

            int txIndex = currentBlock.get().getMessage().getTransactionsCount();
            currentTransaction.get().buildTxStartMessage((TransactionCapsule) tx, txIndex);
        } catch (Exception e) {
            logger.error("transactionStart failed", e);
        }
    }

    @Override
    public void transactionEnd(Message protobufResultMessage, boolean isPending) {
        try {
            TransactionInfo txInfo = TransactionInfo.parseFrom(protobufResultMessage.toByteArray());
            EvmMessageBuilder trace = currentTrace.get();

            currentTransaction.get().buildTxEndMessage(txInfo, trace.getCollectedLogs());

            addRemovedFlagToLogs(txInfo.getLogCount(), trace.getCollectedLogs().size());

            currentTransaction.get().addTrace(trace.getMessage());

            if (isPending) {
                long nanoseconds = Instant.now().toEpochMilli() * 1_000_000L;
                currentTransaction.get().setBroadcastedTime(nanoseconds);
            }

            currentBlock.get().addTransaction(currentTransaction.get().getMessage());

            if (!isPending) {
                return;
            }

            Descriptor descriptor = getDescriptor("broadcasted");

            processor.process(descriptor, currentBlock.get().getMessage().toByteArray(), config.getKafkaTopicBroadcasted());

            this.currentBlock.remove();
            this.currentTransaction.remove();
            this.currentTrace.remove();

        } catch (Exception e) {
            logger.error("transactionEnd failed", e);
        }
    }

    @Override
    public void captureStart(byte[] from, byte[] to, boolean create, byte[] input, byte[] code, long gas, byte[] value, String tokenId) {
        try {
            currentTrace.get().captureStart(from, to, create, input, code, gas, value, tokenId);
        } catch (Exception e) {
            logger.error("captureStart failed", e);
        }
    }

    @Override
    public void captureEnd(long energyUsed, RuntimeException error) {
        try {
            currentTrace.get().captureEnd(energyUsed, error);
        } catch (Exception e) {
            logger.error("captureEnd failed", e);
        }
    }

    @Override
    public void captureFault(int opcodeNum, String opcodeName, long energy, Stack<DataWord> stackData, byte[] callerData, byte[] contractData, byte[] callValueData, int pc, byte[] memory, int callDepth, RuntimeException error) {
        try {
            currentTrace.get().captureFault(opcodeNum, opcodeName, energy, stackData, callerData, contractData, callValueData, pc, memory, callDepth, error);
        } catch (Exception e) {
            logger.error("captureFault failed", e);
        }
    }

    @Override
    public void captureEnter(byte[] from, byte[] to, byte[] data, long gas, byte[] value, int opCode, byte[] code, String tokenId) {
        try {
            currentTrace.get().captureEnter(from, to, data, gas, value, opCode, code, tokenId);
        } catch (Exception e) {
            logger.error("captureEnter failed", e);
        }
    }

    @Override
    public void captureExit(long energyUsed, RuntimeException error) {
        try {
            currentTrace.get().captureExit(energyUsed, error);
        } catch (Exception e) {
            logger.error("captureExit failed", e);
        }
    }

    @Override
    public void captureState(int opcodeNum, String opcodeName, long energy, int pc, int callDepth) {
        try {
            currentTrace.get().captureState(opcodeNum, opcodeName, energy, pc, callDepth);
        } catch (Exception e) {
            logger.error("captureState failed", e);
        }
    }

    @Override
    public void addLogToCaptureState(byte[] address, byte[] data, List<DataWord> topicsData, byte[] code) {
        try {
            currentTrace.get().addLogToCaptureState(address, data, topicsData, code);
        } catch (Exception e) {
            logger.error("addLogToCaptureState failed", e);
        }
    }

    @Override
    public void addStorageToCaptureState(byte[] address, byte[] loc, byte[] value) {
        try {
            currentTrace.get().addStorageToCaptureState(address, loc, value);
        } catch (Exception e) {
            logger.error("addStorageToCaptureState failed", e);
        }
    }

    private void addRemovedFlagToLogs(int nodeLogsCount, int collectedLogsCount) {
        if (nodeLogsCount == 0 && nodeLogsCount != collectedLogsCount) {
            currentTransaction.get().addRemovedFlagToLogs();
        }
    }

    private Descriptor getDescriptor(String type) {
        TronMessage.BlockHeader blockMsgHeader = currentBlock.get().getMessage().getHeader();

        String blockHash = ByteArray.toHexString(blockMsgHeader.getHash().toByteArray());
        long blockNumber = blockMsgHeader.getNumber();
        String parentHash = ByteArray.toHexString(blockMsgHeader.getParentHash().toByteArray());
        long parentNumber = blockMsgHeader.getParentNumber();

        Descriptor descriptor;

        if (Objects.equals(type, "blocks")) {
            descriptor = new BlockMessageDescriptor();
        } else if (Objects.equals(type, "broadcasted")) {
            TronMessage.TransactionHeader txHeader = currentTransaction.get().getMessage().getHeader();

            BroadcastedMessageDescriptor broadcastedDescriptor = new BroadcastedMessageDescriptor();
            List<String> txsList = Collections.singletonList(ByteArray.toHexString(txHeader.getHash().toByteArray()));
            broadcastedDescriptor.setTransactionsList(txsList);
            broadcastedDescriptor.setTimeStart(txHeader.getTime());
            broadcastedDescriptor.setTimeEnd(txHeader.getTime());
            descriptor = broadcastedDescriptor;
        } else {
            logger.error("Invalid descriptor type: {}", type);
            throw new IllegalArgumentException("Invalid descriptor type: " + type);
        }

        descriptor.setBlockHash(blockHash);
        descriptor.setBlockNumber(blockNumber);
        descriptor.setParentHash(parentHash);
        descriptor.setParentNumber(parentNumber);
        descriptor.setChainId(config.getChainId());

        return descriptor;
    }
}
