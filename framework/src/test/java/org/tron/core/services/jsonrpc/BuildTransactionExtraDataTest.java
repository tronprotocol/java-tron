package org.tron.core.services.jsonrpc;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Wallet;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.Manager;
import org.tron.core.db2.core.Chainbase.Cursor;
import org.tron.core.services.NodeInfoService;
import org.tron.core.services.jsonrpc.types.BuildArguments;
import org.tron.json.JSONObject;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.SmartContractOuterClass.CreateSmartContract;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;

public class BuildTransactionExtraDataTest {

  private final Wallet wallet = mock(Wallet.class);
  private final BuildArguments args = new BuildArguments();
  private TronJsonRpcImpl jsonRpc;

  @Before
  public void setUp() throws Exception {
    when(wallet.getCursor()).thenReturn(Cursor.HEAD);
    when(wallet.createTransactionCapsule(any(), any())).thenAnswer(invocation ->
        new TransactionCapsule(invocation.getArgument(0, Message.class),
            invocation.getArgument(1, ContractType.class)));
    jsonRpc = new TronJsonRpcImpl(mock(NodeInfoService.class), wallet, mock(Manager.class));
    args.setFrom("0x99357684bc659f5166046b56c95a0e99f1265cd1");
    args.setExtraData("616263");
  }

  @After
  public void tearDown() {
    jsonRpc.close();
  }

  @Test
  public void transferPreservesMemo() throws Exception {
    args.setTo("0x548794500882809695a8a687866e76d4271a1abc");
    args.setValue("0x1");
    assertEquals(ContractType.TransferContract, buildWithMemo().getType());
  }

  @Test
  public void deployPreservesMemoAndBytecode() throws Exception {
    args.setData("0x6000");
    CreateSmartContract deploy = buildWithMemo().getParameter().unpack(CreateSmartContract.class);
    assertEquals("6000",
        ByteArray.toHexString(deploy.getNewContract().getBytecode().toByteArray()));
  }

  @Test
  public void triggerPreservesMemoAndCallData() throws Exception {
    args.setTo("0x548794500882809695a8a687866e76d4271a1abc");
    args.setData("0x60fe47b1");
    when(wallet.getContract(any())).thenReturn(SmartContract.getDefaultInstance());
    when(wallet.triggerContract(any(), any(), any(), any())).thenAnswer(invocation ->
        invocation.getArgument(1, TransactionCapsule.class).getInstance());
    TriggerSmartContract call = buildWithMemo().getParameter().unpack(TriggerSmartContract.class);
    assertEquals("60fe47b1", ByteArray.toHexString(call.getData().toByteArray()));
  }

  private Transaction.Contract buildWithMemo() throws Exception {
    JSONObject tx = jsonRpc.buildTransaction(args).getTransaction();
    assertEquals("616263", tx.getJSONObject("raw_data").getString("data"));
    byte[] bytes = ByteArray.fromHexString(tx.getString("raw_data_hex"));
    Transaction.raw raw = Transaction.raw.parseFrom(bytes);
    assertEquals(ByteString.copyFromUtf8("abc"), raw.getData());
    assertEquals(ByteArray.toHexString(Sha256Hash.hash(bytes)), tx.getString("txID"));
    return raw.getContract(0);
  }
}
