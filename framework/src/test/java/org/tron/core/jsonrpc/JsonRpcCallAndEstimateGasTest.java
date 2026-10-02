package org.tron.core.jsonrpc;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.tron.api.GrpcAPI.EstimateEnergyMessage;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.Manager;
import org.tron.core.exception.jsonrpc.JsonRpcExecutionRevertedException;
import org.tron.core.exception.jsonrpc.JsonRpcInternalException;
import org.tron.core.services.NodeInfoService;
import org.tron.core.services.jsonrpc.TronJsonRpcImpl;
import org.tron.core.services.jsonrpc.types.CallArguments;
import org.tron.core.vm.program.Program;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;

public class JsonRpcCallAndEstimateGasTest {

  private static final String ERROR_REVERT_HEX = "08c379a0"
      + "0000000000000000000000000000000000000000000000000000000000000020"
      + "0000000000000000000000000000000000000000000000000000000000000016"
      + "6e6f7420656e6f75676820696e7075742076616c756500000000000000000000";
  private static final String REVERT_MSG = "REVERT opcode executed";
  private static final String MOCK_FROM_ADDRESS = "0x0000000000000000000000000000000000000000";
  private static final String MOCK_TO_ADDRESS = "0x0000000000000000000000000000000000000001";

  private enum EstimatePath {
    CONSTANT_CALL,
    ESTIMATE_ENERGY
  }

  private final boolean originalEstimateEnergy = CommonParameter.getInstance().isEstimateEnergy();
  private TronJsonRpcImpl mockRpc;

  @After
  public void tearDown() throws Exception {
    if (mockRpc != null) {
      mockRpc.close();
      mockRpc = null;
    }
    CommonParameter.getInstance().setEstimateEnergy(originalEstimateEnergy);
  }

  @Test
  public void testGetCallAppendsRevertReason() throws Exception {
    byte[] revertData = ByteArray.fromHexString(ERROR_REVERT_HEX);

    mockRpc = newRpcWithMockedFailedCall(revertData, contractResult.REVERT,
        REVERT_MSG, EstimatePath.CONSTANT_CALL);

    JsonRpcExecutionRevertedException e = assertThrows(JsonRpcExecutionRevertedException.class,
        () -> mockRpc.getCall(newCallArgs(), "latest"));
    Assert.assertEquals(REVERT_MSG + ": not enough input value", e.getMessage());
    Assert.assertEquals("0x" + ERROR_REVERT_HEX, e.getData());
  }

  @Test
  public void testGetCallSkipsRevertReasonForPanicSelector() throws Exception {
    String panicHex = "4e487b71"
        + "0000000000000000000000000000000000000000000000000000000000000001";
    byte[] panicData = ByteArray.fromHexString(panicHex);

    mockRpc = newRpcWithMockedFailedCall(panicData, contractResult.REVERT,
        REVERT_MSG, EstimatePath.CONSTANT_CALL);

    JsonRpcExecutionRevertedException e = assertThrows(JsonRpcExecutionRevertedException.class,
        () -> mockRpc.getCall(newCallArgs(), "latest"));
    Assert.assertEquals(REVERT_MSG, e.getMessage());
    Assert.assertEquals("0x" + panicHex, e.getData());
  }

  @Test
  public void testGetCallSkipsRevertReasonForShortData() throws Exception {
    mockRpc = newRpcWithMockedFailedCall(new byte[] {1, 2, 3}, contractResult.REVERT,
        REVERT_MSG, EstimatePath.CONSTANT_CALL);

    JsonRpcExecutionRevertedException e = assertThrows(JsonRpcExecutionRevertedException.class,
        () -> mockRpc.getCall(newCallArgs(), "latest"));
    Assert.assertEquals(REVERT_MSG, e.getMessage());
    Assert.assertEquals("0x010203", e.getData());
  }

  @Test
  public void testEstimateGasAppendsRevertReason() throws Exception {
    byte[] revertData = ByteArray.fromHexString(ERROR_REVERT_HEX);

    mockRpc = newRpcWithMockedFailedCall(revertData, contractResult.REVERT,
        REVERT_MSG, EstimatePath.CONSTANT_CALL);
    CommonParameter.getInstance().setEstimateEnergy(false);

    JsonRpcExecutionRevertedException e = assertThrows(JsonRpcExecutionRevertedException.class,
        () -> mockRpc.estimateGas(newCallArgs()));
    Assert.assertEquals(REVERT_MSG + ": not enough input value", e.getMessage());
    Assert.assertEquals("0x" + ERROR_REVERT_HEX, e.getData());
  }

