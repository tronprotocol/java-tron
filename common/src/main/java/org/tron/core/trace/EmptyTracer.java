package org.tron.core.trace;

import lombok.extern.slf4j.Slf4j;
import org.tron.common.runtime.vm.DataWord;

import java.util.List;
import java.util.Stack;

@Slf4j(topic = "tracer")
public class EmptyTracer implements Tracer{
    @Override
    public void init(String tracerConfig) {
        logger.info("EmptyTracer initialization,  TracerConfigFile: {}", tracerConfig);
    }

    @Override
    public void blockStart(Object block) {
        logger.debug("blockStart triggered, Block: {}", block);
    }

    @Override
    public void blockEnd(Object block) {
        logger.debug("blockEnd triggered, Block: {}", block);
    }

    @Override
    public void captureStart(byte[] from, byte[] to, byte[] code, long gas) {
        logger.debug("captureStart triggered");
    }

    @Override
    public void captureEnd(long energyUsed, RuntimeException error) {
        logger.debug("captureEnd triggered");
    }

    @Override
    public void captureEnter(byte[] from, byte[] to, byte[] data, long gas, byte[] value, int opCode, byte[] code) {
        logger.debug("captureEnter triggered");
    }

    @Override
    public void captureExit(long energyUsed, RuntimeException error) {
        logger.debug("captureExit triggered");
    }

    @Override
    public void captureState(int opcodeNum, String opcodeName, long energy, int pc, int callDepth, RuntimeException error) {
        logger.debug("captureState triggered");
    }

    @Override
    public void captureFault(int opcodeNum, String opcodeName, long energy, Stack<DataWord> stackData, byte[] callerData, byte[] contractData, byte[] callValueData, int pc, byte[] memory, int callDepth, RuntimeException error) {
        logger.debug("captureFault triggered");
    }

    @Override
    public void addLogToCaptureState(byte[] address, byte[] data, List<DataWord> topicsData, byte[] code) {
        logger.debug("addLogToCaptureState triggered");
    }

    @Override
    public void addStorageToCaptureState(byte[] address, byte[] loc, byte[] value) {
        logger.debug("addStorageToCaptureState triggered");
    }
}
