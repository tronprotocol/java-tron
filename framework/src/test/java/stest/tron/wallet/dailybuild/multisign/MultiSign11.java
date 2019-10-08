package stest.tron.wallet.dailybuild.multisign;

import static org.tron.api.GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR;

import com.google.protobuf.ByteString;
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
public class MultiSign11 {

  private static final long now = System.currentTimeMillis();
  private static final long TotalSupply = 1000L;
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  private static ByteString assetAccountId = null;
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String witnessKey001 = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] witnessAddress001 = PublicMethed.getFinalAddress(witnessKey001);
  private final String contractTronDiceAddr = "TMYcx6eoRXnePKT1jVn25ZNeMNJ6828HWk";
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
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private byte[] transferTokenContractAddress = null;

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

    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = true, description = "Active address is witness")
  public void testActiveAddress01() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee + multiSignFee;

    Assert.assertTrue(PublicMethed
        .sendcoin(ownerAddress, needCoin + 1_000_000, fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();
    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(witnessKey001);
    activePermissionKeys.add(tmpKey02);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001)
            + "\",\"weight\":2147483647}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":4,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001)
            + "\",\"weight\":3}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}" + "]}]}";
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
            ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.add(witnessKey001);

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2,
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getOwnerPermission()
            .getKeysCount());

    PublicMethedForMutiSign.printPermissionList(
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList());

    System.out.printf(PublicMethedForMutiSign.printPermission(
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getOwnerPermission()));

    logger.info("** trigger a normal transaction");
    Assert.assertTrue(PublicMethedForMutiSign
        .sendcoinWithPermissionId(fromAddress, 1_000000, ownerAddress, 2, ownerKey,
            blockingStubFull,
            activePermissionKeys.toArray(new String[activePermissionKeys.size()])));
    Long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin + 1000000);
  }

  @Test(enabled = true, description = "Active address is contract address")
  public void testActiveAddress02() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee;

    Assert.assertTrue(PublicMethed
        .sendcoin(ownerAddress, needCoin + 1_000_000, fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();
    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(tmpKey02);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + contractTronDiceAddr + "\",\"weight\":1}," + "{\"address\":\""
            + PublicMethed.getAddressString(testKey002) + "\",\"weight\":2147483647}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + contractTronDiceAddr + "\",\"weight\":3},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":3}"
            + "]}]}";
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
            ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.add(testKey002);

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(3,
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getOwnerPermission()
            .getKeysCount());

    PublicMethedForMutiSign.printPermissionList(
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList());

    System.out.printf(PublicMethedForMutiSign.printPermission(
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getOwnerPermission()));

    logger.info("** trigger a normal transaction");
    Assert.assertTrue(PublicMethedForMutiSign
        .sendcoinWithPermissionId(fromAddress, 1_000000, ownerAddress, 2, ownerKey,
            blockingStubFull,
            activePermissionKeys.toArray(new String[activePermissionKeys.size()])));
    Long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin + 1000000);
  }

  @Test(enabled = true, description = "Active address is inactive address")
  public void testActiveAddress03() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee + multiSignFee;

    Assert.assertTrue(PublicMethed
        .sendcoin(ownerAddress, needCoin + 1_000_000, fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();
    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(witnessKey001);
    activePermissionKeys.add(tmpKey02);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":2147483647}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":5,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001)
            + "\",\"weight\":3}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":3}" + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
            ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.add(tmpKey01);

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2,
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getOwnerPermission()
            .getKeysCount());

    PublicMethedForMutiSign.printPermissionList(
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList());

    System.out.printf(PublicMethedForMutiSign.printPermission(
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getOwnerPermission()));

    logger.info("** trigger a normal transaction");
    Assert.assertTrue(PublicMethedForMutiSign
        .sendcoinWithPermissionId(fromAddress, 1_000000, ownerAddress, 2, ownerKey,
            blockingStubFull,
            activePermissionKeys.toArray(new String[activePermissionKeys.size()])));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin + 1000000);
  }

  @Test(enabled = true, description = "Active address is owner address")
  public void testActiveAddress04() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee;

    Assert.assertTrue(PublicMethed
        .sendcoin(ownerAddress, needCoin + 1_000_000, fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();
    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":2147483647}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":3}" + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
            ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertEquals(1, PublicMethedForMutiSign.getActivePermissionKeyCount(
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2,
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getOwnerPermission()
            .getKeysCount());

    PublicMethedForMutiSign.printPermissionList(
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList());

    System.out.printf(PublicMethedForMutiSign.printPermission(
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getOwnerPermission()));

    logger.info("** trigger a normal transaction");
    Assert.assertTrue(PublicMethedForMutiSign
        .sendcoinWithPermissionId(fromAddress, 1_000000, ownerAddress, 2, ownerKey,
            blockingStubFull,
            activePermissionKeys.toArray(new String[activePermissionKeys.size()])));
    Long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin + 1000000);
  }

  @Test(enabled = true, description = "Active address is exception condition")
  public void testActiveAddress05() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    Assert.assertTrue(
        PublicMethed.sendcoin(ownerAddress, 1_000_000, fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    // address = same address more than once
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":2147483647}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":3}," + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":3}" + "]}]}";

    GrpcAPI.Return response = PublicMethed
        .accountPermissionUpdateForResponse(accountPermissionJson, ownerAddress, ownerKey,
            blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals(
        "contract validate error : address should be distinct" + " in permission Active",
        response.getMessage().toStringUtf8());

    // address = not exist
    String fakeAddress = "THph9K2M2nLvkianrMGswRhz5hjSA9fuH1";
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":2147483647}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + fakeAddress + "\",\"weight\":3},"
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":3}"
            + "]}]}";

    boolean ret = false;
    try {
      PublicMethed.accountPermissionUpdateForResponse(accountPermissionJson, ownerAddress, ownerKey,
          blockingStubFull);
    } catch (NullPointerException e) {
      logger.info("NullPointerException !");
      ret = true;
    }
    Assert.assertTrue(ret);

    // address = long address
    fakeAddress = "TR3FAbhiSeP7kSh39RjGYpwCqfMDHPMhX4d121";

    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":2147483647}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + fakeAddress + "\",\"weight\":3},"
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":3}"
            + "]}]}";

    ret = false;
    try {
      PublicMethed.accountPermissionUpdateForResponse(accountPermissionJson, ownerAddress, ownerKey,
          blockingStubFull);
    } catch (NullPointerException e) {
      logger.info("NullPointerException !");
      ret = true;
    }
    Assert.assertTrue(ret);

    // address = short address
    fakeAddress = "THph9K2M2nLvkianrMGswRhz5hj";

    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":2147483647}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + fakeAddress + "\",\"weight\":3},"
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":3}"
            + "]}]}";

    ret = false;
    try {
      PublicMethed.accountPermissionUpdateForResponse(accountPermissionJson, ownerAddress, ownerKey,
          blockingStubFull);
    } catch (NullPointerException e) {
      logger.info("NullPointerException !");
      ret = true;
    }
    Assert.assertTrue(ret);

    // address =
    fakeAddress = "";
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":2147483647}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + fakeAddress + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01) + "\",\"weight\":3}"
            + "]}]}";
    ret = false;
    try {
      PublicMethed.accountPermissionUpdateForResponse(accountPermissionJson, fakeAddress.getBytes(),
          ownerKey, blockingStubFull);
    } catch (NullPointerException e) {
      logger.info("NullPointerException!");
      ret = true;
    }

    Assert.assertTrue(ret);

    // address = null
    fakeAddress = null;
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":2147483647}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + fakeAddress + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01) + "\",\"weight\":3}"
            + "]}]}";
    ret = false;
    try {
      PublicMethed.accountPermissionUpdateForResponse(accountPermissionJson, fakeAddress.getBytes(),
          ownerKey, blockingStubFull);
    } catch (NullPointerException e) {
      logger.info("NullPointerException!");
      ret = true;
    }

    Assert.assertTrue(ret);

    // address = null
    fakeAddress = "1.1";
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":2147483647}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + fakeAddress + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01) + "\",\"weight\":3}"
            + "]}]}";
    ret = false;
    try {
      PublicMethed.accountPermissionUpdateForResponse(accountPermissionJson, fakeAddress.getBytes(),
          ownerKey, blockingStubFull);
    } catch (IllegalArgumentException e) {
      logger.info("IllegalArgumentException!");
      ret = true;
    }
    Assert.assertTrue(ret);
    Long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore, balanceAfter);


  }

  @Test(enabled = true, description = "Active address count is 5")
  public void testActiveAddress06() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee + multiSignFee;

    Assert.assertTrue(PublicMethed
        .sendcoin(ownerAddress, needCoin + 1_000_000, fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();
    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(witnessKey001);
    activePermissionKeys.add(ownerKey);
    activePermissionKeys.add(tmpKey01);
    activePermissionKeys.add(tmpKey02);
    activePermissionKeys.add(testKey002);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":2147483647}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\""
            + ",\"threshold\":10,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":3}" + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
            ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertEquals(5, PublicMethedForMutiSign.getActivePermissionKeyCount(
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2,
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getOwnerPermission()
            .getKeysCount());

    PublicMethedForMutiSign.printPermissionList(
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList());

    System.out.printf(PublicMethedForMutiSign.printPermission(
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getOwnerPermission()));

    ownerPermissionKeys.add(tmpKey01);

    logger.info("** trigger a normal transaction");
    Assert.assertTrue(PublicMethedForMutiSign
        .sendcoinWithPermissionId(fromAddress, 1_000000, ownerAddress, 2, ownerKey,
            blockingStubFull,
            activePermissionKeys.toArray(new String[activePermissionKeys.size()])));
    Long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin + 1000000);


  }

  @Test(enabled = true, description = "Active address count is more than 5")
  public void testActiveAddress07() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    Assert.assertTrue(
        PublicMethed.sendcoin(ownerAddress, 1_000_000, fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":2147483647}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + contractTronDiceAddr + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01) + "\",\"weight\":3}"
            + "]}]}";
    GrpcAPI.Return response = PublicMethed
        .accountPermissionUpdateForResponse(accountPermissionJson, ownerAddress, ownerKey,
            blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals(
        "contract validate error : number of keys in permission should" + " not be greater than 5",
        response.getMessage().toStringUtf8());
    Long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore, balanceAfter);
  }

  @Test(enabled = true, description = "Active address count is 0")
  public void testActiveAddress08() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    Assert.assertTrue(
        PublicMethed.sendcoin(ownerAddress, 1_000_000, fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":2147483647}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[]}]}";
    GrpcAPI.Return response = PublicMethed
        .accountPermissionUpdateForResponse(accountPermissionJson, ownerAddress, ownerKey,
            blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : key's count should be greater than 0",
        response.getMessage().toStringUtf8());
    Long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore, balanceAfter);
  }

  @Test(enabled = true, description = "Active address count is 1")
  public void testActiveAddress09() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee;

    Assert.assertTrue(PublicMethed
        .sendcoin(ownerAddress, needCoin + 1_000_000, fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();
    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(tmpKey01);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001)
            + "\",\"weight\":2147483647}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":3}" + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
            ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertEquals(1, PublicMethedForMutiSign.getActivePermissionKeyCount(
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2,
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getOwnerPermission()
            .getKeysCount());

    PublicMethedForMutiSign.printPermissionList(
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList());

    System.out.printf(PublicMethedForMutiSign.printPermission(
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getOwnerPermission()));

    ownerPermissionKeys.add(witnessKey001);

    logger.info("** trigger a normal transaction");
    Assert.assertTrue(PublicMethedForMutiSign
        .sendcoinWithPermissionId(fromAddress, 1_000000, ownerAddress, 2, ownerKey,
            blockingStubFull,
            activePermissionKeys.toArray(new String[activePermissionKeys.size()])));
    Long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin + 1000000);
  }

  @Test(enabled = true, description = "Active permission count is 8")
  public void testActiveAddress10() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee + multiSignFee;

    Assert.assertTrue(PublicMethed
        .sendcoin(ownerAddress, needCoin + 1_000_000, fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();
    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(witnessKey001);
    activePermissionKeys.add(ownerKey);
    activePermissionKeys.add(tmpKey01);
    activePermissionKeys.add(tmpKey02);
    activePermissionKeys.add(testKey002);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":2147483647}]}," + "\"active_permissions\":["
            + "{\"type\":2,\"permission_name\":\"active0\",\"threshold\":3,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":3}" + "]},"
            + "{\"type\":2,\"permission_name\":\"active1\",\"threshold\":5,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":3}" + "]},"
            + "{\"type\":2,\"permission_name\":\"active2\",\"threshold\":7,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":3}" + "]},"
            + "{\"type\":2,\"permission_name\":\"active3\",\"threshold\":9,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":3}" + "]},"
            + "{\"type\":2,\"permission_name\":\"active4\",\"threshold\":10,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":3}" + "]},"
            + "{\"type\":2,\"permission_name\":\"active5\",\"threshold\":11,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":3}" + "]},"
            + "{\"type\":2,\"permission_name\":\"active6\",\"threshold\":10,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":3}" + "]},"
            + "{\"type\":2,\"permission_name\":\"active7\",\"threshold\":11,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":3}" + "]}]}";
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
            ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertEquals(40, PublicMethedForMutiSign.getActivePermissionKeyCount(
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2,
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getOwnerPermission()
            .getKeysCount());

    PublicMethedForMutiSign.printPermissionList(
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList());

    System.out.printf(PublicMethedForMutiSign.printPermission(
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getOwnerPermission()));

    ownerPermissionKeys.add(tmpKey01);

    logger.info("** trigger a normal transaction");
    Assert.assertTrue(PublicMethedForMutiSign
        .sendcoinWithPermissionId(fromAddress, 1_000000, ownerAddress, 9, ownerKey,
            blockingStubFull,
            activePermissionKeys.toArray(new String[activePermissionKeys.size()])));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin + 1000000);
  }

  @Test(enabled = true, description = "Active permission count is 9")
  public void testActiveAddress11() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    Assert.assertTrue(
        PublicMethed.sendcoin(ownerAddress, 1_000_000, fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":2147483647}]}," + "\"active_permissions\":["
            + "{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":3}" + "]},"
            + "{\"type\":2,\"permission_name\":\"active1\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":3}" + "]},"
            + "{\"type\":2,\"permission_name\":\"active2\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":3}" + "]},"
            + "{\"type\":2,\"permission_name\":\"active3\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":3}" + "]},"
            + "{\"type\":2,\"permission_name\":\"active4\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":3}" + "]},"
            + "{\"type\":2,\"permission_name\":\"active5\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":3}" + "]},"
            + "{\"type\":2,\"permission_name\":\"active6\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":3}" + "]},"
            + "{\"type\":2,\"permission_name\":\"active7\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":3}" + "]},"
            + "{\"type\":2,\"permission_name\":\"active8\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":3}" + "]}]}";

    GrpcAPI.Return response = PublicMethed
        .accountPermissionUpdateForResponse(accountPermissionJson, ownerAddress, ownerKey,
            blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : active permission is too many",
        response.getMessage().toStringUtf8());
    Long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore, balanceAfter);
  }

  @Test(enabled = true, description = "Active permission count is 8, "
      + "sum of weight is less than threshold")
  public void testActiveAddress12() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    Assert.assertTrue(
        PublicMethed.sendcoin(ownerAddress, 1_000_000, fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":2147483647}]}," + "\"active_permissions\":["
            + "{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":3}" + "]},"
            + "{\"type\":2,\"permission_name\":\"active1\",\"threshold\":9223372036854775807,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":3}" + "]},"
            + "{\"type\":2,\"permission_name\":\"active2\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":3}" + "]},"
            + "{\"type\":2,\"permission_name\":\"active3\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":3}" + "]},"
            + "{\"type\":2,\"permission_name\":\"active4\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":3}" + "]},"
            + "{\"type\":2,\"permission_name\":\"active5\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":3}" + "]},"
            + "{\"type\":2,\"permission_name\":\"active6\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":3}" + "]},"
            + "{\"type\":2,\"permission_name\":\"active7\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":2}," + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey01)
            + "\",\"weight\":3}" + "]}]}";

    GrpcAPI.Return response = PublicMethed
        .accountPermissionUpdateForResponse(accountPermissionJson, ownerAddress, ownerKey,
            blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : sum of all key's weight should"
        + " not be less than threshold in permission Active", response.getMessage().toStringUtf8());
    Long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore, balanceAfter);
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