  @Test
  public void testEstimateGasSkipsRevertReasonForEmptyData() throws Exception {
    mockRpc = newRpcWithMockedFailedCall(new byte[0], contractResult.REVERT,
        REVERT_MSG, EstimatePath.CONSTANT_CALL);
    CommonParameter.getInstance().setEstimateEnergy(false);

    JsonRpcExecutionRevertedException e = assertThrows(JsonRpcExecutionRevertedException.class,
        () -> mockRpc.estimateGas(newCallArgs()));
    Assert.assertEquals(REVERT_MSG, e.getMessage());
    Assert.assertEquals("0x", e.getData());
  }

  @Test
  public void testEstimateGasWithEstimateEnergyAppendsRevertReason() throws Exception {
    byte[] revertData = ByteArray.fromHexString(ERROR_REVERT_HEX);

    mockRpc = newRpcWithMockedFailedCall(revertData, contractResult.REVERT,
        REVERT_MSG, EstimatePath.ESTIMATE_ENERGY);
    CommonParameter.getInstance().setEstimateEnergy(true);

    JsonRpcExecutionRevertedException e = assertThrows(JsonRpcExecutionRevertedException.class,
        () -> mockRpc.estimateGas(newCallArgs()));
    Assert.assertEquals(REVERT_MSG + ": not enough input value", e.getMessage());
    Assert.assertEquals("0x" + ERROR_REVERT_HEX, e.getData());
  }

  @Test
  public void testEstimateGasWithEstimateEnergySkipsRevertReasonForShortData() throws Exception {
    mockRpc = newRpcWithMockedFailedCall(new byte[] {1, 2, 3}, contractResult.REVERT,
        REVERT_MSG, EstimatePath.ESTIMATE_ENERGY);
    CommonParameter.getInstance().setEstimateEnergy(true);

    JsonRpcExecutionRevertedException e = assertThrows(JsonRpcExecutionRevertedException.class,
        () -> mockRpc.estimateGas(newCallArgs()));
    Assert.assertEquals(REVERT_MSG, e.getMessage());
    Assert.assertEquals("0x010203", e.getData());
  }

  @Test
  public void testEstimateGasWithEstimateEnergyReturnsEstimatedEnergy() throws Exception {
    long energyRequired = 0x4321L;

    mockRpc = newRpcWithMockedEstimateGasSuccessfulCall(energyRequired,
        EstimatePath.ESTIMATE_ENERGY);
    CommonParameter.getInstance().setEstimateEnergy(true);

    String result = mockRpc.estimateGas(newCallArgs());

    Assert.assertEquals(ByteArray.toJsonHex(energyRequired), result);
  }

  @Test
  public void testGetCallNonRevertFailureIsNotExecutionReverted() throws Exception {
    mockRpc = newRpcWithMockedFailedCall(new byte[0], contractResult.OUT_OF_ENERGY,
        "Out of energy", EstimatePath.CONSTANT_CALL);

    JsonRpcInternalException e = assertThrows(JsonRpcInternalException.class,
        () -> mockRpc.getCall(newCallArgs(), "latest"));
    Assert.assertEquals("Out of energy", e.getMessage());
    Assert.assertNull(e.getData());
  }

  @Test
  public void testGetCallNonRevertFailureAttachesReturnData() throws Exception {
    byte[] resData = ByteArray.fromHexString("deadbeef00");
    mockRpc = newRpcWithMockedFailedCall(resData, contractResult.DEFAULT,
        "Unknown failure", EstimatePath.CONSTANT_CALL);

    JsonRpcInternalException e = assertThrows(JsonRpcInternalException.class,
        () -> mockRpc.getCall(newCallArgs(), "latest"));
    Assert.assertEquals("Unknown failure", e.getMessage());
    Assert.assertEquals("0xdeadbeef00", e.getData());
  }

  @Test
  public void testGetCallThrownVmExceptionIsInternalError() throws Exception {
    // for a plain eth_call, Wallet rethrows every non-revert VM failure instead of
    // returning a FAILED result, so it must surface as -32000 without data
    mockRpc = newRpcWithThrowingCall(new Program.OutOfEnergyException("Not enough energy"));

    JsonRpcInternalException e = assertThrows(JsonRpcInternalException.class,
        () -> mockRpc.getCall(newCallArgs(), "latest"));
    Assert.assertEquals("Not enough energy", e.getMessage());
    Assert.assertNull(e.getData());
  }

