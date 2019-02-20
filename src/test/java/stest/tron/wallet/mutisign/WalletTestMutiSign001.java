package stest.tron.wallet.mutisign;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
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
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;

@Slf4j
public class WalletTestMutiSign001 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private static final long now = System.currentTimeMillis();
  private static String name = "MutiSign001_" + Long.toString(now);
  private static final long totalSupply = now;
  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  ByteString assetAccountId1;
  String[] permissionKeyString = new String[2];
  String[] ownerKeyString = new String[5];
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

  ECKey ecKey5 = new ECKey(Utils.getRandom());
  byte[] manager3Address = ecKey5.getAddress();
  String manager3Key = ByteArray.toHexString(ecKey3.getPrivKeyBytes());

  ECKey ecKey6 = new ECKey(Utils.getRandom());
  byte[] manager4Address = ecKey6.getAddress();
  String manager4Key = ByteArray.toHexString(ecKey4.getPrivKeyBytes());

  ECKey ecKey7 = new ECKey(Utils.getRandom());
  byte[] manager5Address = ecKey7.getAddress();
  String manager5Key = ByteArray.toHexString(ecKey5.getPrivKeyBytes());

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
    manager1Key = Configuration.getByPath("testng.conf")
        .getString("witness.key1");
    manager1Address = PublicMethed.getFinalAddress(manager1Key);

    manager2Key = Configuration.getByPath("testng.conf")
        .getString("witness.key2");
    manager2Address = PublicMethed.getFinalAddress(manager2Key);

    manager3Key = Configuration.getByPath("testng.conf")
        .getString("witness.key3");
    manager3Address = PublicMethed.getFinalAddress(manager3Key);

    manager4Key = Configuration.getByPath("testng.conf")
        .getString("witness.key4");
    manager4Address = PublicMethed.getFinalAddress(manager4Key);

    manager5Key = Configuration.getByPath("testng.conf")
        .getString("witness.key5");
    manager5Address = PublicMethed.getFinalAddress(manager5Key);

    ecKey3 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey3.getAddress();
    ownerKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
    PublicMethed.printAddress(ownerKey);

    Assert.assertTrue(PublicMethed.sendcoin(ownerAddress,12999983262505860L,fromAddress,testKey002,
        blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    permissionKeyString[0] = Configuration.getByPath("testng.conf")
        .getString("witness.key1");
    permissionKeyString[1] = Configuration.getByPath("testng.conf")
        .getString("witness.key2");
    ownerKeyString[0] = ownerKey;
    //ownerKeyString[1] = manager2Key;
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":5,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(manager2Key) + "\",\"weight\":1}"
            + "{\"address\":\"" + PublicMethed.getAddressString(manager3Key) + "\",\"weight\":1}"
            + "{\"address\":\"" + PublicMethed.getAddressString(manager4Key) + "\",\"weight\":1}"
            + "{\"address\":\"" + PublicMethed.getAddressString(manager5Key) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(manager2Key) + "\",\"weight\":1}"
            + "]}]}";

    logger.info(accountPermissionJson);
    PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,ownerAddress,ownerKey,
        blockingStubFull,ownerKeyString);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    ownerKeyString[0] = manager1Key;
    ownerKeyString[1] = manager2Key;
    ownerKeyString[2] = manager3Key;
    ownerKeyString[3] = manager4Key;
    ownerKeyString[4] = manager5Key;

    Assert.assertTrue(PublicMethedForMutiSign.sendcoinWithPermissionId(
        manager2Address, 100L, ownerAddress, 0, ownerKey, blockingStubFull, ownerKeyString));
    logger.info(" create asset end");
  }
  /**
   * constructor.
   */

  @Test(enabled = false)
  public void testMutiSign2TransferAssetissue() {
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.printAddress(manager1Key);
    Account getAssetIdFromOwnerAccount;
    getAssetIdFromOwnerAccount = PublicMethed.queryAccount(ownerAddress, blockingStubFull);
    assetAccountId1 = getAssetIdFromOwnerAccount.getAssetIssuedID();
    Assert.assertTrue(PublicMethedForMutiSign.transferAsset(manager1Address,
        assetAccountId1.toByteArray(), 10,ownerAddress,ownerKey,blockingStubFull,
        ownerKeyString));
  }

  /**
   * constructor.
   */

  @Test(enabled = false)
  public void testMutiSign3ParticipateAssetissue() {
    ecKey4 = new ECKey(Utils.getRandom());
    participateAddress = ecKey4.getAddress();
    participateKey = ByteArray.toHexString(ecKey4.getPrivKeyBytes());

    Assert.assertTrue(PublicMethed.sendcoin(participateAddress,2048000000L,fromAddress,testKey002,
        blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    ownerKeyString[0] = participateKey;
    ownerKeyString[1] = manager1Key;
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
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
    PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,participateAddress,
        participateKey, blockingStubFull,ownerKeyString);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethedForMutiSign.participateAssetIssueWithPermissionId(ownerAddress,
        assetAccountId1.toByteArray(), 10, participateAddress, participateKey, 0,
        blockingStubFull, ownerKeyString));
  }

  /**
   * constructor.
   */

  @Test(enabled = false)
  public void testMutiSign4updateAssetissue() {
    url = "MutiSign001_update_url" + Long.toString(now);
    ownerKeyString[0] = ownerKey;
    description = "MutiSign001_update_description" + Long.toString(now);
    Assert.assertTrue(PublicMethedForMutiSign
        .updateAssetWithPermissionId(ownerAddress, description.getBytes(), url.getBytes(), 100L,
            100L, ownerKey, 2, blockingStubFull, permissionKeyString));
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


