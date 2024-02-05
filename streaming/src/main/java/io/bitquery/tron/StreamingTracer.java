package io.bitquery.tron;

import io.bitquery.streaming.StreamingProcessor;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.runtime.vm.DataWord;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.trace.Tracer;

import java.util.List;
import java.util.Stack;

@Slf4j(topic = "tracer")
public class StreamingTracer implements Tracer {
    private StreamingProcessor processor;
    private BlockMessageCreator currentBlock;

    @Override
    public void init(String implementationConfigFile) throws Exception {
        this.processor = new StreamingProcessor(implementationConfigFile);
    }

    @Override
    public void close() {
        if (this.processor != null){
            this.processor.close();
        }

        this.processor = null;
    }

    @Override
    public void blockStart(Object block) {
        try {
            this.currentBlock = new BlockMessageCreator((BlockCapsule) block);
        } catch (Exception ex) {
            logger.warn("blockStart failed, error: {}", ex.getMessage());
        }
    }

    @Override
    public void blockEnd(Object block) {
        try {
            if (processor != null){
                currentBlock.blockEnd(block);
                processor.process(this.getBlockMessage());
            }
        } catch (Exception e) { // TODO: may be we need to throw ex from here to stop node
            logger.warn("blockEnd failed, error: {}", e.getMessage());
        }
    }

    @Override
    public void captureStart(byte[] from, byte[] to, byte[] code, long gas) {
        try {
            if (this.currentBlock != null) {
                this.currentBlock.getEvmBuilder().captureStart(from, to, code, gas);
            }
        } catch (Exception e) {
            logger.warn("captureStart failed, error: {}", e.getMessage());
        }
    }

    @Override
    public void captureEnd(long energyUsed, RuntimeException error) {
        try {
            if (this.currentBlock != null) {
                this.currentBlock.getEvmBuilder().captureEnd(energyUsed, error);
            }
        } catch (Exception e) {
            logger.warn("captureEnd failed, error: {}", e.getMessage());
        }
    }

    @Override
    public void captureFault(int opcodeNum, String opcodeName, long energy, Stack<DataWord> stackData, byte[] callerData, byte[] contractData, byte[] callValueData, int pc, byte[] memory, int callDepth, RuntimeException error) {
        try {
            if (this.currentBlock != null) {
                this.currentBlock.getEvmBuilder().captureFault(opcodeNum, opcodeName, energy, stackData, callerData, contractData, callValueData, pc, memory, callDepth, error);
            }
        } catch(Exception e) {
            logger.warn("captureFault failed, error: {}", e.getMessage());
        }
    }

    @Override
    public void captureEnter(byte[] from, byte[] to, byte[] data, long gas, byte[] value, int opCode, byte[] code) {
        try {
            if (this.currentBlock!=null) {
                this.currentBlock.getEvmBuilder().captureEnter(from, to, data, gas, value, opCode, code);
            }
        } catch (Exception e) {
            logger.warn("captureEnter failed, error: {}", e.getMessage());
        }
    }

    @Override
    public void captureExit(long energyUsed, RuntimeException error) {
        try {
            if (this.currentBlock != null) {
                this.currentBlock.getEvmBuilder().captureExit(energyUsed, error);
            }
        } catch(Exception e) {
            logger.warn("captureExit failed, error: {}", e.getMessage());
        }
    }

    @Override
    public void captureState(int opcodeNum, String opcodeName, long energy, int pc, int callDepth) {
        try {
            if (this.currentBlock != null) {
                this.currentBlock.getEvmBuilder().captureState(opcodeNum, opcodeName, energy, pc, callDepth);
            }
        } catch (Exception ex) {
            logger.warn("captureState failed, error: {}", ex.getMessage());
        }
    }

    @Override
    public void addLogToCaptureState(byte[] address, byte[] data, List<DataWord> topicsData, byte[] code) {
        try {
            if (this.currentBlock!=null) {
                this.currentBlock.getEvmBuilder().addLogToCaptureState(address, data, topicsData, code);
            }
        } catch (Exception e) {
            logger.warn("addLogToCaptureState failed, error: {}", e.getMessage());
        }
    }

    @Override
    public void addStorageToCaptureState(byte[] address, byte[] loc, byte[] value) {
        try {
            if (this.currentBlock!=null) {
                this.currentBlock.getEvmBuilder().addStorageToCaptureState(address, loc, value);
            }
        } catch (Exception e) {
            logger.warn("addStorageToCaptureState failed, error: {}", e.getMessage());
        }
    }
}
