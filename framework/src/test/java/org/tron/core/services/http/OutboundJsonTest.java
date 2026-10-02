package org.tron.core.services.http;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.nio.charset.StandardCharsets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.TransactionIdList;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.StringUtil;
import org.tron.core.Wallet;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.args.Args;
import org.tron.json.JSONArray;
import org.tron.json.JSONObject;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.SmartContractOuterClass.CreateSmartContract;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;

public class OutboundJsonTest {

  private static final String OWNER_ADDRESS = "41c076305e35aea1fe45a772fcaaab8a36e87bdb55";
  private static final ByteString OWNER =
      ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));
  private static final long TIMESTAMP = 1_700_000_000_000L;

  private long savedHttpMaxMessageSize;

  @Before
  public void setUp() {
    JsonFormat.clearInt64AsString();
    savedHttpMaxMessageSize = Args.getInstance().getHttpMaxMessageSize();
    Args.getInstance().setHttpMaxMessageSize(1_024);
  }

  @After
  public void tearDown() {
    JsonFormat.clearInt64AsString();
    Args.getInstance().setHttpMaxMessageSize(savedHttpMaxMessageSize);
  }

  @Test
  public void testPrintTransactionWithAbi() {
    int entryCount = 2;
    SmartContract.ABI.Builder abi = SmartContract.ABI.newBuilder();
    for (int i = 0; i < entryCount; i++) {
      abi.addEntrys(SmartContract.ABI.Entry.newBuilder().setName("f" + i)
          .setType(SmartContract.ABI.Entry.EntryType.Function));
    }
    ByteString bytecode = ByteString.copyFrom(new byte[] {0});
    CreateSmartContract create = CreateSmartContract.newBuilder().setOwnerAddress(OWNER)
        .setNewContract(SmartContract.newBuilder().setOriginAddress(OWNER).setAbi(abi)
            .setBytecode(bytecode))
        .build();
    Transaction transaction = transaction(ContractType.CreateSmartContract, Any.pack(create));
    JSONObject output = Util.printTransactionToJSON(transaction, false);
    JSONObject contract = output.getJSONObject("raw_data").getJSONArray("contract")
        .getJSONObject(0);
    JSONObject value = contract.getJSONObject("parameter").getJSONObject("value");
    JSONArray entries = value.getJSONObject("new_contract").getJSONObject("abi")
        .getJSONArray("entrys");
    assertEquals(entryCount, entries.size());
    assertEquals("f0", entries.getJSONObject(0).getString("name"));
    assertEquals("f1", entries.getJSONObject(entryCount - 1).getString("name"));
    assertEquals("Function", entries.getJSONObject(0).getString("type"));
    assertEquals("CreateSmartContract", contract.getString("type"));
    assertEquals(OWNER_ADDRESS, value.getString("owner_address"));
    byte[] contractAddress = Util.generateContractAddress(transaction, OWNER.toByteArray());
    assertEquals(ByteArray.toHexString(contractAddress),
        output.getString("contract_address"));
    assertTransactionIdentity(transaction, output);

    JSONObject serialized = JSONObject.outboundParseObject(output.toJSONString());
    assertEquals(output, serialized);
  }

  @Test
  public void testPrintTransactionIdList() {
    int count = 3;
    TransactionIdList.Builder builder = TransactionIdList.newBuilder();
    for (int i = 0; i < count; i++) {
      builder.addTxId("tx" + i);
    }
    TransactionIdList list = builder.build();
    JSONObject output = JSONObject.outboundParseObject(Util.printTransactionIdList(list, false));
    JSONArray ids = output.getJSONArray("txId");
    assertEquals(count, ids.size());
    assertEquals("tx0", ids.getString(0));
    assertEquals("tx" + (count - 1), ids.getString(count - 1));
  }

  @Test
  public void testPrintTransactionPreservesVisibleAndInt64Formatting() {
    long callValue = 9_007_199_254_740_993L;
    TriggerSmartContract trigger = TriggerSmartContract.newBuilder().setOwnerAddress(OWNER)
        .setContractAddress(OWNER).setCallValue(callValue).build();
    Transaction transaction = transaction(ContractType.TriggerSmartContract, Any.pack(trigger));
    for (boolean visible : new boolean[] {false, true}) {
      for (boolean int64AsString : new boolean[] {false, true}) {
        JsonFormat.setInt64AsString(int64AsString);
        JSONObject output = Util.printTransactionToJSON(transaction, visible);
        JSONObject rawData = output.getJSONObject("raw_data");
        JSONObject contract = rawData.getJSONArray("contract").getJSONObject(0);
        JSONObject value = contract.getJSONObject("parameter").getJSONObject("value");
        String address = visible ? StringUtil.encode58Check(OWNER.toByteArray()) : OWNER_ADDRESS;
        assertEquals(address, value.getString("owner_address"));
        assertEquals(address, value.getString("contract_address"));
        assertEquals("TriggerSmartContract", contract.getString("type"));
        assertEquals(int64AsString, value.unwrap().get("call_value").isTextual());
        assertEquals(int64AsString, rawData.unwrap().get("timestamp").isTextual());
        assertEquals(callValue, value.getLongValue("call_value"));
        assertEquals(TIMESTAMP, rawData.getLongValue("timestamp"));
        assertTransactionIdentity(transaction, output);
      }
    }
  }

  @Test
  public void testPrintTransactionFeePreservesReceipt() {
    String input = "{\"receipt\":{\"energy_fee\":100,\"net_fee\":20}}";
    JSONObject output = JSONObject.parseObject(Util.printTransactionFee(input));
    assertEquals(JSONObject.parseObject(input).getJSONObject("receipt"),
        output.getJSONObject("Receipt"));
    assertEquals(100, output.getJSONObject("Receipt").getLongValue("energy_fee"));
    assertEquals(20, output.getJSONObject("Receipt").getLongValue("net_fee"));
  }

  @Test
  public void testGetContractPreservesAddressesForGetAndPost() throws Exception {
    SmartContract contract = SmartContract.newBuilder().setName("normal-contract")
        .setOriginAddress(OWNER).setContractAddress(OWNER).build();
    Wallet wallet = mock(Wallet.class);
    when(wallet.getContract(BytesMessage.newBuilder().setValue(OWNER).build()))
        .thenReturn(contract);
    GetContractServlet servlet = new GetContractServlet();
    ReflectionTestUtils.setField(servlet, "wallet", wallet);
    for (boolean visible : new boolean[] {false, true}) {
      String address = visible ? StringUtil.encode58Check(OWNER.toByteArray()) : OWNER_ADDRESS;
      MockHttpServletRequest get = new MockHttpServletRequest("GET", "/wallet/getcontract");
      get.setParameter("value", address);
      get.setParameter("visible", Boolean.toString(visible));
      MockHttpServletResponse getResponse = new MockHttpServletResponse();
      servlet.doGet(get, getResponse);
      assertContractResponse(getResponse, address);

      JSONObject body = new JSONObject();
      body.put("value", address);
      body.put("visible", visible);
      MockHttpServletRequest post = new MockHttpServletRequest("POST", "/wallet/getcontract");
      post.setContentType("application/json");
      post.setContent(body.toJSONString().getBytes(StandardCharsets.UTF_8));
      MockHttpServletResponse postResponse = new MockHttpServletResponse();
      servlet.doPost(post, postResponse);
      assertContractResponse(postResponse, address);
    }
  }

  private static void assertContractResponse(MockHttpServletResponse response, String address)
      throws Exception {
    assertEquals(200, response.getStatus());
    JSONObject body = JSONObject.parseObject(response.getContentAsString());
    assertEquals(response.getContentAsString(), "normal-contract", body.getString("name"));
    assertEquals(address, body.getString("origin_address"));
    assertEquals(address, body.getString("contract_address"));
  }

  private static Transaction transaction(ContractType type, Any parameter) {
    Contract contract = Contract.newBuilder().setType(type).setParameter(parameter).build();
    return Transaction.newBuilder().setRawData(Transaction.raw.newBuilder()
        .setTimestamp(TIMESTAMP).addContract(contract)).build();
  }

  private static void assertTransactionIdentity(Transaction transaction, JSONObject output) {
    assertEquals(ByteArray.toHexString(transaction.getRawData().toByteArray()),
        output.getString("raw_data_hex"));
    assertEquals(ByteArray.toHexString(new TransactionCapsule(transaction)
        .getTransactionId().getBytes()), output.getString("txID"));
  }
}
