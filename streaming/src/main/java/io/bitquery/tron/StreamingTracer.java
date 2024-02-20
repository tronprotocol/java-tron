package io.bitquery.tron;

import com.google.protobuf.Message;
import io.bitquery.streaming.StreamingProcessor;
import io.bitquery.streaming.TracerConfig;
import io.bitquery.streaming.blockchain.BlockMessageDescriptor;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.runtime.vm.DataWord;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.trace.Tracer;
import org.tron.protos.Protocol.TransactionInfo;

import java.util.List;
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
        this.processor = new StreamingProcessor(config, config.getKafkaTopicBlocks());
    }

    @Override
    public void close() {
        processor.close();
    }

    @Override
    public void blockStart(Object block) {
        try {
            this.currentBlock.set(new BlockMessageBuilder());
            currentBlock.get().buildBlockStartMessage((BlockCapsule) block);
        } catch (Exception e) {
            logger.error("blockStart failed", e);
        }
    }

    // TODO: may be we need to throw ex from here to stop node
    @Override
    public void blockEnd(Object block) {
        try {
            currentBlock.get().buildBlockEndMessage();

            BlockCapsule blockCap = (BlockCapsule) block;

            BlockMessageDescriptor descriptor = new BlockMessageDescriptor();
            descriptor.setBlockHash(blockCap.getBlockId().toString());
            descriptor.setBlockNumber(blockCap.getNum());
            descriptor.setParentHash(blockCap.getParentHash().toString());
            descriptor.setParentNumber(blockCap.getParentBlockId().getNum());
            descriptor.setChainId(config.getChainId());

            BlockMessageValidator validator = new BlockMessageValidator(currentBlock.get().getMessage());
            validator.validate();

            processor.process(descriptor, currentBlock.get().getMessage().toByteArray());

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
    public void transactionEnd(Message protobufResultMessage) {
        try {
            TransactionInfo txInfo = TransactionInfo.parseFrom(protobufResultMessage.toByteArray());
            currentTransaction.get().buildTxEndMessage(txInfo);

            if (!currentTransaction.get().getMessage().getResult().getStatus().equals("SUCESS")) {
                currentTrace.get().cleanLogFromCaptureState();
            }

            currentTransaction.get().addTrace(currentTrace.get().getMessage());

            currentBlock.get().addTransaction(currentTransaction.get().getMessage());
        } catch (Exception e) {
            logger.error("transactionEnd failed", e);
        }
    }

    @Override
    public void captureStart(byte[] from, byte[] to, boolean create, byte[] input, byte[] code, long gas, byte[] value) {
        try {
            currentTrace.get().captureStart(from, to, create, input, code, gas, value);
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
    public void captureEnter(byte[] from, byte[] to, byte[] data, long gas, byte[] value, int opCode, byte[] code) {
        try {
            currentTrace.get().captureEnter(from, to, data, gas, value, opCode, code);
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
}
