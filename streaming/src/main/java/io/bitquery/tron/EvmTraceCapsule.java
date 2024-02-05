package io.bitquery.tron;

import com.google.protobuf.ByteString;
import evm_messages.BlockMessageOuterClass.Contract;
import evm_messages.BlockMessageOuterClass.CaptureFault;
import evm_messages.BlockMessageOuterClass.Store;
import evm_messages.BlockMessageOuterClass.Topic;
import evm_messages.BlockMessageOuterClass.Log;
import evm_messages.BlockMessageOuterClass.LogHeader;
import evm_messages.BlockMessageOuterClass.CaptureExit;
import evm_messages.BlockMessageOuterClass.CaptureStateHeader;
import evm_messages.BlockMessageOuterClass.CaptureState;
import evm_messages.BlockMessageOuterClass.Call;
import evm_messages.BlockMessageOuterClass.Opcode;
import evm_messages.BlockMessageOuterClass.CaptureEnter;
import evm_messages.BlockMessageOuterClass.AddressCode;
import evm_messages.BlockMessageOuterClass.CaptureEnd;
import evm_messages.BlockMessageOuterClass.CaptureStart;
import evm_messages.BlockMessageOuterClass.Trace;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.ByteUtil;
import org.tron.core.capsule.EvmTraceCapsuleI;

import java.util.Arrays;
import java.util.List;

@Slf4j(topic = "capsule")
public class EvmTraceCapsule implements EvmTraceCapsuleI {
    private Trace.Builder traceBuilder;
    private Call call;

    private int enterIndex;
    private int exitIndex;

    public EvmTraceCapsule() {
        this.traceBuilder = Trace.newBuilder();
        this.call = null;

        this.enterIndex = 0;
        this.exitIndex = 0;
    }

    @Override
    public void setCaptureStart(ByteString from, ByteString to, long energy, AddressCode addressCodeTo) {
        CaptureStart captureStart = CaptureStart.newBuilder()
                .setFrom(from)
                .setTo(to)
//                .setCreate(create)
//                .setInput(data)
                .setGas(energy)
//                .setValue(value)
                .setToCode(addressCodeTo)
                .build();

        this.traceBuilder.setCaptureStart(captureStart);
    }

    @Override
    public void setCaptureEnd(long energyUsed, RuntimeException error) {
        CaptureEnd captureEnd = CaptureEnd.newBuilder()
                // .setOutput()
                .setGasUsed(energyUsed)
                .setError(getErrorString(error))
                .build();

        this.traceBuilder.setCaptureEnd(captureEnd);
    }

    @Override
    public void setCaptureEnter(ByteString from, ByteString to, ByteString data, long energy, ByteString value, Opcode opcode, AddressCode addressCodeTo) {
        this.enterIndex += 1;

        CaptureEnter captureEnter = CaptureEnter.newBuilder()
                .setOpcode(opcode)
                .setFrom(from)
                .setTo(to)
                .setInput(data)
                .setGas(energy)
                .setValue(value)
                .setToCode(addressCodeTo)
                .build();

        int depth = 1;
        int callerIndex = -1;

        if (this.call != null) {
            depth = this.call.getDepth() + 1;
            callerIndex = this.call.getIndex();
        }

        this.call = Call.newBuilder()
                .setDepth(depth)
                .setCaptureEnter(captureEnter)
                .setCallerIndex(callerIndex)
                .setIndex(0)
                .setEnterIndex(this.enterIndex)
                .setExitIndex(this.exitIndex)
                .build();

        int callsCount = this.traceBuilder.getCallsCount();
        if (callsCount != 0) {
            this.call = this.call.toBuilder().setIndex(callsCount).build();
        }

        this.traceBuilder.addCalls(this.call);
    }

    // CaptureExit should always be called after CaptureEnter
    // Otherwise nothing is changed
    @Override
    public void setCaptureExit(long energyUsed, RuntimeException error) {
        this.exitIndex += 1;

        CaptureExit captureExit = CaptureExit.newBuilder()
                // .setOutput(output)
                .setGasUsed(energyUsed)
                .setError(getErrorString(error))
                .build();

        if (this.call == null) {
            return;
        }

        // Add captureExit to already existed call record.
        Call callWithExit = this.call.toBuilder().setCaptureExit(captureExit).build();
        this.traceBuilder.setCalls(this.call.getIndex(), callWithExit);

        int callerIndex = this.call.getCallerIndex();
        if (callerIndex >= 0) {
            this.call = this.traceBuilder.getCalls(callerIndex);
        } else {
            this.call = null;
        }
    }

