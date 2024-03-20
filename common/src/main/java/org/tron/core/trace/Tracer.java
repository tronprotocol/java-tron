package org.tron.core.trace;

import com.google.protobuf.Message;
import org.tron.common.runtime.vm.DataWord;

import java.util.List;
import java.util.Stack;

public interface Tracer {
    public void init(String configFile) throws Exception;
    public void close();

    public void blockStart(Object block);
    public void blockEnd();

    public void transactionStart(Object tx);
    public void transactionEnd(Message protobufResultMessage, boolean isPending);

    public void captureStart(byte[] from, byte[] to, boolean create, byte[] input, byte[] code, long gas, byte[] value, String tokenId);
    public void captureEnd(long energyUsed, RuntimeException error);
    public void captureFault(int opcodeNum, String opcodeName, long energy, Stack<DataWord> stackData, byte[] callerData, byte[] contractData, byte[] callValueData, int pc, byte[] memory, int callDepth, RuntimeException error);
    public void captureEnter(byte[] from, byte[] to, byte[] data, long gas, byte[] value, int opCode, byte[] code, String tokenId);
    public void captureExit(long energyUsed, RuntimeException error);
    public void captureState(int opcodeNum, String opcodeName, long energy, int pc, int callDepth);
    public void addLogToCaptureState(byte[] address, byte[] data, List<DataWord> topicsData, byte[] code);
    public void addStorageToCaptureState(byte[] address, byte[] loc, byte[] value);
}
