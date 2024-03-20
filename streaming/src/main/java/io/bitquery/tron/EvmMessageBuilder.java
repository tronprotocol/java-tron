package io.bitquery.tron;

import com.google.protobuf.ByteString;
import io.bitquery.protos.EvmMessage.Trace;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.tron.common.runtime.vm.DataWord;
import io.bitquery.protos.EvmMessage.Contract;
import io.bitquery.protos.EvmMessage.CaptureFault;
import io.bitquery.protos.EvmMessage.Store;
import io.bitquery.protos.EvmMessage.Topic;
import io.bitquery.protos.EvmMessage.Log;
import io.bitquery.protos.EvmMessage.LogHeader;
import io.bitquery.protos.EvmMessage.CaptureExit;
import io.bitquery.protos.EvmMessage.CaptureStateHeader;
import io.bitquery.protos.EvmMessage.CaptureState;
import io.bitquery.protos.EvmMessage.Call;
import io.bitquery.protos.EvmMessage.Opcode;
import io.bitquery.protos.EvmMessage.CaptureEnter;
import io.bitquery.protos.EvmMessage.AddressCode;
import io.bitquery.protos.EvmMessage.CaptureEnd;
import io.bitquery.protos.EvmMessage.CaptureStart;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.vm.Op;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

@Slf4j(topic = "tracer")
public class EvmMessageBuilder {
    private Trace.Builder messageBuilder;

    private Call call;

    private int enterIndex;
    private int exitIndex;

    @Getter
    private List<Log> collectedLogs;

    private CaptureState currentCaptureState;

    public EvmMessageBuilder() {
        this.messageBuilder = Trace.newBuilder();
        this.call = null;

        this.enterIndex = 0;
        this.exitIndex = 0;

        this.collectedLogs = new ArrayList<>();
        this.currentCaptureState = null;
    }

    public Trace getMessage() {
        return messageBuilder.build();
    }

    public void captureStart(byte[] from, byte[] to, boolean create, byte[] input, byte[] code, long gas, byte[] value, String tokenId) {
        AddressCode addressCodeTo = addressCode(code);

        CaptureStart.Builder captureStartBuilder = CaptureStart.newBuilder()
                .setFrom(ByteString.copyFrom(from))
                .setTo(ByteString.copyFrom(to))
                .setCreate(create)
                .setInput(ByteString.copyFrom(input))
                .setGas(gas)
                .setValue(ByteString.copyFrom(value))
                .setToCode(addressCodeTo);

        if (StringUtils.isNotBlank(tokenId)) {
            captureStartBuilder.setTokenId(tokenId);
        }

        this.messageBuilder.setCaptureStart(captureStartBuilder.build());
    }

    public void captureEnd(long energyUsed, RuntimeException error) {
        CaptureEnd captureEnd = CaptureEnd.newBuilder()
                // .setOutput()
                .setGasUsed(energyUsed)
                .setError(getErrorString(error))
                .build();

        this.messageBuilder.setCaptureEnd(captureEnd);
    }

    public void captureEnter(byte[] from, byte[] to, byte[] data, long gas, byte[] value, int opCode, byte[] code, String tokenId) {
        String opcodeName = Op.getNameOf(opCode);

//        byte[] code = program.getContractState().getCode(to);
        AddressCode addressCodeTo = addressCode(code);

        Opcode opcode = Opcode.newBuilder()
                .setCode(opCode)
                .setName(opcodeName)
                .build();

        setCaptureEnter(
                ByteString.copyFrom(from),
                ByteString.copyFrom(to),
                ByteString.copyFrom(data),
                gas,
                ByteString.copyFrom(value),
                // byteStringTokensId,
                opcode,
                addressCodeTo,
                tokenId
        );
    }

    public void captureExit(long energyUsed, RuntimeException error) {
        setCaptureExit(energyUsed, error);
    }

    public void captureState(int opcodeNum, String opcodeName, long energy, int pc, int callDepth) {
        Opcode opcode = opcode(opcodeNum, opcodeName);

        if (skipOpcode(opcode)) {
            return;
        }

        CaptureStateHeader header = CaptureStateHeader.newBuilder()
                .setPc(pc)
                .setOpcode(opcode)
                .setGas(energy)
                .setCost(energy)
                .setDepth(callDepth)
                .setEnterIndex(this.enterIndex)
                .setExitIndex(this.exitIndex)
                .build();

        CaptureState captureState = CaptureState.newBuilder()
                .setCaptureStateHeader(header)
                .build();

        this.currentCaptureState = captureState;

        if (call != null) {
            this.call = call.toBuilder().addCaptureStates(currentCaptureState).build();
            return;
        }

        this.messageBuilder.addCaptureStates(currentCaptureState);
    }

