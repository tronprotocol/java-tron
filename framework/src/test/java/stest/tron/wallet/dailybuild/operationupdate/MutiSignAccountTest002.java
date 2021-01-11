package stest.tron.wallet.dailybuild.operationupdate;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import java.util.Random;
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
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;

@Slf4j
public class MutiSignAccountTest002 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String witnessKey001 = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] witnessAddress = PublicMethed.getFinalAddress(witnessKey001);
  private final String operations = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.operations");
  ByteString assetAccountId1;
  String[] permissionKeyString = new String[2];
  String[] ownerKeyString = new String[3];
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
  byte[] newAddress = ecKey4.getAddress();
  String newKey = ByteArray.toHexString(ecKey4.getPrivKeyBytes());
  private long multiSignFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.multiSignFee");
  private long updateAccountPermissionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.updateAccountPermissionFee");
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  /**
   * constructor.
   */

  public static byte[] randomBytes(int length) {
    // generate the random number
    byte[] result = new byte[length];
    new Random().nextBytes(result);
    result[0] = Wallet.getAddressPreFixByte();
    return result;
  }

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
  public void testMutiSignForAccount() {
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

    ecKey4 = new ECKey(Utils.getRandom());
    newAddress = ecKey4.getAddress();
    newKey = ByteArray.toHexString(ecKey4.getPrivKeyBytes());

    long needCoin = updateAccountPermissionFee * 1 + multiSignFee * 10;

    Assert.assertTrue(
        PublicMethed.sendcoin(ownerAddress, needCoin + 100000000L, fromAddress, testKey002,
            blockingStubFull));

    Assert.assertTrue(PublicMethed
        .freezeBalanceForReceiver(fromAddress, 1000000000, 0, 0, ByteString.copyFrom(ownerAddress),
            testKey002, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;
    ownerKeyString[0] = ownerKey;
    ownerKeyString[1] = manager1Key;
    ownerKeyString[2] = manager2Key;
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":3,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(manager2Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"" + operations + "\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(manager2Key) + "\",\"weight\":1}"
            + "]}]}";
    logger.info(accountPermissionJson);
    String txid = PublicMethedForMutiSign
        .accountPermissionUpdateForTransactionId(accountPermissionJson, ownerAddress, ownerKey,
            blockingStubFull, ownerKeyString);

    final String updateName = Long.toString(System.currentTimeMillis());
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
    Assert.assertEquals(fee, energyFee + netFee + updateAccountPermissionFee);

    balanceBefore = balanceAfter;
    byte[] accountName = "11z2112310".getBytes();
    Assert.assertTrue(PublicMethedForMutiSign.createAccount1(
        ownerAddress, newAddress, ownerKey, blockingStubFull, 2, permissionKeyString));
    Assert.assertTrue(
        PublicMethedForMutiSign.setAccountId1(accountName,
            ownerAddress, ownerKey, 2, blockingStubFull, permissionKeyString));
    Assert.assertTrue(PublicMethedForMutiSign.sendcoinWithPermissionId(
        newAddress, 100L, ownerAddress, 2, ownerKey, blockingStubFull, permissionKeyString));
    Assert.assertTrue(PublicMethedForMutiSign.freezeBalanceWithPermissionId(
        ownerAddress, 1000000L, 0, 2, ownerKey, blockingStubFull, permissionKeyString));
    Assert.assertTrue(PublicMethedForMutiSign.freezeBalanceGetEnergyWithPermissionId(
        ownerAddress, 1000000L, 0, 1, ownerKey, blockingStubFull, 2, permissionKeyString));
    Assert.assertTrue(PublicMethedForMutiSign.freezeBalanceForReceiverWithPermissionId(
        ownerAddress, 1000000L, 0, 0, ByteString.copyFrom(newAddress),
        ownerKey, blockingStubFull, 2, permissionKeyString));
    Assert.assertTrue(PublicMethedForMutiSign.unFreezeBalanceWithPermissionId(
        ownerAddress, ownerKey, 0, null, 2, blockingStubFull, permissionKeyString));
    Assert.assertTrue(PublicMethedForMutiSign.unFreezeBalanceWithPermissionId(
        ownerAddress, ownerKey, 0, newAddress, 2, blockingStubFull, permissionKeyString));
    Assert.assertTrue(PublicMethedForMutiSign.updateAccountWithPermissionId(
        ownerAddress, updateName.getBytes(), ownerKey, blockingStubFull, 2, permissionKeyString));

    String voteStr = Base58.encode58Check(witnessAddress);
    HashMap<String, String> smallVoteMap = new HashMap<String, String>();
    smallVoteMap.put(voteStr, "1");
    Assert.assertTrue(PublicMethedForMutiSign.voteWitnessWithPermissionId(
        smallVoteMap, ownerAddress, ownerKey, blockingStubFull, 2, permissionKeyString));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, multiSignFee * 10 + 1000000 + 100);

    Assert.assertTrue(
        PublicMethed.unFreezeBalance(fromAddress, testKey002, 0, ownerAddress, blockingStubFull));

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


