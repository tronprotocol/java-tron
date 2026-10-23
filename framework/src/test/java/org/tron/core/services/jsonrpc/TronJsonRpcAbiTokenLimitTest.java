package org.tron.core.services.jsonrpc;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.TestConstants;
import org.tron.core.Wallet;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.db2.core.Chainbase.Cursor;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidParamsException;
import org.tron.core.services.NodeInfoService;
import org.tron.core.services.jsonrpc.TronJsonRpc.TransactionJson;
import org.tron.core.services.jsonrpc.types.BuildArguments;
import org.tron.json.JSONObject;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.SmartContractOuterClass.CreateSmartContract;

public class TronJsonRpcAbiTokenLimitTest {

  private Wallet wallet;
  private TronJsonRpcImpl jsonRpc;

  @BeforeClass
  public static void initArgs() {
    Args.setParam(new String[]{}, TestConstants.TEST_CONF);
  }

  @AfterClass
  public static void clearArgs() {
    Args.clearParam();
  }

  @Before
  public void setUp() {
    wallet = mock(Wallet.class);
    when(wallet.getCursor()).thenReturn(Cursor.HEAD);
    jsonRpc = new TronJsonRpcImpl(mock(NodeInfoService.class), wallet, mock(Manager.class));
  }

  @After
  public void tearDown() throws Exception {
    if (jsonRpc != null) {
      jsonRpc.close();
    }
  }

  @Test
  public void testBuildTransactionRejectsAbiOverTokenLimit() throws Exception {
    String abi = "[" + String.join(",", Collections.nCopies(50_000, "{}")) + "]";
    String json = "{\"abi\":\"" + abi + "\"}";
    assertTrue(json.getBytes(UTF_8).length < 4 * 1024 * 1024);
    assertEquals(abi, JSONObject.parseObject(json).getString("abi"));
    BuildArguments args = createArguments(abi);

    JsonRpcInvalidParamsException exception = assertThrows(JsonRpcInvalidParamsException.class,
        () -> jsonRpc.buildTransaction(args));

    assertEquals("invalid abi", exception.getMessage());
    verify(wallet, never()).createTransactionCapsule(any(), any());
  }

  @Test
  public void testBuildTransactionAcceptsSmallAbi() throws Exception {
    Transaction transaction = Transaction.newBuilder()
        .setRawData(Transaction.raw.newBuilder().addContract(Transaction.Contract.newBuilder()))
        .build();
    when(wallet.createTransactionCapsule(any(), eq(ContractType.CreateSmartContract)))
        .thenReturn(new TransactionCapsule(transaction));
    BuildArguments args = createArguments("[{\"name\":\"test\",\"type\":\"function\"}]");

    TransactionJson result = jsonRpc.buildTransaction(args);

    assertNotNull(result.getTransaction());
    assertTrue(result.getTransaction().containsKey("txID"));
    verify(wallet).createTransactionCapsule(
        argThat(contract -> contract instanceof CreateSmartContract
            && ((CreateSmartContract) contract).getNewContract().getAbi().getEntrysCount() == 1
            && ((CreateSmartContract) contract).getNewContract().getAbi().getEntrys(0)
                .getName().equals("test")),
        eq(ContractType.CreateSmartContract));
  }

  private static BuildArguments createArguments(String abi) {
    BuildArguments args = new BuildArguments();
    args.setFrom("0x99357684bc659f5166046b56c95a0e99f1265cd1");
    args.setData("0x6000");
    args.setAbi(abi);
    return args;
  }
}
