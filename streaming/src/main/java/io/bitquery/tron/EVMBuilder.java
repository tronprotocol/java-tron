package io.bitquery.tron;

import evm_messages.BlockMessageOuterClass;
import org.tron.common.runtime.vm.DataWord;
import org.tron.protos.streaming.TronMessage;

import java.util.List;
import java.util.Stack;

public class EVMBuilder {


    // private protobuf

    public void captureStart(byte[] from, byte[] to, byte[] code, long gas) {

    }

    public void captureEnd(long energyUsed, RuntimeException error) {
    }

    public void captureFault(int opcodeNum, String opcodeName, long energy, Stack<DataWord> stackData, byte[] callerData, byte[] contractData, byte[] callValueData, int pc, byte[] memory, int callDepth, RuntimeException error) {
    }

    public void captureEnter(byte[] from, byte[] to, byte[] data, long gas, byte[] value, int opCode, byte[] code) {
    }

    public void captureExit(long energyUsed, RuntimeException error) {
    }

    public void captureState(int opcodeNum, String opcodeName, long energy, int pc, int callDepth) {
    }

    public void addLogToCaptureState(byte[] address, byte[] data, List<DataWord> topicsData, byte[] code) {
    }

    public void addStorageToCaptureState(byte[] address, byte[] loc, byte[] value) {
    }

    public BlockMessageOuterClass.Trace getMessage() {

    }
}