  @Test
  public void testEstimateGasWithEstimateEnergyNonRevertFailureIsNotExecutionReverted()
      throws Exception {
    mockRpc = newRpcWithMockedFailedCall(new byte[0], contractResult.OUT_OF_ENERGY,
        "Out of energy", EstimatePath.ESTIMATE_ENERGY);
    CommonParameter.getInstance().setEstimateEnergy(true);

    JsonRpcInternalException e = assertThrows(JsonRpcInternalException.class,
        () -> mockRpc.estimateGas(newCallArgs()));
    Assert.assertEquals("Out of energy", e.getMessage());
    Assert.assertNull(e.getData());
  }

  @Test
  public void testEstimateGasWithEstimateEnergyNonRevertFailureAttachesReturnData()
      throws Exception {
    byte[] resData = ByteArray.fromHexString("deadbeef00");
    mockRpc = newRpcWithMockedFailedCall(resData, contractResult.OUT_OF_ENERGY,
        "Out of energy", EstimatePath.ESTIMATE_ENERGY);
    CommonParameter.getInstance().setEstimateEnergy(true);

    JsonRpcInternalException e = assertThrows(JsonRpcInternalException.class,
        () -> mockRpc.estimateGas(newCallArgs()));
    Assert.assertEquals("Out of energy", e.getMessage());
    Assert.assertEquals("0xdeadbeef00", e.getData());
  }

  @Test
  public void testGetCallReturnsConstantResult() throws Exception {
    byte[] part1 = ByteArray.fromHexString("deadbeef");
    byte[] part2 = ByteArray.fromHexString("cafebabe");

    mockRpc = newRpcWithMockedSuccessfulCall(part1, part2);

    String result = mockRpc.getCall(newCallArgs(), "latest");

    Assert.assertEquals("0xdeadbeefcafebabe", result);
  }

  @Test
  public void testEstimateGasReturnsEnergyUsed() throws Exception {
    long energyUsed = 0x1234L;

    mockRpc = newRpcWithMockedEstimateGasSuccessfulCall(energyUsed, EstimatePath.CONSTANT_CALL);
    CommonParameter.getInstance().setEstimateEnergy(false);

    String result = mockRpc.estimateGas(newCallArgs());

    Assert.assertEquals(ByteArray.toJsonHex(energyUsed), result);
  }

  private static CallArguments newCallArgs() {
    CallArguments args = new CallArguments();
    args.setFrom(MOCK_FROM_ADDRESS);
    args.setTo(MOCK_TO_ADDRESS);
    args.setValue("0x0");
    args.setData("0x");
    return args;
  }

  private static TronJsonRpcImpl newRpcWithMockedFailedCall(byte[] resData,
      contractResult contractRet, String message, EstimatePath path) throws Exception {
    Wallet mockWallet = mock(Wallet.class);
    Manager mockManager = mock(Manager.class);
    NodeInfoService mockNodeInfo = mock(NodeInfoService.class);

    when(mockWallet.createTransactionCapsule(any(), any()))
        .thenReturn(new TransactionCapsule(Protocol.Transaction.newBuilder().build()));
    when(mockWallet.getContract(any())).thenReturn(SmartContract.getDefaultInstance());

    Protocol.Transaction failedTransaction = Protocol.Transaction.newBuilder()
        .addRet(Protocol.Transaction.Result.newBuilder()
            .setRet(Protocol.Transaction.Result.code.FAILED)
            .setContractRet(contractRet))
        .build();

    if (path == EstimatePath.ESTIMATE_ENERGY) {
      when(mockWallet.estimateEnergy(any(), any(), any(), any(), any()))
          .thenAnswer(invocation -> {
            TransactionExtention.Builder extBuilder = invocation.getArgument(2);
            Return.Builder retBuilder = invocation.getArgument(3);
            EstimateEnergyMessage.Builder estimateBuilder = invocation.getArgument(4);
            extBuilder.addConstantResult(ByteString.copyFrom(resData));
            retBuilder.setMessage(ByteString.copyFromUtf8(message));
            estimateBuilder.setResult(retBuilder);
            return failedTransaction;
          });
    } else {
      when(mockWallet.triggerConstantContract(any(), any(), any(), any()))
          .thenAnswer(invocation -> {
            TransactionExtention.Builder extBuilder = invocation.getArgument(2);
            Return.Builder retBuilder = invocation.getArgument(3);
            extBuilder.addConstantResult(ByteString.copyFrom(resData));
            retBuilder.setMessage(ByteString.copyFromUtf8(message));
            return failedTransaction;
          });
    }

    TronJsonRpcImpl rpc = new TronJsonRpcImpl(mockNodeInfo, mockWallet);
    rpc.setManager(mockManager);
    return rpc;
  }

