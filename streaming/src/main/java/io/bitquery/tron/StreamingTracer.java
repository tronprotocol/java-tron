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

    private BlockMessageBuilder currentBlock;

    private TransactionMessageBuilder currentTransaction;

    private EvmMessageBuilder currentTrace;

    @Override
    public void init(String tracerConfigPath) throws Exception {
        config = new TracerConfig(tracerConfigPath);
        this.processor = new StreamingProcessor(config);
    }

    @Override
    public void close() {
        processor.close();
    }

    @Override
    public void blockStart(Object block) {
        try {
            this.currentBlock = new BlockMessageBuilder();
            currentBlock.buildBlockStartMessage((BlockCapsule) block);
        } catch (Exception e) {
            logger.error("blockStart failed, error: {}", e.getMessage());
        }
    }

    // TODO: may be we need to throw ex from here to stop node
    @Override
    public void blockEnd(Object block) {
        try {
            BlockCapsule blockCap = (BlockCapsule) block;

            BlockMessageDescriptor descriptor = new BlockMessageDescriptor();
            descriptor.setBlockHash(blockCap.getBlockId().toString());
            descriptor.setBlockNumber(blockCap.getNum());
            descriptor.setParentHash(blockCap.getParentHash().toString());
            descriptor.setParentNumber(blockCap.getParentBlockId().getNum());
            descriptor.setChainId(config.getChainId());

            processor.process(descriptor, currentBlock.getMessage().toByteArray());

        } catch (Exception e) {
            logger.error("blockEnd failed, error: {}", e.getMessage());
        }
    }

    @Override
    public void transactionStart(Object tx) {
        try {
            this.currentTransaction = new TransactionMessageBuilder();
            this.currentTrace = new EvmMessageBuilder();

            currentTransaction.buildTxStartMessage((TransactionCapsule) tx);
        } catch (Exception e) {
            logger.error("transactionStart failed, error: {}", e.getMessage());
        }
    }

    @Override
    public void transactionEnd(Message protobufResultMessage) {
        try {
            TransactionInfo txInfo = TransactionInfo.parseFrom(protobufResultMessage.toByteArray());
            currentTransaction.buildTxEndMessage(txInfo);
            currentTransaction.addtrace(currentTrace.getMessage());

            currentBlock.addTransaction(currentTransaction.getMessage());
        } catch (Exception e) {
            logger.error("transactionEnd failed, error: {}", e.getMessage());
        }
    }

    @Override
    public void captureStart(byte[] from, byte[] to, byte[] code, long gas) {
        try {
            currentTrace.captureStart(from, to, code, gas);
        } catch (Exception e) {
            logger.error("captureStart failed, error: {}", e.getMessage());
        }
    }

    @Override
    public void captureEnd(long energyUsed, RuntimeException error) {
        try {
            currentTrace.captureEnd(energyUsed, error);
        } catch (Exception e) {
            logger.error("captureEnd failed, error: {}", e.getMessage());
        }
    }

    @Override
    public void captureFault(int opcodeNum, String opcodeName, long energy, Stack<DataWord> stackData, byte[] callerData, byte[] contractData, byte[] callValueData, int pc, byte[] memory, int callDepth, RuntimeException error) {
        try {
            currentTrace.captureFault(opcodeNum, opcodeName, energy, stackData, callerData, contractData, callValueData, pc, memory, callDepth, error);
        } catch (Exception e) {
            logger.error("captureFault failed, error: {}", e.getMessage());
        }
    }

    @Override
    public void captureEnter(byte[] from, byte[] to, byte[] data, long gas, byte[] value, int opCode, byte[] code) {
        try {
            currentTrace.captureEnter(from, to, data, gas, value, opCode, code);
        } catch (Exception e) {
            logger.error("captureEnter failed, error: {}", e.getMessage());
        }
    }

    @Override
    public void captureExit(long energyUsed, RuntimeException error) {
        try {
            currentTrace.captureExit(energyUsed, error);
        } catch (Exception e) {
            logger.error("captureExit failed, error: {}", e.getMessage());
        }
    }

    @Override
    public void captureState(int opcodeNum, String opcodeName, long energy, int pc, int callDepth) {
        try {
            currentTrace.captureState(opcodeNum, opcodeName, energy, pc, callDepth);
        } catch (Exception e) {
            logger.error("captureState failed, error: {}", e.getMessage());
        }
    }

    @Override
    public void addLogToCaptureState(byte[] address, byte[] data, List<DataWord> topicsData, byte[] code) {
        try {
            currentTrace.addLogToCaptureState(address, data, topicsData, code);
        } catch (Exception e) {
            logger.error("addLogToCaptureState failed, error: {}", e.getMessage());
        }
    }

    @Override
    public void addStorageToCaptureState(byte[] address, byte[] loc, byte[] value) {
        try {
            currentTrace.addStorageToCaptureState(address, loc, value);
        } catch (Exception e) {
            logger.error("addStorageToCaptureState failed, error: {}", e.getMessage());
        }
    }
}
