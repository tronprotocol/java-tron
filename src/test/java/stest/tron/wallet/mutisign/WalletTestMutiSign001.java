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
  String[] ownerKeyString = new String[1];
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

    Assert.assertTrue(PublicMethed.sendcoin(ownerAddress,2048000000L,fromAddress,testKey002,
        blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;
    ownerKeyString[0] = ownerKey;
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
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

    permissionKeyString[0] = ownerKey;

    Long start = System.currentTimeMillis() + 5000;
    Long end = System.currentTimeMillis() + 1000000000;
    logger.info("try create asset issue");

    Assert.assertTrue(PublicMethedForMutiSign.createAssetIssue(ownerAddress,name,totalSupply,1,
        1,start,end,1,description,url,2000L,2000L,
        1L, 1L, ownerKey, blockingStubFull, permissionKeyString));
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
    Assert.assertTrue(PublicMethedForMutiSign.transferAsset(manager1Address,
        assetAccountId1.toByteArray(), 10,ownerAddress,ownerKey,blockingStubFull,
        permissionKeyString));
  }

  /**
   * constructor.
   */

  @Test(enabled = true)
  public void testMutiSign3ParticipateAssetissue() {
    ecKey4 = new ECKey(Utils.getRandom());
    participateAddress = ecKey4.getAddress();
    participateKey = ByteArray.toHexString(ecKey4.getPrivKeyBytes());

    Assert.assertTrue(PublicMethed.sendcoin(participateAddress,2048000000L,fromAddress,testKey002,
        blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    ownerKeyString[0] = participateKey;
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
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

    permissionKeyString[0] = participateKey;

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethedForMutiSign.participateAssetIssue(ownerAddress,assetAccountId1
            .toByteArray(), 10,participateAddress,participateKey,
        blockingStubFull, permissionKeyString));
  }

  /**
   * constructor.
   */

  @Test(enabled = true)
  public void testMutiSign4updateAssetissue() {
    url = "MutiSign001_update_url" + Long.toString(now);
    ownerKeyString[0] = ownerKey;
    description = "MutiSign001_update_description" + Long.toString(now);
    Assert.assertTrue(PublicMethedForMutiSign.updateAsset(ownerAddress,description.getBytes(),url
        .getBytes(), 100L, 100L, ownerKey, blockingStubFull, ownerKeyString));
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


