package org.tron.core.tracing;

import lombok.extern.slf4j.Slf4j;
import org.tron.common.runtime.vm.DataWord;

import java.util.List;
import java.util.Stack;

@Slf4j(topic = "emptyTracer")
public class EmptyTracer implements Tracer{
    @Override
    public void init(String implementationConfigFile) {
        logger.info("init {}",implementationConfigFile);
    }

    @Override
    public void blockStart(Object block) {
        logger.debug("blockStart {}",block);
    }

    @Override
    public void blockEnd(Object block) {
        logger.debug("blockEnd {}",block);
    }

    @Override
    public void captureStart(byte[] from, byte[] to, byte[] code, long gas) {
        logger.debug("captureStart");
    }

    @Override
    public void captureEnd(long energyUsed, RuntimeException error) {
        logger.debug("captureEnd");
    }

    @Override
    public void captureEnter(byte[] from, byte[] to, byte[] data, long gas, byte[] value, int opCode, byte[] code) {
        logger.debug("captureEnter");
    }

    @Override
    public void captureExit(long energyUsed, RuntimeException error) {
        logger.debug("captureExit");
    }

    @Override
    public void captureState(int opcodeNum, String opcodeName, long energy, int pc, int callDepth, RuntimeException error) {
        logger.debug("captureState");
    }

    @Override
    public void captureFault(int opcodeNum, String opcodeName, long energy, Stack<DataWord> stackData, byte[] callerData, byte[] contractData, byte[] callValueData, int pc, byte[] memory, int callDepth, RuntimeException error) {
        logger.debug("captureFault");
    }

    @Override
    public void addLogToCaptureState(byte[] address, byte[] data, List<DataWord> topicsData, byte[] code) {
        logger.debug("addLogToCaptureState");
    }

    @Override
    public void addStorageToCaptureState(byte[] address, byte[] loc, byte[] value) {
        logger.debug("addStorageToCaptureState");
    }
}
