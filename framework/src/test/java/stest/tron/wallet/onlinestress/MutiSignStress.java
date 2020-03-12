package stest.tron.wallet.onlinestress;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
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
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;

@Slf4j
public class MutiSignStress {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String witnessKey001 = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] witnessAddress = PublicMethed.getFinalAddress(witnessKey001);
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
  byte[] newAddress = ecKey4.getAddress();
  String newKey = ByteArray.toHexString(ecKey4.getPrivKeyBytes());
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

  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = true, threadPoolSize = 20, invocationCount = 20)
  public void testMutiSignForAccount() {
    Integer i = 0;
    while (i < 20) {
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

      PublicMethed.sendcoin(ownerAddress, 4000000L, fromAddress, testKey002,
          blockingStubFull);
      PublicMethed.sendcoin(ownerAddress, 4000000L, fromAddress, testKey002,
          blockingStubFull);
      PublicMethed.sendcoin(ownerAddress, 4000000L, fromAddress, testKey002,
          blockingStubFull);
      permissionKeyString[0] = manager1Key;
      permissionKeyString[1] = manager2Key;
      ownerKeyString[0] = ownerKey;
      accountPermissionJson = "[{\"keys\":[{\"address\":\""
          + PublicMethed.getAddressString(ownerKey)
          + "\",\"weight\":2}],\"name\":\"owner\",\"threshold\":2,\"parent\":\"owner\"},"
          + "{\"parent\":\"owner\",\"keys\":[{\"address\":\""
          + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},{\"address\":\""
          + PublicMethed.getAddressString(manager2Key) + "\",\"weight\":1}],\"name\":\"active\","
          + "\"threshold\":2}]";
      //logger.info(accountPermissionJson);
      PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey,
          blockingStubFull, ownerKeyString);

      String updateName = Long.toString(System.currentTimeMillis());

      PublicMethedForMutiSign.sendcoin(newAddress, 1000000L, ownerAddress, ownerKey,
          blockingStubFull, permissionKeyString);
      PublicMethedForMutiSign.sendcoin(newAddress, 1000000L, ownerAddress, ownerKey,
          blockingStubFull, permissionKeyString);
      PublicMethedForMutiSign.sendcoin(newAddress, 1000000L, ownerAddress, ownerKey,
          blockingStubFull, permissionKeyString);
      PublicMethedForMutiSign.freezeBalance(ownerAddress, 1000000L, 0,
          ownerKey, blockingStubFull, permissionKeyString);
      PublicMethedForMutiSign.freezeBalance(ownerAddress, 1000000L, 0,
          ownerKey, blockingStubFull, permissionKeyString);
      PublicMethedForMutiSign.freezeBalance(ownerAddress, 1000000L, 0,
          ownerKey, blockingStubFull, permissionKeyString);
      PublicMethedForMutiSign.unFreezeBalance(ownerAddress, ownerKey, 0, null,
          blockingStubFull, permissionKeyString);
      PublicMethedForMutiSign.unFreezeBalance(ownerAddress, ownerKey, 0, null,
          blockingStubFull, permissionKeyString);
      PublicMethedForMutiSign.unFreezeBalance(ownerAddress, ownerKey, 0, null,
          blockingStubFull, permissionKeyString);
    }


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


