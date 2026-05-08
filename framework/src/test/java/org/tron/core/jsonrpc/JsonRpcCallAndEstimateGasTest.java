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
import org.tron.core.exception.jsonrpc.JsonRpcInternalException;
import org.tron.core.services.NodeInfoService;
import org.tron.core.services.jsonrpc.TronJsonRpcImpl;
import org.tron.core.services.jsonrpc.types.CallArguments;
import org.tron.protos.Protocol;
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

    mockRpc = newRpcWithMockedFailedCall(revertData, EstimatePath.CONSTANT_CALL);

    JsonRpcInternalException e = assertThrows(JsonRpcInternalException.class,
        () -> mockRpc.getCall(newCallArgs(), "latest"));
    Assert.assertEquals(REVERT_MSG + ": not enough input value", e.getMessage());
  }

  @Test
  public void testGetCallSkipsRevertReasonForPanicSelector() throws Exception {
    byte[] panicData = ByteArray.fromHexString("4e487b71"
        + "0000000000000000000000000000000000000000000000000000000000000001");

    mockRpc = newRpcWithMockedFailedCall(panicData, EstimatePath.CONSTANT_CALL);

    JsonRpcInternalException e = assertThrows(JsonRpcInternalException.class,
        () -> mockRpc.getCall(newCallArgs(), "latest"));
    Assert.assertEquals(REVERT_MSG, e.getMessage());
  }

  @Test
  public void testGetCallSkipsRevertReasonForShortData() throws Exception {
    mockRpc = newRpcWithMockedFailedCall(new byte[] {1, 2, 3}, EstimatePath.CONSTANT_CALL);

    JsonRpcInternalException e = assertThrows(JsonRpcInternalException.class,
        () -> mockRpc.getCall(newCallArgs(), "latest"));
    Assert.assertEquals(REVERT_MSG, e.getMessage());
  }

  @Test
  public void testEstimateGasAppendsRevertReason() throws Exception {
    byte[] revertData = ByteArray.fromHexString(ERROR_REVERT_HEX);

    mockRpc = newRpcWithMockedFailedCall(revertData, EstimatePath.CONSTANT_CALL);
    CommonParameter.getInstance().setEstimateEnergy(false);

    JsonRpcInternalException e = assertThrows(JsonRpcInternalException.class,
        () -> mockRpc.estimateGas(newCallArgs()));
    Assert.assertEquals(REVERT_MSG + ": not enough input value", e.getMessage());
  }

  @Test
  public void testEstimateGasSkipsRevertReasonForEmptyData() throws Exception {
    mockRpc = newRpcWithMockedFailedCall(new byte[0], EstimatePath.CONSTANT_CALL);
    CommonParameter.getInstance().setEstimateEnergy(false);

    JsonRpcInternalException e = assertThrows(JsonRpcInternalException.class,
        () -> mockRpc.estimateGas(newCallArgs()));
    Assert.assertEquals(REVERT_MSG, e.getMessage());
  }

  @Test
  public void testEstimateGasWithEstimateEnergyAppendsRevertReason() throws Exception {
    byte[] revertData = ByteArray.fromHexString(ERROR_REVERT_HEX);

    mockRpc = newRpcWithMockedFailedCall(revertData, EstimatePath.ESTIMATE_ENERGY);
    CommonParameter.getInstance().setEstimateEnergy(true);

    JsonRpcInternalException e = assertThrows(JsonRpcInternalException.class,
        () -> mockRpc.estimateGas(newCallArgs()));
    Assert.assertEquals(REVERT_MSG + ": not enough input value", e.getMessage());
  }

  @Test
  public void testEstimateGasWithEstimateEnergySkipsRevertReasonForShortData() throws Exception {
    mockRpc = newRpcWithMockedFailedCall(new byte[] {1, 2, 3}, EstimatePath.ESTIMATE_ENERGY);
    CommonParameter.getInstance().setEstimateEnergy(true);

    JsonRpcInternalException e = assertThrows(JsonRpcInternalException.class,
        () -> mockRpc.estimateGas(newCallArgs()));
    Assert.assertEquals(REVERT_MSG, e.getMessage());
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

  private static TronJsonRpcImpl newRpcWithMockedFailedCall(byte[] resData, EstimatePath path)
      throws Exception {
    Wallet mockWallet = mock(Wallet.class);
    Manager mockManager = mock(Manager.class);
    NodeInfoService mockNodeInfo = mock(NodeInfoService.class);

    when(mockWallet.createTransactionCapsule(any(), any()))
        .thenReturn(new TransactionCapsule(Protocol.Transaction.newBuilder().build()));
    when(mockWallet.getContract(any())).thenReturn(SmartContract.getDefaultInstance());

    if (path == EstimatePath.ESTIMATE_ENERGY) {
      when(mockWallet.estimateEnergy(any(), any(), any(), any(), any()))
          .thenAnswer(invocation -> {
            TransactionExtention.Builder extBuilder = invocation.getArgument(2);
            Return.Builder retBuilder = invocation.getArgument(3);
            EstimateEnergyMessage.Builder estimateBuilder = invocation.getArgument(4);
            extBuilder.addConstantResult(ByteString.copyFrom(resData));
            retBuilder.setMessage(ByteString.copyFromUtf8(REVERT_MSG));
            estimateBuilder.setResult(retBuilder);
            return Protocol.Transaction.newBuilder()
                .addRet(Protocol.Transaction.Result.newBuilder()
                    .setRet(Protocol.Transaction.Result.code.FAILED))
                .build();
          });
    } else {
      when(mockWallet.triggerConstantContract(any(), any(), any(), any()))
          .thenAnswer(invocation -> {
            TransactionExtention.Builder extBuilder = invocation.getArgument(2);
            Return.Builder retBuilder = invocation.getArgument(3);
            extBuilder.addConstantResult(ByteString.copyFrom(resData));
            retBuilder.setMessage(ByteString.copyFromUtf8(REVERT_MSG));
            return Protocol.Transaction.newBuilder()
                .addRet(Protocol.Transaction.Result.newBuilder()
                    .setRet(Protocol.Transaction.Result.code.FAILED))
                .build();
          });
    }

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
