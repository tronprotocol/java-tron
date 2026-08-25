package org.tron.core.services.http;

import com.google.protobuf.ByteString;
import java.security.InvalidParameterException;
import java.util.Arrays;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.api.GrpcAPI.TransactionApprovedList;
import org.tron.api.GrpcAPI.TransactionSignWeight;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.utils.TransactionUtil;
import org.tron.json.JSONObject;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.contract.SmartContractOuterClass.CreateSmartContract;

public class UtilTest extends BaseTest {

  private static final String OWNER_ADDRESS;

  @Resource
  private Wallet wallet;
  @Resource
  private TransactionUtil transactionUtil;

  static {
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "c076305e35aea1fe45a772fcaaab8a36e87bdb55";
    Args.setParam(new String[] {"-d", dbPath()}, TestConstants.TEST_CONF);
  }

  @Before
  public void setUp() {
    byte[] owner = ByteArray.fromHexString(OWNER_ADDRESS);
    AccountCapsule ownerCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(owner),
            Protocol.AccountType.Normal,
            10_000_000_000L);
    ownerCapsule.setFrozenForBandwidth(1000000L, 1000000L);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
  }

  @Test
  public void testPackTransactionWithInvalidType() {

    String strTransaction = "{\n"
        + "    \"visible\": false,\n"
        + "    \"signature\": [\n"
        + "        \"5c23bddabccd3e4e5ebdf7d2f21dc58af9f88e0b99620374c5354e0dd9efb3a436167d95b70d2"
        + "d825180bf90bc84525acb13a203f209afd5d397316f6b2c387c01\"\n"
        + "    ],\n"
        + "    \"txID\": \"fc33817936b06e50d4b6f1797e62f52d69af6c0da580a607241a9c03a48e390e\",\n"
        + "    \"raw_data\": {\n"
        + "        \"contract\": [\n"
        + "            {\n"
        + "                \"parameter\": {\n"
        + "                    \"value\": {\n"
        + "                      \"amount\": 10,\n"
        + "                      \"owner_address\":\"41c076305e35aea1fe45a772fcaaab8a36e87bdb55\","
        + "                      \"to_address\": \"415624c12e308b03a1a6b21d9b86e3942fac1ab92b\"\n"
        + "                    },\n"
        + "                    \"type_url\": \"type.googleapis.com/protocol.TransferContract\"\n"
        + "                },\n"
        + "                \"type\": \"TransferContract11111\"\n"
        + "            }\n"
        + "        ],\n"
        + "        \"ref_block_bytes\": \"d8ed\",\n"
        + "        \"ref_block_hash\": \"2e066c3259e756f5\",\n"
        + "        \"expiration\": 1651906644000,\n"
        + "        \"timestamp\": 1651906586162\n"
        + "    },\n"
        + "    \"raw_data_hex\": \"0a02d8ed22082e066c3259e756f540a090bcea89305a65080112610a2d747970"
        + "652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e5472616e73666572436f6e74726163741230"
        + "0a1541c076305e35aea1fe45a772fcaaab8a36e87bdb551215415624c12e308b03a1a6b21d9b86e3942fac1a"
        + "b92b180a70b2ccb8ea8930\"\n"
        + "}";
    Transaction transaction = Util.packTransaction(strTransaction, false);
    TransactionApprovedList transactionApprovedList =
        wallet.getTransactionApprovedList(transaction);
    Assert.assertEquals("Invalid transaction: no valid contract",
        transactionApprovedList.getResult().getMessage());

    TransactionSignWeight txSignWeight = transactionUtil.getTransactionSignWeight(transaction);
    Assert.assertEquals("Invalid transaction: no valid contract",
        txSignWeight.getResult().getMessage());


    strTransaction = "{\n"
        + "    \"visible\": false,\n"
        + "    \"signature\": [\n"
        + "        \"5c23bddabccd3e4e5ebdf7d2f21dc58af9f88e0b99620374c5354e0dd9efb3a436167d95b70d2d"
        + "825180bf90bc84525acb13a203f209afd5d397316f6b2c387c01\"\n"
        + "    ],\n"
        + "    \"txID\": \"fc33817936b06e50d4b6f1797e62f52d69af6c0da580a607241a9c03a48e390e\",\n"
        + "    \"raw_data\": {\n"
        + "        \"contract\": [\n"
        + "            {\n"
        + "                \"parameter\": {\n"
        + "                    \"value\": {\n"
        + "                      \"amount\": 10,\n"
        + "                      \"owner_address\":\"41c076305e35aea1fe45a772fcaaab8a36e87bdb55\","
        + "                      \"to_address\": \"415624c12e308b03a1a6b21d9b86e3942fac1ab92b\"\n"
        + "                    },\n"
        + "                    \"type_url\": \"type.googleapis.com/protocol.TransferContract\"\n"
        + "                }\n"
        + "            }\n"
        + "        ],\n"
        + "        \"ref_block_bytes\": \"d8ed\",\n"
        + "        \"ref_block_hash\": \"2e066c3259e756f5\",\n"
        + "        \"expiration\": 1651906644000,\n"
        + "        \"timestamp\": 1651906586162\n"
        + "    },\n"
        + "    \"raw_data_hex\": \"0a02d8ed22082e066c3259e756f540a090bcea89305a65080112610a2d747970"
        + "652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e5472616e73666572436f6e74726163741230"
        + "0a1541c076305e35aea1fe45a772fcaaab8a36e87bdb551215415624c12e308b03a1a6b21d9b86e3942fac1a"
        + "b92b180a70b2ccb8ea8930\"\n"
        + "}";
    transaction = Util.packTransaction(strTransaction, false);
    transactionApprovedList = wallet.getTransactionApprovedList(transaction);
    Assert.assertEquals("Invalid transaction: no valid contract",
        transactionApprovedList.getResult().getMessage());

    txSignWeight = transactionUtil.getTransactionSignWeight(transaction);
    Assert.assertEquals("Invalid transaction: no valid contract",
        txSignWeight.getResult().getMessage());
  }

  @Test
  public void testCheckBodySizeUsesHttpLimit() throws Exception {
    long originalHttpMax = Args.getInstance().getHttpMaxMessageSize();
    int originalRpcMax = Args.getInstance().getMaxMessageSize();
    try {
      // set httpMaxMessageSize larger than maxMessageSize
      Args.getInstance().setHttpMaxMessageSize(200);
      Args.getInstance().setMaxMessageSize(100);

      String withinHttpLimit = new String(new char[150]).replace('\0', 'a');
      // should pass: 150 < httpMaxMessageSize(200), even though > maxMessageSize(100)
      Util.checkBodySize(withinHttpLimit);

      String exceedsHttpLimit = new String(new char[201]).replace('\0', 'b');
      Exception e = Assert.assertThrows(Exception.class,
          () -> Util.checkBodySize(exceedsHttpLimit));
      Assert.assertTrue(e.getMessage().contains("200"));
    } finally {
      Args.getInstance().setHttpMaxMessageSize(originalHttpMax);
      Args.getInstance().setMaxMessageSize(originalRpcMax);
    }
  }

  @Test
  public void testPackTransaction() {
    String strTransaction = "{\n"
        + "    \"visible\": false,\n"
        + "    \"signature\": [\n"
        + "        \"5c23bddabccd3e4e5ebdf7d2f21dc58af9f88e0b99620374c5354e0dd9efb3a436167d95b70d2"
        + "d825180bf90bc84525acb13a203f209afd5d397316f6b2c387c01\"\n"
        + "    ],\n"
        + "    \"txID\": \"fc33817936b06e50d4b6f1797e62f52d69af6c0da580a607241a9c03a48e390e\",\n"
        + "    \"raw_data\": {\n"
        + "        \"contract\": [\n"
        + "            {\n"
        + "                \"parameter\": {\n"
        + "                    \"value\": {\n"
        + "                      \"amount\": 10,\n"
        + "                      \"owner_address\":\"41c076305e35aea1fe45a772fcaaab8a36e87bdb55\","
        + "                      \"to_address\": \"415624c12e308b03a1a6b21d9b86e3942fac1ab92b\"\n"
        + "                    },\n"
        + "                    \"type_url\": \"type.googleapis.com/protocol.TransferContract\"\n"
        + "                },\n"
        + "                \"type\": \"TransferContract\"\n"
        + "            }\n"
        + "        ],\n"
        + "        \"ref_block_bytes\": \"d8ed\",\n"
        + "        \"ref_block_hash\": \"2e066c3259e756f5\",\n"
        + "        \"expiration\": 1651906644000,\n"
        + "        \"timestamp\": 1651906586162\n"
        + "    },\n"
        + "    \"raw_data_hex\": \"0a02d8ed22082e066c3259e756f540a090bcea89305a65080112610a2d747970"
        + "652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e5472616e73666572436f6e74726163741230"
        + "0a1541c076305e35aea1fe45a772fcaaab8a36e87bdb551215415624c12e308b03a1a6b21d9b86e3942fac1a"
        + "b92b180a70b2ccb8ea8930\"\n"
        + "}";
    Transaction transaction = Util.packTransaction(strTransaction, false);
    TransactionSignWeight txSignWeight = transactionUtil.getTransactionSignWeight(transaction);
    Assert.assertNotNull(txSignWeight);
  }

  @Test
  public void testPackCreateSmartContractOmitsNullAbiOutputs() throws Exception {
    String strTransaction = "{\n"
        + "    \"visible\": false,\n"
        + "    \"raw_data\": {\n"
        + "        \"contract\": [\n"
        + "            {\n"
        + "                \"parameter\": {\n"
        + "                    \"value\": {\n"
        + "                      \"owner_address\":\"41c076305e35aea1fe45a772fcaaab8a36e87bdb55\","
        + "                      \"new_contract\": {\n"
        + "                        \"origin_address\":"
        + " \"41c076305e35aea1fe45a772fcaaab8a36e87bdb55\","
        + "                        \"name\":\"TestContract\","
        + "                        \"abi\": {\n"
        + "                          \"entrys\": [\n"
        + "                            {\"inputs\":[],\"name\":\"test\","
        + " \"outputs\":null,\"type\":\"function\"}\n"
        + "                          ]\n"
        + "                        }\n"
        + "                      }\n"
        + "                    },\n"
        + "                    \"type_url\":"
        + " \"type.googleapis.com/protocol.CreateSmartContract\"\n"
        + "                },\n"
        + "                \"type\": \"CreateSmartContract\"\n"
        + "            }\n"
        + "        ],\n"
        + "        \"ref_block_bytes\": \"d8ed\",\n"
        + "        \"ref_block_hash\": \"2e066c3259e756f5\",\n"
        + "        \"expiration\": 1651906644000,\n"
        + "        \"timestamp\": 1651906586162\n"
        + "    }\n"
        + "}";

    Transaction transaction = Util.packTransaction(strTransaction, false);
    Assert.assertNotNull(transaction);
    Assert.assertEquals(1, transaction.getRawData().getContractCount());
    CreateSmartContract contract = transaction.getRawData().getContract(0)
        .getParameter().unpack(CreateSmartContract.class);
    Assert.assertEquals(1, contract.getNewContract().getAbi().getEntrysCount());
    Assert.assertEquals(0, contract.getNewContract().getAbi().getEntrys(0).getOutputsCount());
  }

  private Transaction buildTransferTransaction() {
    String strTransaction = "{\n"
        + "    \"visible\": false,\n"
        + "    \"txID\": \"fc33817936b06e50d4b6f1797e62f52d69af6c0da580a607241a9c03a48e390e\",\n"
        + "    \"raw_data\": {\n"
        + "        \"contract\": [\n"
        + "            {\n"
        + "                \"parameter\": {\n"
        + "                    \"value\": {\n"
        + "                      \"amount\": 10,\n"
        + "                      \"owner_address\":\"41c076305e35aea1fe45a772fcaaab8a36e87bdb55\","
        + "                      \"to_address\": \"415624c12e308b03a1a6b21d9b86e3942fac1ab92b\"\n"
        + "                    },\n"
        + "                    \"type_url\": \"type.googleapis.com/protocol.TransferContract\"\n"
        + "                },\n"
        + "                \"type\": \"TransferContract\"\n"
        + "            }\n"
        + "        ],\n"
        + "        \"ref_block_bytes\": \"d8ed\",\n"
        + "        \"ref_block_hash\": \"2e066c3259e756f5\",\n"
        + "        \"expiration\": 1651906644000,\n"
        + "        \"timestamp\": 1651906586162\n"
        + "    },\n"
        + "    \"raw_data_hex\": \"0a02d8ed22082e066c3259e756f540a090bcea89305a65080112610a2d747970"
        + "652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e5472616e73666572436f6e74726163741230"
        + "0a1541c076305e35aea1fe45a772fcaaab8a36e87bdb551215415624c12e308b03a1a6b21d9b86e3942fac1a"
        + "b92b180a70b2ccb8ea8930\"\n"
        + "}";
    return Util.packTransaction(strTransaction, false);
  }

  private Transaction buildTooManySigsTransaction() {
    Transaction transaction = buildTransferTransaction();
    int totalSignNum = dbManager.getDynamicPropertiesStore().getTotalSignNum();
    ByteString dummySig = ByteString.copyFrom(new byte[65]);
    Transaction.Builder builder = transaction.toBuilder();
    for (int i = 0; i < totalSignNum + 1; i++) {
      builder.addSignature(dummySig);
    }
    return builder.build();
  }

  @Test
  public void testPrintApprovedListTooManySigsHttpPath() {
    Transaction transaction = buildTooManySigsTransaction();
    TransactionApprovedList reply = wallet.getTransactionApprovedList(transaction);
    Assert.assertEquals(TransactionApprovedList.Result.response_code.OTHER_ERROR,
        reply.getResult().getCode());
    // The early-return reply has no transaction; the HTTP print helper must not throw.
    String json = Util.printTransactionApprovedList(reply, false);
    JSONObject jsonObject = JSONObject.parseObject(json);
    Assert.assertNull(jsonObject.getJSONObject("transaction"));
    Assert.assertTrue(jsonObject.getJSONObject("result").getString("message")
        .contains("too many signatures"));
  }

  @Test
  public void testPrintSignWeightTooManySigsHttpPath() {
    Transaction transaction = buildTooManySigsTransaction();
    TransactionSignWeight reply = transactionUtil.getTransactionSignWeight(transaction);
    Assert.assertEquals(TransactionSignWeight.Result.response_code.OTHER_ERROR,
        reply.getResult().getCode());
    // The early-return reply has no transaction; the HTTP print helper must not throw.
    String json = Util.printTransactionSignWeight(reply, false);
    JSONObject jsonObject = JSONObject.parseObject(json);
    Assert.assertNull(jsonObject.getJSONObject("transaction"));
    Assert.assertTrue(jsonObject.getJSONObject("result").getString("message")
        .contains("too many signatures"));
  }

  /**
   * A container is not a number. It used to skip the length bound altogether and reach the
   * conversion as its full serialized form, bounded only by the shim's 65535-character fallback.
   */
  @Test
  public void testPermissionIdRejectsNonNumericTypes() {
    assertRejected("[" + StringUtils.repeat('9', 128) + "]");
    assertRejected("{\"a\":" + StringUtils.repeat('9', 128) + "}");
    assertRejected("true");
  }

  private Transaction applyPermissionId(String rawJsonValue) {
    JSONObject jsonObject =
        JSONObject.parseObject("{\"" + Util.PERMISSION_ID + "\":" + rawJsonValue + "}");
    return Util.setTransactionPermissionId(jsonObject, buildTransferTransaction());
  }

  private int permissionIdOf(Transaction transaction) {
    return transaction.getRawData().getContract(0).getPermissionId();
  }

  @Test
  public void testPermissionIdAcceptsLegacyExactIntegerRepresentations() {
    Assert.assertEquals(2, permissionIdOf(applyPermissionId("2")));
    Assert.assertEquals(2, permissionIdOf(applyPermissionId("\"2\"")));
    Assert.assertEquals(1, permissionIdOf(applyPermissionId("1.0")));
    Assert.assertEquals(100, permissionIdOf(applyPermissionId("1e2")));
    Assert.assertEquals(1, permissionIdOf(applyPermissionId("\"1.0\"")));
    Assert.assertEquals(1, permissionIdOf(applyPermissionId("\"1.\"")));
    Assert.assertEquals(1000, permissionIdOf(applyPermissionId("\"1,000\"")));
  }

  @Test
  public void testPermissionIdAcceptsMaximumLengthNumericString() {
    char[] digits = new char[64];
    Arrays.fill(digits, '0');
    digits[digits.length - 1] = '2';

    Assert.assertEquals(2, permissionIdOf(applyPermissionId("\"" + new String(digits) + "\"")));
  }

  /**
   * One character past the bound is refused, and so is a value far beyond it: the field is
   * bounded by its own length rather than by whatever the numeric conversion happens to survive.
   */
  @Test
  public void testPermissionIdRejectsStringsPastTheLengthBoundary() {
    assertRejected("\"" + StringUtils.repeat('0', 64) + "2\"");
    assertRejected("\"" + StringUtils.repeat('9', 10_000) + "\"");
  }

  /** Quoting must not decide which regime applies: the same digits unquoted are refused too. */
  @Test
  public void testPermissionIdRejectsUnquotedNumbersPastTheLengthBoundary() {
    assertRejected("9" + StringUtils.repeat('9', 64));
  }

  private void assertRejected(String rawJsonValue) {
    InvalidParameterException e = Assert.assertThrows(InvalidParameterException.class,
        () -> applyPermissionId(rawJsonValue));
    Assert.assertTrue(e.getMessage().contains(Util.PERMISSION_ID));
    Assert.assertFalse("the message must not echo the submitted value",
        e.getMessage().contains(rawJsonValue));
  }

  @Test
  public void testPermissionIdRejectsFraction() {
    assertRejected("1.9");
    assertRejected("2.999");
  }

  @Test
  public void testPermissionIdRejectsNonLegacyNumericStrings() {
    assertRejected("\"1e2\"");
    assertRejected("\"1E2\"");
    assertRejected("\".0\"");
  }

  @Test
  public void testPermissionIdRejectsIntOverflow() {
    assertRejected("4294967297");
    assertRejected("99999999999");
  }

  @Test
  public void testPermissionIdRejectsNonNumber() {
    assertRejected("\"abc\"");
    assertRejected("true");
    assertRejected("[1]");
  }

  @Test
  public void testPermissionIdRejectsExplicitNull() {
    InvalidParameterException e = Assert.assertThrows(InvalidParameterException.class,
        () -> applyPermissionId("null"));
    Assert.assertTrue(e.getMessage().contains(Util.PERMISSION_ID));
  }

  @Test
  public void testAbsentPermissionIdLeavesTransactionUnchanged() {
    JSONObject jsonObject = JSONObject.parseObject("{\"amount\":1}");
    Transaction transaction = buildTransferTransaction();
    Assert.assertEquals(0,
        permissionIdOf(Util.setTransactionPermissionId(jsonObject, transaction)));
  }

  @Test
  public void testPermissionIdNotPositiveLeavesTransactionUnchanged() {
    Assert.assertEquals(0, permissionIdOf(applyPermissionId("0")));
    Assert.assertEquals(0, permissionIdOf(applyPermissionId("-1")));
  }
}