    public void captureFault(int opcodeNum, String opcodeName, long energy, Stack<DataWord> stackData, byte[] callerData, byte[] contractData, byte[] callValueData, int pc, byte[] memory, int callDepth, RuntimeException error) {
        Opcode opcode = opcode(opcodeNum, opcodeName);

        List<ByteString> stack = new ArrayList<>();
        for (DataWord s : stackData) {
            ByteString byteStr = ByteString.copyFrom(s.getData());
            stack.add(byteStr);
        }

        Contract contract = Contract.newBuilder()
                .setCallerAddress(ByteString.copyFrom(callerData))
//                .setCaller()
                .setAddress(ByteString.copyFrom(contractData))
//                .setCodeAddr(ByteString.copyFrom(codeAddr))
//                .setInput()
                .setValue(ByteString.copyFrom(callValueData))
                .build();

        CaptureFault captureFault = CaptureFault.newBuilder()
                .setPc(pc)
                .setOpcode(opcode)
                .setGas(energy)
                .setCost(energy)
                .addAllStack(stack)
                .setContract(contract)
                .setMemory(ByteString.copyFrom(memory))
                .setDepth(callDepth)
                .setError(getErrorString(error))
                .build();

        this.messageBuilder.setCaptureFault(captureFault);
    }

    public void addLogToCaptureState(byte[] address, byte[] data, List<DataWord> topicsData, byte[] code) {
        AddressCode addressCode = addressCode(code);

        LogHeader logHeader = LogHeader.newBuilder()
                .setAddress(ByteString.copyFrom(address))
                .setData(ByteString.copyFrom(data))
                .setAddressCode(addressCode)
                .build();

        Log.Builder log = buildLog(logHeader, topicsData);

        this.currentCaptureState = currentCaptureState.toBuilder().setLog(log).build();

        if (call != null) {
            int lastCaptureStateIndex = call.getCaptureStatesCount() - 1;
            this.call = call.toBuilder().setCaptureStates(lastCaptureStateIndex, currentCaptureState).build();
        } else {
            int lastCaptureStateIndex = messageBuilder.getCaptureStatesCount() - 1;
            this.messageBuilder.setCaptureStates(lastCaptureStateIndex, currentCaptureState);
        }

        this.collectedLogs.add(log.build());
    }

    public void addStorageToCaptureState(byte[] address, byte[] loc, byte[] value) {
        Store store = Store.newBuilder()
                .setAddress(ByteString.copyFrom(address))
                .setLocation(ByteString.copyFrom(loc))
                .setValue(ByteString.copyFrom(value))
                .build();

        this.currentCaptureState = currentCaptureState.toBuilder().setStore(store).build();

        if (call != null) {
            int lastCaptureStateIndex = call.getCaptureStatesCount() - 1;
            this.call = call.toBuilder().setCaptureStates(lastCaptureStateIndex, currentCaptureState).build();
        } else {
            int lastCaptureStateIndex = messageBuilder.getCaptureStatesCount() - 1;
            this.messageBuilder.setCaptureStates(lastCaptureStateIndex, currentCaptureState);
        }
    }

    private void setCaptureEnter(ByteString from, ByteString to, ByteString data, long energy, ByteString value, Opcode opcode, AddressCode addressCodeTo, String tokenId) {
        this.enterIndex += 1;

        CaptureEnter.Builder captureEnterBuilder = CaptureEnter.newBuilder()
                .setOpcode(opcode)
                .setFrom(from)
                .setTo(to)
                .setInput(data)
                .setGas(energy)
                .setValue(value)
                .setToCode(addressCodeTo);

        if (StringUtils.isNotBlank(tokenId)) {
            captureEnterBuilder.setTokenId(tokenId);
        }

        int depth = 1;
        int callerIndex = -1;

        if (this.call != null) {
            depth = this.call.getDepth() + 1;
            callerIndex = this.call.getIndex();
        }

        this.call = Call.newBuilder()
                .setDepth(depth)
                .setCaptureEnter(captureEnterBuilder.build())
                .setCallerIndex(callerIndex)
                .setIndex(0)
                .setEnterIndex(this.enterIndex)
                .setExitIndex(this.exitIndex)
                .build();

        int callsCount = this.messageBuilder.getCallsCount();
        if (callsCount != 0) {
            this.call = this.call.toBuilder().setIndex(callsCount).build();
        }

        this.messageBuilder.addCalls(this.call);
    }

    private void setCaptureExit(long energyUsed, RuntimeException error) {
        this.exitIndex += 1;

        CaptureExit captureExit = CaptureExit.newBuilder()
                // .setOutput(output)
                .setGasUsed(energyUsed)
                .setError(getErrorString(error))
                .build();

        if (this.call == null) {
            logger.warn("captureExit event can't be set, since there is no call");
            return;
        }

        // Add captureExit to already existed call record.
        Call callWithExit = this.call.toBuilder().setCaptureExit(captureExit).build();
        this.messageBuilder.setCalls(this.call.getIndex(), callWithExit);

        int callerIndex = this.call.getCallerIndex();
        if (callerIndex >= 0) {
            this.call = this.messageBuilder.getCalls(callerIndex);
        } else {
            this.call = null;
        }
    }

    private Log.Builder buildLog(LogHeader header, List<DataWord> topicsData) {
        Log.Builder log = Log.newBuilder().setLogHeader(header);

        int index = 0;
        for (DataWord topicData : topicsData) {
            Topic topic = Topic.newBuilder()
                    .setIndex(index)
                    .setHash(ByteString.copyFrom(topicData.getData()))
                    .build();

            log.addTopics(topic);

            index++;
        }

        return log;
    }

    private Opcode opcode(int code, String name) {
        Opcode opcode = Opcode.newBuilder()
                .setCode(code)
                .setName(name)
                .build();

        return opcode;
    }

    private AddressCode addressCode(byte[] code) {
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
}
