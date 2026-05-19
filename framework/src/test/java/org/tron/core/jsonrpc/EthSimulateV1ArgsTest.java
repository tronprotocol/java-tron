package org.tron.core.jsonrpc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Test;
import org.tron.core.Wallet;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.Manager;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidParamsException;
import org.tron.core.services.NodeInfoService;
import org.tron.core.services.jsonrpc.TronJsonRpcImpl;
import org.tron.core.services.jsonrpc.types.CallArguments;
import org.tron.core.services.jsonrpc.types.SimulateBlock;
import org.tron.core.services.jsonrpc.types.SimulateBlockResult;
import org.tron.core.services.jsonrpc.types.SimulateCallResult;
import org.tron.core.services.jsonrpc.types.SimulateV1Args;
import org.tron.protos.Protocol;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;

public class EthSimulateV1ArgsTest {

  private TronJsonRpcImpl rpc;

  @After
  public void tearDown() throws Exception {
    if (rpc != null) {
      rpc.close();
      rpc = null;
    }
  }

  @Test
  public void rejectsNullBlockStateCalls() throws Exception {
    rpc = newRpc();
    SimulateV1Args args = new SimulateV1Args();
    JsonRpcInvalidParamsException e = assertThrows(JsonRpcInvalidParamsException.class,
        () -> rpc.ethSimulateV1(args, "latest"));
    assertTrue(e.getMessage().contains("blockStateCalls"));
  }

  @Test
  public void rejectsEmptyBlockStateCalls() throws Exception {
    rpc = newRpc();
    SimulateV1Args args = new SimulateV1Args();
    args.setBlockStateCalls(new ArrayList<>());
    JsonRpcInvalidParamsException e = assertThrows(JsonRpcInvalidParamsException.class,
        () -> rpc.ethSimulateV1(args, "latest"));
    assertTrue(e.getMessage().contains("single-block"));
  }

  @Test
  public void rejectsMultipleBlocks() throws Exception {
    rpc = newRpc();
    SimulateV1Args args = new SimulateV1Args();
    args.setBlockStateCalls(Arrays.asList(emptyBlock(), emptyBlock()));
    JsonRpcInvalidParamsException e = assertThrows(JsonRpcInvalidParamsException.class,
        () -> rpc.ethSimulateV1(args, "latest"));
    assertTrue(e.getMessage().contains("single-block"));
  }

  @Test
  public void rejectsBlockOverrides() throws Exception {
    rpc = newRpc();
    SimulateBlock block = emptyBlock();
    block.setBlockOverrides(new ObjectMapper().createObjectNode());
    SimulateV1Args args = wrap(block);
    JsonRpcInvalidParamsException e = assertThrows(JsonRpcInvalidParamsException.class,
        () -> rpc.ethSimulateV1(args, "latest"));
    assertTrue(e.getMessage().contains("blockOverrides"));
  }

  @Test
  public void rejectsStateOverrides() throws Exception {
    rpc = newRpc();
    SimulateBlock block = emptyBlock();
    block.setStateOverrides(new ObjectMapper().createObjectNode());
    SimulateV1Args args = wrap(block);
    JsonRpcInvalidParamsException e = assertThrows(JsonRpcInvalidParamsException.class,
        () -> rpc.ethSimulateV1(args, "latest"));
    assertTrue(e.getMessage().contains("stateOverrides"));
  }

  @Test
  public void rejectsUnknownBlockField() throws Exception {
    rpc = newRpc();
    String json = "{\"calls\":[],\"futureFeature\":42}";
    SimulateBlock block = new ObjectMapper().readValue(json, SimulateBlock.class);
    SimulateV1Args args = wrap(block);
    JsonRpcInvalidParamsException e = assertThrows(JsonRpcInvalidParamsException.class,
        () -> rpc.ethSimulateV1(args, "latest"));
    assertTrue(e.getMessage().contains("unknown"));
    assertTrue(e.getMessage().contains("futureFeature"));
  }

  @Test
  public void rejectsTooManyCalls() throws Exception {
    rpc = newRpc();
    SimulateBlock block = emptyBlock();
    List<CallArguments> calls = new ArrayList<>();
    for (int i = 0; i < 33; i++) {
      calls.add(new CallArguments());
    }
    block.setCalls(calls);
    SimulateV1Args args = wrap(block);
    JsonRpcInvalidParamsException e = assertThrows(JsonRpcInvalidParamsException.class,
        () -> rpc.ethSimulateV1(args, "latest"));
    assertTrue(e.getMessage().contains("too many"));
  }

  @Test
  public void rejectsHexBlockNumberTag() throws Exception {
    rpc = newRpc();
    SimulateV1Args args = wrap(emptyBlock());
    JsonRpcInvalidParamsException e = assertThrows(JsonRpcInvalidParamsException.class,
        () -> rpc.ethSimulateV1(args, "0x12345"));
    assertTrue(e.getMessage().contains("latest"));
  }

  @Test
  public void rejectsEarliestTag() throws Exception {
    rpc = newRpc();
    SimulateV1Args args = wrap(emptyBlock());
    assertThrows(JsonRpcInvalidParamsException.class,
        () -> rpc.ethSimulateV1(args, "earliest"));
  }

  @Test
  public void simulateBlockResultJsonRoundTrip() throws Exception {
    SimulateBlockResult result = new SimulateBlockResult();
    result.setNumber("0x1");
    result.setHash("0xabc");
    SimulateCallResult c0 = new SimulateCallResult();
    c0.setStatus("0x1");
    c0.setReturnData("0xdeadbeef");
    SimulateCallResult c1 = new SimulateCallResult();
    c1.setStatus("0x0");
    c1.setReturnData("0x");
    result.setCalls(Arrays.asList(c0, c1));
    result.setTransactions(new Object[] {"0xhash0", "0xhash1"});

    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(result);
    ObjectNode parsed = (ObjectNode) mapper.readTree(json);

    ArrayNode calls = (ArrayNode) parsed.get("calls");
    assertNotNull("calls field must be present", calls);
    assertEquals(2, calls.size());
    assertEquals("0x1", calls.get(0).get("status").asText());
    assertEquals("0xdeadbeef", calls.get(0).get("returnData").asText());

    ArrayNode txs = (ArrayNode) parsed.get("transactions");
    assertNotNull("transactions field must be present", txs);
    assertEquals(2, txs.size());
    assertEquals("0xhash0", txs.get(0).asText());
  }

  private static SimulateBlock emptyBlock() {
    SimulateBlock block = new SimulateBlock();
    block.setCalls(new ArrayList<>());
    return block;
  }

  private static SimulateV1Args wrap(SimulateBlock block) {
    SimulateV1Args args = new SimulateV1Args();
    args.setBlockStateCalls(Collections.singletonList(block));
    return args;
  }

  private static TronJsonRpcImpl newRpc() throws Exception {
    Wallet mockWallet = mock(Wallet.class);
    Manager mockManager = mock(Manager.class);
    NodeInfoService mockNodeInfo = mock(NodeInfoService.class);
    when(mockWallet.createTransactionCapsule(any(), any()))
        .thenReturn(new TransactionCapsule(Protocol.Transaction.newBuilder().build()));
    when(mockWallet.getContract(any())).thenReturn(SmartContract.getDefaultInstance());
    TronJsonRpcImpl rpc = new TronJsonRpcImpl(mockNodeInfo, mockWallet);
    rpc.setManager(mockManager);
    return rpc;
  }
}
