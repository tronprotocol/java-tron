package stest.tron.wallet.mutisign;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;

@Slf4j
public class WalletTestMutiSign001 {

  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = now;
  private static String name = "MutiSign001_" + Long.toString(now);
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");
  ByteString assetAccountId1;
  String[] permissionKeyString = new String[2];
  String[] ownerKeyString = new String[2];
  String accountPermissionJson = "";
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] manager1Address = ecKey1.getAddress();
  String manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] manager2Address = ecKey2.getAddress();
  String manager2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] ownerAddress = ecKey3.getAddress();
  String ownerKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
  ECKey ecKey4 = new ECKey(Utils.getRandom());
  byte[] participateAddress = ecKey4.getAddress();
  String participateKey = ByteArray.toHexString(ecKey4.getPrivKeyBytes());
  private long multiSignFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.multiSignFee");
  private long updateAccountPermissionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.updateAccountPermissionFee");
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);

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

  @Test(enabled = true)
  public void testMutiSign1CreateAssetissue() {
    ecKey1 = new ECKey(Utils.getRandom());
    manager1Address = ecKey1.getAddress();
    manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    ecKey2 = new ECKey(Utils.getRandom());
    manager2Address = ecKey2.getAddress();
    manager2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    ecKey3 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey3.getAddress();
    ownerKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
    PublicMethed.printAddress(ownerKey);

    long needCoin = updateAccountPermissionFee * 1 + multiSignFee * 3;

    Assert.assertTrue(
        PublicMethed.sendcoin(ownerAddress, needCoin + 2048000000L, fromAddress, testKey002,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;
    ownerKeyString[0] = ownerKey;
    ownerKeyString[1] = manager1Key;
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(manager2Key) + "\",\"weight\":1}"
            + "]}]}";

    logger.info(accountPermissionJson);
    String txid = PublicMethedForMutiSign
        .accountPermissionUpdateForTransactionId(accountPermissionJson, ownerAddress, ownerKey,
            blockingStubFull, ownerKeyString);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);

    long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    long energyFee = infoById.get().getReceipt().getEnergyFee();
    long netFee = infoById.get().getReceipt().getNetFee();
    long fee = infoById.get().getFee();

    logger.info("balanceAfter: " + balanceAfter);
    logger.info("energyFee: " + energyFee);
    logger.info("netFee: " + netFee);
    logger.info("fee: " + fee);

    Assert.assertEquals(balanceBefore - balanceAfter, fee);
    Assert.assertEquals(fee, energyFee + netFee + updateAccountPermissionFee);

    balanceBefore = balanceAfter;

    Long start = System.currentTimeMillis() + 5000;
    Long end = System.currentTimeMillis() + 1000000000;
    logger.info("try create asset issue");

    txid = PublicMethedForMutiSign
        .createAssetIssueForTransactionId(ownerAddress, name, totalSupply, 1,
            1, start, end, 1, description, url, 2000L, 2000L,
            1L, 1L, ownerKey, blockingStubFull, ownerKeyString);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertNotNull(txid);

    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    energyFee = infoById.get().getReceipt().getEnergyFee();
    netFee = infoById.get().getReceipt().getNetFee();
    fee = infoById.get().getFee();

    logger.info("balanceAfter: " + balanceAfter);
    logger.info("energyFee: " + energyFee);
    logger.info("netFee: " + netFee);
    logger.info("fee: " + fee);

    Assert.assertEquals(balanceBefore - balanceAfter, fee);
    Assert.assertEquals(fee, energyFee + netFee + multiSignFee + 1024_000000L);

    logger.info(" create asset end");
  }

  /**
   * constructor.
   */

  @Test(enabled = true)
  public void testMutiSign2TransferAssetissue() {
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.printAddress(manager1Key);
    Account getAssetIdFromOwnerAccount;
    getAssetIdFromOwnerAccount = PublicMethed.queryAccount(ownerAddress, blockingStubFull);
    assetAccountId1 = getAssetIdFromOwnerAccount.getAssetIssuedID();
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    String txid = PublicMethedForMutiSign.transferAssetForTransactionId(manager1Address,
        assetAccountId1.toByteArray(), 10, ownerAddress, ownerKey, blockingStubFull,
        ownerKeyString);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertNotNull(txid);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    long energyFee = infoById.get().getReceipt().getEnergyFee();
    long netFee = infoById.get().getReceipt().getNetFee();
    long fee = infoById.get().getFee();

    logger.info("balanceAfter: " + balanceAfter);
    logger.info("energyFee: " + energyFee);
    logger.info("netFee: " + netFee);
    logger.info("fee: " + fee);

    Assert.assertEquals(balanceBefore - balanceAfter, fee);
    Assert.assertEquals(fee, energyFee + netFee + multiSignFee);
  }

  /**
   * constructor.
   */

  @Test(enabled = true)
  public void testMutiSign3ParticipateAssetissue() {
    ecKey4 = new ECKey(Utils.getRandom());
    participateAddress = ecKey4.getAddress();
    participateKey = ByteArray.toHexString(ecKey4.getPrivKeyBytes());

    long needCoin = updateAccountPermissionFee * 1 + multiSignFee * 2;

    Assert.assertTrue(
        PublicMethed.sendcoin(participateAddress, needCoin + 2048000000L, fromAddress, testKey002,
            blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(participateAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    ownerKeyString[0] = participateKey;
    ownerKeyString[1] = manager1Key;
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(participateKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(manager2Key) + "\",\"weight\":1}"
            + "]}]}";
    logger.info(accountPermissionJson);
    String txid = PublicMethedForMutiSign
        .accountPermissionUpdateForTransactionId(accountPermissionJson, participateAddress,
            participateKey, blockingStubFull, ownerKeyString);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertNotNull(txid);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    long balanceAfter = PublicMethed.queryAccount(participateAddress, blockingStubFull)
        .getBalance();
    long energyFee = infoById.get().getReceipt().getEnergyFee();
    long netFee = infoById.get().getReceipt().getNetFee();
    long fee = infoById.get().getFee();

    logger.info("balanceAfter: " + balanceAfter);
    logger.info("energyFee: " + energyFee);
    logger.info("netFee: " + netFee);
    logger.info("fee: " + fee);

    Assert.assertEquals(balanceBefore - balanceAfter, fee);
    Assert.assertEquals(fee, energyFee + netFee + updateAccountPermissionFee);

    balanceBefore = balanceAfter;

    txid = PublicMethedForMutiSign.participateAssetIssueForTransactionId(ownerAddress,
        assetAccountId1.toByteArray(), 10, participateAddress, participateKey, 0,
        blockingStubFull, ownerKeyString);

    Assert.assertNotNull(txid);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    balanceAfter = PublicMethed.queryAccount(participateAddress, blockingStubFull)
        .getBalance();
    energyFee = infoById.get().getReceipt().getEnergyFee();
    netFee = infoById.get().getReceipt().getNetFee();
    fee = infoById.get().getFee();

    logger.info("balanceAfter: " + balanceAfter);
    logger.info("energyFee: " + energyFee);
    logger.info("netFee: " + netFee);
    logger.info("fee: " + fee);

    Assert.assertEquals(balanceBefore - balanceAfter, fee + 10);
    Assert.assertEquals(fee, energyFee + netFee + multiSignFee);
  }

  /**
   * constructor.
   */

  @Test(enabled = true)
  public void testMutiSign4updateAssetissue() {
    url = "MutiSign001_update_url" + Long.toString(now);
    ownerKeyString[0] = ownerKey;
    description = "MutiSign001_update_description" + Long.toString(now);

    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    String txid = PublicMethedForMutiSign
        .updateAssetForTransactionId(ownerAddress, description.getBytes(), url.getBytes(), 100L,
            100L, ownerKey, 2, blockingStubFull, permissionKeyString);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertNotNull(txid);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    long energyFee = infoById.get().getReceipt().getEnergyFee();
    long netFee = infoById.get().getReceipt().getNetFee();
    long fee = infoById.get().getFee();

    logger.info("balanceAfter: " + balanceAfter);
    logger.info("energyFee: " + energyFee);
    logger.info("netFee: " + netFee);
    logger.info("fee: " + fee);

    Assert.assertEquals(balanceBefore - balanceAfter, fee);
    Assert.assertEquals(fee, energyFee + netFee + multiSignFee);
  }


  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