    @Override
    public void addCaptureState(int pc, Opcode opcode, long energy, long cost, int depth, RuntimeException error) {
        if (skipOpcode(opcode)) {
            return;
        }

        CaptureStateHeader header = CaptureStateHeader.newBuilder()
                .setPc(pc)
                .setOpcode(opcode)
                .setGas(energy)
                .setCost(cost)
                .setDepth(depth)
                .setError(getErrorString(error))
                .setEnterIndex(this.enterIndex)
                .setExitIndex(this.exitIndex)
                .build();

        CaptureState captureState = CaptureState.newBuilder()
                .setCaptureStateHeader(header)
//                .setLog()
//                .setStore()
                .build();


        this.traceBuilder.addCaptureStates(captureState);
    }

    @Override
    public void addLogToCaptureState(byte[] address, byte[] data, AddressCode addressCode, List<DataWord> topicsData) {
        int lastIndex = this.traceBuilder.getCaptureStatesCount() - 1;
        if (lastIndex == -1) {
            return;
        }

        LogHeader logHeader = LogHeader.newBuilder()
                .setAddress(ByteString.copyFrom(address))
                .setData(ByteString.copyFrom(data))
                .setAddressCode(addressCode)
                .build();

        Log.Builder log = Log.newBuilder().setLogHeader(logHeader);

        int index = 0;
        for (DataWord topicData : topicsData) {
            Topic topic = Topic.newBuilder()
                    .setIndex(index)
                    .setHash(ByteString.copyFrom(topicData.getData()))
                    .build();

            log.addTopics(topic);

            index++;
        }

        CaptureState captureStateWithLog = this.traceBuilder.getCaptureStates(lastIndex).toBuilder()
                .setLog(log)
                .build();

        this.traceBuilder.setCaptureStates(lastIndex, captureStateWithLog);
    }

    @Override
    public void addStorageToCaptureState(byte[] address, byte[] loc, byte[] value) {
        int lastIndex = this.traceBuilder.getCaptureStatesCount() - 1;
        if (lastIndex == -1) {
            return;
        }

        Store store = Store.newBuilder()
                .setAddress(ByteString.copyFrom(address))
                .setLocation(ByteString.copyFrom(loc))
                .setValue(ByteString.copyFrom(value))
                .build();

        CaptureState captureStateWithStore = this.traceBuilder.getCaptureStates(lastIndex).toBuilder()
                .setStore(store)
                .build();

        this.traceBuilder.setCaptureStates(lastIndex, captureStateWithStore);
    }

    @Override
    public void setCaptureFault(int pc, Opcode opcode, long energy, long cost, int depth, RuntimeException error, List<ByteString> stack, Contract contract, byte[] memory) {
        CaptureFault captureFault = CaptureFault.newBuilder()
                .setPc(pc)
                .setOpcode(opcode)
                .setGas(energy)
                .setCost(cost)
                .addAllStack(stack)
                .setContract(contract)
                .setMemory(ByteString.copyFrom(memory))
                .setDepth(depth)
                .setError(getErrorString(error))
                .build();

        this.traceBuilder.setCaptureFault(captureFault);
    }

    @Override
    public AddressCode addressCode(byte[] code) {
        if (code == null) {
            code = ByteUtil.EMPTY_BYTE_ARRAY;
        }

        ByteString hash =  ByteString.copyFrom(code);
        int size = code.length;

        AddressCode addressCode = AddressCode.newBuilder()
                .setHash(hash)
                .setSize(size)
                .build();

        return addressCode;
    }

    @Override
    public Opcode opcode(int code, String name) {
        Opcode opcode = Opcode.newBuilder()
                .setCode(code)
                .setName(name)
                .build();

        return opcode;
    }

    @Override
    public Contract contract(byte[] callerAddress, byte[] address, byte[] codeAddr, byte[] value) {
        // callerAddress and caller are the same fields.
        Contract contract = Contract.newBuilder()
                .setCallerAddress(ByteString.copyFrom(callerAddress))
//                .setCaller()
                .setAddress(ByteString.copyFrom(address))
                .setCodeAddr(ByteString.copyFrom(codeAddr))
//                .setInput()
                .setValue(ByteString.copyFrom(value))
                .build();

        return contract;
    }

    private String getErrorString(RuntimeException error) {
        if (error != null) {
            return error.getMessage();
        }

        return "";
    }

    // List of opcodes that will not be recorded in captureState
    private boolean skipOpcode(Opcode opcode) {
        int code = opcode.getCode();
        String name = opcode.getName();

        List<String> skippedNames = Arrays.asList("POP", "JUMP", "JUMPI", "JUMPDEST", "MSTORE", "MSTORE8");
        if (skippedNames.contains(name)) {
            return true;
        }

        // Arithmetic operations
        if (code >= 1 && code <= 11) {
            return true;
        }

        // Bitwise, comparison and cryptographic operations
        if (code >= 16 && code <= 32) {
            return true;
        }

        // Push, dup and swap operations
        if (code >= 95 && code <= 159) {
            return true;
        }

        return false;
    }

    @Override
    public byte[] getData() { return this.traceBuilder.build().toByteArray();}

    @Override
    public Trace getInstance() { return this.traceBuilder.build();}
}
