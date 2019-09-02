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
public class MultiSign01 {

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

  @Test(enabled = true, description = "Owner permission_name is owner")
  public void testOwnerName01() {
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

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    List<String> ownerPermissionKeys = new ArrayList<>();

    ownerPermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
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
    ownerPermissionKeys.add(witnessKey001);

    Assert.assertEquals(2,
        PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
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

  @Test(enabled = true, description = "Owner permission_name is owner1")
  public void testOwnerName02() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    final byte[] ownerAddress = ecKey1.getAddress();
    final String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
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

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson = "{\"owner_permission\":{\"type\":0,"
        + "\"permission_name\":\"owner1\",\"threshold\":1,\"keys\":["
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

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

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

  @Test(enabled = true, description = "Owner permission_name is string \"123\"")
  public void testOwnerName03() {
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

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"123\",\"threshold\":1,\"keys\":["
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

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

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
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);

    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);

  }

  @Test(enabled = true, description = "Owner permission_name is string \"\"")
  public void testOwnerName04() {
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

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\",\"threshold\":1,\"keys\":["
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

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

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
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);

    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);
  }

  @Test(enabled = true, description = "Owner permission_name is null")
  public void testOwnerName05() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    Assert.assertTrue(PublicMethed.sendcoin(ownerAddress, 1_000000, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);
    // null
    String accountPermissionJson = "{\"owner_permission\":{\"type\":0,\"permission_name\":" + null
        + ",\"threshold\":1,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
        + "\",\"weight\":1}]},"
        + "\"witness_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\""
        + "keys\":[{\"address\":\"" + PublicMethed.getAddressString(witnessKey001)
        + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
        + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
        + "\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
        + "]}]}";
    boolean ret = false;
    try {
      PublicMethed
          .accountPermissionUpdateForResponse(accountPermissionJson,
              ownerAddress, ownerKey, blockingStubFull);
    } catch (NullPointerException e) {
      logger.info("Expected NullPointerException!");
      ret = true;
    }
    Assert.assertTrue(ret);
    Long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);

    Assert.assertEquals(balanceBefore, balanceAfter);

  }

  @Test(enabled = true, description = "Owner without permission_name")
  public void testOwnerName06() {
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

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"threshold\":1,\"keys\":["
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

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

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

  @Test(enabled = true, description = "Owner permission_name is numbers")
  public void testOwnerName07() {
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
        "{\"owner_permission\":{\"type\":0,\"permission_name\":123,\"threshold\":1,\"keys\":["
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

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

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

  @Test(enabled = true, description = "Owner permission_name length is 32")
  public void testOwnerName08() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    long needCoin = updateAccountPermissionFee * 2;

    Assert.assertTrue(PublicMethed.sendcoin(ownerAddress, needCoin + 1000000, fromAddress,
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
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"0.1\",\"threshold\":1,\"keys\":["
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

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

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

  @Test(enabled = true, description = "Owner permission_name length is 32")
  public void testOwnerName09() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    long needCoin = updateAccountPermissionFee * 2;

    Assert.assertTrue(PublicMethed.sendcoin(ownerAddress, needCoin + 1000000, fromAddress,
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
        "{\"owner_permission\":{\"type\":0,"
            + "\"permission_name\":\"abcdefghijklmnopqrstuvwxyzabcdef\",\"threshold\":1,\"keys\":["
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

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

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
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);

    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);

  }


  @Test(enabled = true, description = "Owner permission_name length is 33")
  public void testOwnerName10() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    Assert.assertTrue(PublicMethed.sendcoin(ownerAddress, 1000000, fromAddress,
        testKey002, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,"
            + "\"permission_name\":\"abcdefghijklmnopqrstuvwxyzabcdefg\",\"threshold\":1,\"keys\":["
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
    Assert.assertEquals("contract validate error : permission's name is too long",
        response.getMessage().toStringUtf8());

    Long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
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
