package stest.tron.wallet.dailybuild.multisign;

import static org.tron.api.GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;

@Slf4j
public class MultiSign02 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private final String witnessKey001 = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] witnessAddress001 = PublicMethed.getFinalAddress(witnessKey001);

  private long multiSignFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.multiSignFee");
  private long updateAccountPermissionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.updateAccountPermissionFee");

  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] ownerAddress = ecKey1.getAddress();
  private String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  private ECKey ecKey2 = new ECKey(Utils.getRandom());
  private byte[] normalAddr001 = ecKey2.getAddress();
  private String normalKey001 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  private ECKey tmpEcKey01 = new ECKey(Utils.getRandom());
  private byte[] tmpAddr01 = tmpEcKey01.getAddress();
  private String tmpKey01 = ByteArray.toHexString(tmpEcKey01.getPrivKeyBytes());

  private ECKey tmpEcKey02 = new ECKey(Utils.getRandom());
  private byte[] tmpAddr02 = tmpEcKey02.getAddress();
  private String tmpKey02 = ByteArray.toHexString(tmpEcKey02.getPrivKeyBytes());

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  private String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  private String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");


  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {

    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = true, description = "Owner threshold in exception condition")
  public void testOwnerThreshold01() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    Assert.assertTrue(PublicMethed.sendcoin(ownerAddress, 1_000_000, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    ownerPermissionKeys.add(ownerKey);

    // threshold = Integer.MIN_VALUE
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\",\"threshold\":-2147483648,"
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    GrpcAPI.Return response = PublicMethed.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : permission's"
            + " threshold should be greater than 0",
        response.getMessage().toStringUtf8());

    // threshold = 0
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\",\"threshold\":0,"
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    response = PublicMethed.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : permission's"
            + " threshold should be greater than 0",
        response.getMessage().toStringUtf8());

    // threshold = -1
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\",\"threshold\":-1,"
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    response = PublicMethed.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : permission's"
            + " threshold should be greater than 0",
        response.getMessage().toStringUtf8());

    // threshold = long.min
    logger.info("** update owner and active permission to two address");
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\","
            + "\"threshold\":-9223372036854775808,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    response = PublicMethed.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : permission's"
            + " threshold should be greater than 0",
        response.getMessage().toStringUtf8());

    // threshold = long.min - 1000020
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\","
            + "\"threshold\":-9223372036855775828,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    boolean ret = false;
    try {
      PublicMethed.accountPermissionUpdateForResponse(
          accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    } catch (NumberFormatException e) {
      logger.info("NumberFormatException !");
      ret = true;
    }
    Assert.assertTrue(ret);

    // threshold = long.min - 1
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\","
            + "\"threshold\":-9223372036854775809,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":2}"
            + "]}]}";

    ret = false;
    try {
      PublicMethed.accountPermissionUpdateForResponse(
          accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    } catch (NumberFormatException e) {
      logger.info("NumberFormatException !");
      ret = true;
    }
    Assert.assertTrue(ret);

    // threshold = "12a"
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\","
            + "\"threshold\":\"12a\",\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    ret = false;
    try {
      PublicMethed.accountPermissionUpdateForResponse(
          accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    } catch (NumberFormatException e) {
      logger.info("NumberFormatException !");
      ret = true;
    }
    Assert.assertTrue(ret);

    // threshold = ""
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\","
            + "\"threshold\":\"\",\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    ret = false;
    try {
      PublicMethed.accountPermissionUpdateForResponse(
          accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    } catch (NumberFormatException e) {
      logger.info("NumberFormatException !");
      ret = true;
    }
    Assert.assertTrue(ret);

    // theshold =
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\","
            + "\"threshold\":,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    ret = false;
    try {
      PublicMethed.accountPermissionUpdateForResponse(
          accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    } catch (com.alibaba.fastjson.JSONException e) {
      logger.info("JSONException !");
      ret = true;
    }
    Assert.assertTrue(ret);

    // theshold = null
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\","
            + "\"threshold\":" + null + ",\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    ret = false;
    try {
      PublicMethed.accountPermissionUpdateForResponse(
          accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    } catch (NumberFormatException e) {
      logger.info("NumberFormatException !");
      ret = true;
    }
    Assert.assertTrue(ret);

    // theshold = 1.1
    accountPermissionJson = "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\","
        + "\"threshold\": 1.1,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":3,"
        + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
        + "\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":3},"
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}"
        + "]}]}";

    ret = false;
    try {
      PublicMethed.accountPermissionUpdateForResponse(
          accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    } catch (NumberFormatException e) {
      logger.info("NumberFormatException !");
      ret = true;
    }
    Assert.assertTrue(ret);

    // theshold = Long.MAX_VALUE  < sum(weight)
    accountPermissionJson = "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\","
        + "\"threshold\": 9223372036854775807,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2147483647},"
        + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001)
        + "\",\"weight\":9223372036854775807},"
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":3,"
        + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
        + "\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":3},"
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}"
        + "]}]}";

    response = PublicMethed.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : long overflow",
        response.getMessage().toStringUtf8());

    Long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);

    Assert.assertEquals(balanceBefore, balanceAfter);
  }

  @Test(enabled = true, description = "Owner threshold is 1")
  public void testOwnerThreshold02() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee * 2;
    Assert.assertTrue(PublicMethed.sendcoin(ownerAddress, needCoin, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\","
            + "\"threshold\": 1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(tmpKey02);

    Assert.assertEquals(2,
        PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));

    logger.info("** trigger a permission transaction");
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    Long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);

    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);
  }

  @Test(enabled = true, description = "Owner threshold is more than sum of keys' weight")
  public void testOwnerThreshold03() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    Assert.assertTrue(PublicMethed.sendcoin(ownerAddress, 1_000_000, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    // theshold = Long.MAX_VALUE  > sum(weight)
    String accountPermissionJson = "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\","
        + "\"threshold\": 9223372036854775807,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":214748364},"
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
        + "\",\"weight\":214748364}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":3,"
        + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
        + "\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":3},"
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}"
        + "]}]}";

    GrpcAPI.Return response = PublicMethed.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : sum of all key's weight should not"
            + " be less than threshold in permission Owner",
        response.getMessage().toStringUtf8());

    // theshold = Integer.MAX_VALUE  > sum(weight)
    accountPermissionJson = "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\","
        + "\"threshold\": 2147483647,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":214748364},"
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
        + "\",\"weight\":214748364}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":3,"
        + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
        + "\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":3},"
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}"
        + "]}]}";

    response = PublicMethed.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : sum of all key's weight "
            + "should not be less than threshold in permission Owner",
        response.getMessage().toStringUtf8());

    // theshold = Long.MAX_VALUE + 1  > sum(weight)
    accountPermissionJson = "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\","
        + "\"threshold\": 9223372036854775808,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
        + "\",\"weight\":9223372036854775806},"
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":3,"
        + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
        + "\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":3},"
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}"
        + "]}]}";

    boolean ret = false;
    try {
      PublicMethed.accountPermissionUpdateForResponse(
          accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    } catch (NumberFormatException e) {
      logger.info("NumberFormatException !");
      ret = true;
    }
    Assert.assertTrue(ret);

    Long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);

    Assert.assertEquals(balanceBefore, balanceAfter);
  }

  @Test(enabled = true, description = "Owner threshold is Long.MAX_VALUE and equal sum(weight)")
  public void testOwnerThreshold04() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee * 2 + multiSignFee;
    Assert.assertTrue(PublicMethed.sendcoin(ownerAddress, needCoin, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson = "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\","
        + "\"threshold\": 9223372036854775807,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
        + "\",\"weight\":9223372036854775806}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":3,"
        + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
        + "\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":3},"
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}"
        + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(
        PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));

    logger.info("** trigger a permission transaction");
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    Long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);

    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);
  }

  @Test(enabled = true, description = "Owner threshold is Integer.MAX_VALUE")
  public void testOwnerThreshold05() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee * 2;
    Assert.assertTrue(PublicMethed.sendcoin(ownerAddress, needCoin, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson = "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\","
        + "\"threshold\": 2147483647,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2147483647},"
        + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001)
        + "\",\"weight\":2147483647},"
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
        + "\",\"weight\":2147483647}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":3,"
        + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
        + "\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":3},"
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}"
        + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.add(tmpKey02);
    ownerPermissionKeys.add(witnessKey001);

    Assert.assertEquals(2,
        PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(3, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));

    logger.info("** trigger a permission transaction");
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    Long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);

    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);
  }

  @AfterMethod
  public void aftertest() {
    PublicMethed.freedResource(ownerAddress, ownerKey, fromAddress, blockingStubFull);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

}
