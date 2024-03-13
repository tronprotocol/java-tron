package org.tron.core.trace;

import com.google.protobuf.Message;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.runtime.vm.DataWord;

import java.util.List;
import java.util.Stack;

@Slf4j(topic = "tracer")
public class EmptyTracer implements Tracer {

    @Override
    public void init(String configFile) {}

    @Override
    public void close() {}

    @Override
    public void blockStart(Object block) {}

    @Override
    public void blockEnd() {}

    @Override
    public void transactionStart(Object tx) {}

    @Override
    public void transactionEnd(Message protobufResultMessage, boolean isPending) {}

    @Override
    public void captureStart(byte[] from, byte[] to, boolean create, byte[] input, byte[] code, long gas, byte[] value) {}

    @Override
    public void captureEnd(long energyUsed, RuntimeException error) {}

    @Override
    public void captureEnter(byte[] from, byte[] to, byte[] data, long gas, byte[] value, List<byte[]> tokensId, int opCode, byte[] code) {}

    @Override
    public void captureExit(long energyUsed, RuntimeException error) {}

    @Override
    public void captureState(int opcodeNum, String opcodeName, long energy, int pc, int callDepth) {}

    @Override
    public void captureFault(int opcodeNum, String opcodeName, long energy, Stack<DataWord> stackData, byte[] callerData, byte[] contractData, byte[] callValueData, int pc, byte[] memory, int callDepth, RuntimeException error) {}

    @Override
    public void addLogToCaptureState(byte[] address, byte[] data, List<DataWord> topicsData, byte[] code) {}

    @Override
    public void addStorageToCaptureState(byte[] address, byte[] loc, byte[] value) {}
}
