package stest.tron.wallet.dailybuild.multisign;

import static org.tron.api.GrpcAPI.TransactionSignWeight.Result.response_code.ENOUGH_PERMISSION;

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
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.GrpcAPI.TransactionSignWeight;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.AccountContract.AccountPermissionUpdateContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;

@Slf4j
public class MultiSign24 {

  private static final String AVAILABLE_OPERATION
      = "7fff1fc0037e0000000000000000000000000000000000000000000000000000";
  private static final String DEFAULT_OPERATION
      = "7fff1fc0033e0000000000000000000000000000000000000000000000000000";
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

  @Test(enabled = true, description = "Broadcast multi sign normal transaction")
  public void test01BroadcastMultiSignNormalTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee * 2 + multiSignFee * 2;

    Assert.assertTrue(PublicMethed.sendcoin(ownerAddress, needCoin + 1_000_000, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed
        .freezeBalanceForReceiver(fromAddress, 100000000000L, 0, 0,
            ByteString.copyFrom(ownerAddress),
            testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();
    ownerPermissionKeys.add(ownerKey);
    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(ownerKey);

    String accountPermissionJson = "{\"owner_permission\":{\"type\":0,"
        + "\"permission_name\":\"owner1\",\"threshold\":2,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
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
    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(witnessKey001);
    activePermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));

    logger.info("** trigger a normal transaction");
    Transaction transaction = PublicMethedForMutiSign
        .sendcoin2(fromAddress, 1000_000, ownerAddress, ownerKey, blockingStubFull);

    Transaction transaction1 = PublicMethedForMutiSign
        .addTransactionSignWithPermissionId(transaction, tmpKey02, 0, blockingStubFull);

    Transaction transaction2 = PublicMethedForMutiSign.addTransactionSignWithPermissionId(
        transaction1, testKey002, 0, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction2.toByteArray()));

    TransactionSignWeight txWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("TransactionSignWeight info : " + txWeight);

    Assert.assertTrue(PublicMethedForMutiSign.broadcastTransaction(transaction2, blockingStubFull));

