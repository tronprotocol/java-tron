package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;

@Slf4j
public class HttpTestMutiSign001 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String manager1Key = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] manager1Address = PublicMethed.getFinalAddress(manager1Key);
  private final String manager2Key = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] manager2Address = PublicMethed.getFinalAddress(manager2Key);
  private final String manager3Key = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] manager3Address = PublicMethed.getFinalAddress(manager3Key);
  private final String manager4Key = Configuration.getByPath("testng.conf")
      .getString("witness.key2");
  private final byte[] manager4Address = PublicMethed.getFinalAddress(manager4Key);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] ownerAddress = ecKey1.getAddress();
  String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] hexTestAddress = ecKey2.getAddress();
  String hexTestKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  String[] permissionKeyString;
  Long amount = 1000000000L;
  JsonArray keys = new JsonArray();
  JsonArray activeKeys = new JsonArray();
  JsonObject manager1Wight = new JsonObject();
  JsonObject manager2Wight = new JsonObject();
  JsonObject manager3Wight = new JsonObject();
  JsonObject manager4Wight = new JsonObject();
  JsonObject ownerObject = new JsonObject();
  JsonObject witnessObject = new JsonObject();
  JsonObject activeObject = new JsonObject();
  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(1);
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Account Permission Up Date by http")
  public void test1AccountPermissionUpDate() {
    PublicMethed.printAddress(ownerKey);
    response = HttpMethed.sendCoin(httpnode, fromAddress, ownerAddress, amount, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    manager1Wight.addProperty("address", ByteArray.toHexString(manager1Address));
    manager1Wight.addProperty("weight", 1);

    logger.info(manager1Wight.toString());
    manager2Wight.addProperty("address", ByteArray.toHexString(manager2Address));
    manager2Wight.addProperty("weight", 1);

    logger.info(manager2Wight.toString());

    keys.add(manager1Wight);
    keys.add(manager2Wight);

    ownerObject.addProperty("type", 0);
    ownerObject.addProperty("permission_name", "owner");
    ownerObject.addProperty("threshold", 2);
    ownerObject.add("keys", keys);

    manager3Wight.addProperty("address", ByteArray.toHexString(manager3Address));
    manager3Wight.addProperty("weight", 1);

    logger.info(manager3Wight.toString());
    manager4Wight.addProperty("address", ByteArray.toHexString(manager4Address));
    manager4Wight.addProperty("weight", 1);

    logger.info(manager4Wight.toString());

    activeKeys.add(manager3Wight);
    activeKeys.add(manager4Wight);

    activeObject.addProperty("type", 2);
    activeObject.addProperty("permission_name", "active0");
    activeObject.addProperty("threshold", 2);
    activeObject.addProperty("operations",
        "7fff1fc0037e0000000000000000000000000000000000000000000000000000");
    activeObject.add("keys", activeKeys);

    response = HttpMethed
        .accountPermissionUpdate(httpnode, ownerAddress, ownerObject, witnessObject, activeObject,
            ownerKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Add transaction sign by http with permission id")
  public void test2AddTransactionSign() {

    HttpMethed.waitToProduceOneBlock(httpnode);
    permissionKeyString = new String[2];
    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;

    String[] permissionKeyActive = new String[2];
    permissionKeyActive[0] = manager3Key;
    permissionKeyActive[1] = manager4Key;

    response = HttpMethed
        .sendCoin(httpnode, ownerAddress, fromAddress, 10L, 0, permissionKeyString);
    Assert.assertTrue(HttpMethed.verificationResult(response));

    response = HttpMethed
        .sendCoin(httpnode, ownerAddress, fromAddress, 10L, 2, permissionKeyString);
    Assert.assertFalse(HttpMethed.verificationResult(response));

    logger.info("start permission id 2");
    response = HttpMethed
        .sendCoin(httpnode, ownerAddress, fromAddress, 12L, 2, permissionKeyActive);
    Assert.assertTrue(HttpMethed.verificationResult(response));

    response = HttpMethed
        .sendCoin(httpnode, ownerAddress, fromAddress, 12L, 0, permissionKeyActive);
    Assert.assertFalse(HttpMethed.verificationResult(response));

    response = HttpMethed
        .sendCoin(httpnode, ownerAddress, fromAddress, 11L, 1, permissionKeyActive);
    Assert.assertFalse(HttpMethed.verificationResult(response));

    response = HttpMethed
        .sendCoin(httpnode, ownerAddress, fromAddress, 11L, 3, permissionKeyString);
    Assert.assertFalse(HttpMethed.verificationResult(response));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Add broadcasthex http interface to "
      + "broadcast hex transaction string")
  public void test3Broadcasthex() {
    PublicMethed.printAddress(hexTestKey);
    String transactionHex = PublicMethed
        .sendcoinGetTransactionHex(hexTestAddress, 1000L, fromAddress, testKey002,
            blockingStubFull);

    //Wrong type of hex
    response = HttpMethed.broadcasthex(httpnode, transactionHex);
    Assert.assertTrue(HttpMethed.verificationResult(response));

    String wrongTransactionHex = transactionHex + "wrong";
    response = HttpMethed.broadcasthex(httpnode, wrongTransactionHex);
    logger.info("transaction wrong:");
    Assert.assertFalse(HttpMethed.verificationResult(response));

    //SingleSign for broadcastHex
    response = HttpMethed.broadcasthex(httpnode, transactionHex);
    Assert.assertFalse(HttpMethed.verificationResult(response));

    //Mutisign for broadcastHex
    String mutiSignTransactionHex = PublicMethedForMutiSign
        .sendcoinGetTransactionHex(hexTestAddress, 999L, ownerAddress, ownerKey, blockingStubFull,
            permissionKeyString);
    response = HttpMethed.broadcasthex(httpnode, mutiSignTransactionHex);
    Assert.assertTrue(HttpMethed.verificationResult(response));

    //Hex is null
    response = HttpMethed.broadcasthex(httpnode, "");
    Assert.assertFalse(HttpMethed.verificationResult(response));


  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.disConnect();
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

  }
}