  private static TronJsonRpcImpl newRpcWithThrowingCall(RuntimeException vmException)
      throws Exception {
    Wallet mockWallet = mock(Wallet.class);
    Manager mockManager = mock(Manager.class);
    NodeInfoService mockNodeInfo = mock(NodeInfoService.class);

    when(mockWallet.createTransactionCapsule(any(), any()))
        .thenReturn(new TransactionCapsule(Protocol.Transaction.newBuilder().build()));
    when(mockWallet.getContract(any())).thenReturn(SmartContract.getDefaultInstance());
    when(mockWallet.triggerConstantContract(any(), any(), any(), any()))
        .thenThrow(vmException);

    TronJsonRpcImpl rpc = new TronJsonRpcImpl(mockNodeInfo, mockWallet);
    rpc.setManager(mockManager);
    return rpc;
  }

  private static TronJsonRpcImpl newRpcWithMockedSuccessfulCall(byte[]... constantResults)
      throws Exception {
    Wallet mockWallet = mock(Wallet.class);
    Manager mockManager = mock(Manager.class);
    NodeInfoService mockNodeInfo = mock(NodeInfoService.class);

    when(mockWallet.createTransactionCapsule(any(), any()))
        .thenReturn(new TransactionCapsule(Protocol.Transaction.newBuilder().build()));
    when(mockWallet.getContract(any())).thenReturn(SmartContract.getDefaultInstance());

    when(mockWallet.triggerConstantContract(any(), any(), any(), any()))
        .thenAnswer(invocation -> {
          TransactionExtention.Builder extBuilder = invocation.getArgument(2);
          for (byte[] bytes : constantResults) {
            extBuilder.addConstantResult(ByteString.copyFrom(bytes));
          }
          extBuilder.setEnergyUsed(0L);
          return Protocol.Transaction.newBuilder()
              .addRet(Protocol.Transaction.Result.newBuilder()
                  .setRet(Protocol.Transaction.Result.code.SUCESS))
              .build();
        });

    TronJsonRpcImpl rpc = new TronJsonRpcImpl(mockNodeInfo, mockWallet);
    rpc.setManager(mockManager);
    return rpc;
  }

  private static TronJsonRpcImpl newRpcWithMockedEstimateGasSuccessfulCall(long energyValue,
      EstimatePath path) throws Exception {
    Wallet mockWallet = mock(Wallet.class);
    Manager mockManager = mock(Manager.class);
    NodeInfoService mockNodeInfo = mock(NodeInfoService.class);

    when(mockWallet.createTransactionCapsule(any(), any()))
        .thenReturn(new TransactionCapsule(Protocol.Transaction.newBuilder().build()));
    when(mockWallet.getContract(any())).thenReturn(SmartContract.getDefaultInstance());

    if (path == EstimatePath.ESTIMATE_ENERGY) {
      when(mockWallet.estimateEnergy(any(), any(), any(), any(), any()))
          .thenAnswer(invocation -> {
            EstimateEnergyMessage.Builder estimateBuilder = invocation.getArgument(4);
            estimateBuilder.setEnergyRequired(energyValue);
            return Protocol.Transaction.newBuilder()
                .addRet(Protocol.Transaction.Result.newBuilder()
                    .setRet(Protocol.Transaction.Result.code.SUCESS))
                .build();
          });
    } else {
      when(mockWallet.triggerConstantContract(any(), any(), any(), any()))
          .thenAnswer(invocation -> {
            TransactionExtention.Builder extBuilder = invocation.getArgument(2);
            extBuilder.setEnergyUsed(energyValue);
            return Protocol.Transaction.newBuilder()
                .addRet(Protocol.Transaction.Result.newBuilder()
                    .setRet(Protocol.Transaction.Result.code.SUCESS))
                .build();
          });
    }

    TronJsonRpcImpl rpc = new TronJsonRpcImpl(mockNodeInfo, mockWallet);
    rpc.setManager(mockManager);
    return rpc;
  }
}