    PublicMethedForMutiSign
        .recoverAccountPermission(ownerKey, ownerPermissionKeys, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    txWeight = PublicMethedForMutiSign.getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("TransactionSignWeight info : " + txWeight);

    Long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin + 1000000);
    PublicMethed
        .unFreezeBalance(fromAddress, testKey002, 0, ownerAddress, blockingStubFull);

  }

  @Test(enabled = true, description = "Broadcast multi sign permission transaction")
  public void test02BroadcastMultiSignPermissionTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee * 2 + multiSignFee;

    Assert.assertTrue(PublicMethed.sendcoin(ownerAddress, needCoin, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(ownerKey);

    Integer[] ints = {ContractType.AccountPermissionUpdateContract_VALUE};
    String operations = PublicMethedForMutiSign.getOperations(ints);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\","
            + "\"threshold\":5,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":3}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":3,"
            + "\"operations\":\"" + operations + "\",\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(tmpKey02);

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

    Transaction transaction = PublicMethedForMutiSign.accountPermissionUpdateWithoutSign(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()]));

    Transaction transaction1 = PublicMethedForMutiSign
        .addTransactionSignWithPermissionId(transaction, testKey002, 0, blockingStubFull);

    Transaction transaction2 = PublicMethedForMutiSign
        .addTransactionSignWithPermissionId(transaction1, ownerKey, 0, blockingStubFull);

    TransactionSignWeight txWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("TransactionSignWeight info : " + txWeight);

    Assert.assertTrue(PublicMethedForMutiSign.broadcastTransaction(transaction2, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);
  }

  @Test(enabled = true, description = "Broadcast single sign normal transaction")
  public void test03BroadcastSingleSignNormalTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee;

    Assert.assertTrue(PublicMethed.sendcoin(ownerAddress, needCoin + 1_000_000, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(ownerKey);

    Integer[] ints = {ContractType.TransferContract_VALUE};
    String operations = PublicMethedForMutiSign.getOperations(ints);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\","
            + "\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":3}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"" + operations + "\",\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(tmpKey02);

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

    logger.info("** trigger a normal transaction");
    Transaction transaction = PublicMethedForMutiSign
        .sendcoin2(fromAddress, 1000_000, ownerAddress, ownerKey, blockingStubFull);

    Transaction transaction1 = PublicMethedForMutiSign
        .addTransactionSignWithPermissionId(transaction, testKey002, 0, blockingStubFull);

    TransactionSignWeight txWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("TransactionSignWeight info : " + txWeight);

    Assert.assertTrue(PublicMethedForMutiSign.broadcastTransaction(transaction1, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin + 1000000);

  }

  @Test(enabled = true, description = "Broadcast not sign permission transaction")
  public void test04BroadcastNotSignPermissionTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.sendcoin(ownerAddress, needCoin, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\","
            + "\"threshold\":5,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":3}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":3,"
            + "\"operations\":\"" + AVAILABLE_OPERATION + "\",\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(tmpKey02);

    Assert.assertEquals(2,
        PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission());

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

    Transaction transaction = PublicMethedForMutiSign.accountPermissionUpdateWithoutSign(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()]));

    TransactionSignWeight txWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction, blockingStubFull);
    logger.info("TransactionSignWeight info : " + txWeight);

    Assert.assertFalse(PublicMethedForMutiSign.broadcastTransaction(transaction, blockingStubFull));
    Long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);
  }

  @Test(enabled = true, description = "Broadcast not complete multi sign normal transaction")
  public void test05BroadcastMultiSignNotCompletePermissionTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee;

    Assert.assertTrue(PublicMethed.sendcoin(ownerAddress, needCoin + 1_000_000, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\","
            + "\"threshold\":5,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":3}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":3,"
            + "\"operations\":\"" + AVAILABLE_OPERATION + "\",\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(tmpKey02);

    Assert.assertEquals(2,
        PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission());

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

    Transaction transaction = PublicMethedForMutiSign.accountPermissionUpdateWithoutSign(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()]));

    Transaction transaction1 = PublicMethedForMutiSign
        .addTransactionSignWithPermissionId(transaction, testKey002, 0, blockingStubFull);

    TransactionSignWeight txWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("TransactionSignWeight info : " + txWeight);

    Assert
        .assertFalse(PublicMethedForMutiSign.broadcastTransaction(transaction1, blockingStubFull));

    Long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);

  }

  @Test(enabled = true, description = "Broadcast failed transaction")
  public void test06BroadcastSignFailedTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee;

    Assert.assertTrue(PublicMethed.sendcoin(ownerAddress, needCoin + 1_000_000, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\","
            + "\"threshold\":5,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":3}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"" + AVAILABLE_OPERATION + "\",\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertEquals(2,
        PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission());

    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(tmpKey02);

    logger.info("** trigger a normal transaction");
    Transaction transaction = PublicMethedForMutiSign
        .sendcoin2(fromAddress, 1000_000, ownerAddress, ownerKey, blockingStubFull);

    Transaction transaction1 = PublicMethedForMutiSign
        .addTransactionSignWithPermissionId(transaction, tmpKey02, 0, blockingStubFull);

    TransactionSignWeight txWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("TransactionSignWeight info : " + txWeight);

    Assert
        .assertFalse(PublicMethedForMutiSign.broadcastTransaction(transaction1, blockingStubFull));
    Long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);
  }

  @Test(enabled = true, description = "Broadcast timeout normal transaction")
  public void test07BroadcastTimeoutTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee;

    Assert.assertTrue(PublicMethed.sendcoin(ownerAddress, needCoin + 1_000_000, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    PublicMethed.printAddress(ownerKey);

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();
    ownerPermissionKeys.add(ownerKey);
    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\","
            + "\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":3}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"" + AVAILABLE_OPERATION + "\",\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(
        PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission());

    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(tmpKey02);

    logger.info("** trigger a normal transaction");
    Transaction transaction = PublicMethedForMutiSign
        .sendcoin2(fromAddress, 1000_000, ownerAddress, ownerKey, blockingStubFull);

    Transaction transaction1 = PublicMethedForMutiSign
        .addTransactionSignWithPermissionId(transaction, ownerKey, 0, blockingStubFull);

    try {
      Thread.sleep(70000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    TransactionSignWeight txWeight = PublicMethedForMutiSign.getTransactionSignWeight(
        transaction1, blockingStubFull);
    logger.info("TransactionSignWeight info : " + txWeight);

    Assert
        .assertFalse(PublicMethedForMutiSign.broadcastTransaction(transaction1, blockingStubFull));

    Long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);

  }

  @Test(enabled = true, description = "Broadcast empty transaction")
  public void test08BroadcastEmptyTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    Assert.assertTrue(PublicMethed.sendcoin(ownerAddress, 1_000_000, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    PublicMethed.printAddress(ownerKey);

    logger.info("** created an empty transaction");

    AccountPermissionUpdateContract.Builder builder =
        AccountPermissionUpdateContract.newBuilder();

    AccountPermissionUpdateContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.accountPermissionUpdate(contract);
    Transaction transaction = transactionExtention.getTransaction();

    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, ownerKey, blockingStubFull);

    TransactionSignWeight txWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("TransactionSignWeight info : " + txWeight);

    Assert
        .assertFalse(PublicMethedForMutiSign.broadcastTransaction(transaction1, blockingStubFull));
    Long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore, balanceAfter);
  }

  @Test(enabled = true, description = "Broadcast error transaction")
  public void test09BroadcastErrorTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee;

    Assert.assertTrue(PublicMethed.sendcoin(ownerAddress, needCoin + 1_000_000, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\","
            + "\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":3}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"" + AVAILABLE_OPERATION + "\",\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":2}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertEquals(2,
        PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission());

    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(tmpKey02);

    logger.info("** trigger a fake transaction");
    Transaction transaction = PublicMethedForMutiSign
        .createFakeTransaction(ownerAddress, 1_000_000L, ownerAddress);
    Transaction transaction1 = PublicMethedForMutiSign
        .addTransactionSignWithPermissionId(transaction, testKey002, 0, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    TransactionSignWeight txWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("Before broadcast permission TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(3, txWeight.getCurrentWeight());

    Assert
        .assertFalse(PublicMethedForMutiSign.broadcastTransaction(transaction1, blockingStubFull));
    Long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);
  }

  @AfterMethod
  public void aftertest() {
    PublicMethed.freedResource(ownerAddress, ownerKey, fromAddress, blockingStubFull);
    PublicMethed.unFreezeBalance(fromAddress, testKey002, 0, ownerAddress, blockingStubFull);
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
