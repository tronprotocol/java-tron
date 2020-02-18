package stest.tron.wallet.onlinestress;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol;
import org.tron.protos.contract.WitnessContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;

@Slf4j
public class SupportTronlinkAutoTest {

  private final String testKey002 = "7400E3D0727F8A61041A8E8BF86599FE5597CE19DE451E59AED07D60967A5E25";
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
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
  private String fullnode = "47.252.84.177:50051";

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

  @Test(enabled = true, threadPoolSize = 1, invocationCount = 1)
  public void testMutiSignForAccount() {
    Integer i = 0;
    System.out.println("Start genterate address");
    while (i++ < 20) {
      ecKey1 = new ECKey(Utils.getRandom());
      manager1Address = ecKey1.getAddress();
      manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


      ecKey3 = new ECKey(Utils.getRandom());
      ownerAddress = ecKey3.getAddress();
      ownerKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
      //PublicMethed.printAddress(ownerKey);


      PublicMethed.sendcoin(ownerAddress, 200000000000L, fromAddress, testKey002,
          blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      permissionKeyString[0] = manager1Key;
      permissionKeyString[1] = ownerKey;
      ownerKeyString[0] = ownerKey;
      //ownerKeyString[1] = manager1Key;
      accountPermissionJson =
          "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
              + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
              + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
              + "\",\"weight\":1}]},"
              + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":2,"
              + "\"operations\":\"7fff1fc0033e0b00000000000000000000000000000000000000000000000000\","
              + "\"keys\":["
              + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
              + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}"
              + "]}]}";

      //logger.info(accountPermissionJson);
      PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey,
          blockingStubFull, ownerKeyString);
      System.out.println("owner " + i + " --------------------------------------------------------");
      PublicMethed.printAddress(ownerKey);
      System.out.println("mutli sig address for owner " + i  +" ----------------------------------");
      PublicMethed.printAddress(manager1Key);
      System.out.println("------------------------------------------------------------------------");

    }


  }


  @Test(enabled = true, threadPoolSize = 1, invocationCount = 1)
  public void test002CreateWitness() {
    Integer i = 0;
    System.out.println("Start genterate  witness address");
    while (i++ < 10) {
      ecKey3 = new ECKey(Utils.getRandom());
      ownerAddress = ecKey3.getAddress();
      ownerKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
      //PublicMethed.printAddress(ownerKey);


      PublicMethed.sendcoin(ownerAddress, 50000000000L, fromAddress, testKey002,
          blockingStubFull);

      PublicMethed.waitProduceNextBlock(blockingStubFull);

      String createWitnessUrl = "IOS-UI-Witness-00" + i;
      byte[] createUrl = createWitnessUrl.getBytes();
      createWitness(ownerAddress,createUrl,ownerKey);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      System.out.println("witness " + i + " --------------------------------------------------------");
      PublicMethed.printAddress(ownerKey);
      System.out.println("witness url is : " + createWitnessUrl);
      System.out.println("------------------------------------------------------------------------");

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


  public Boolean createWitness(byte[] owner, byte[] url, String priKey) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    WitnessContract.WitnessCreateContract.Builder builder = WitnessContract.WitnessCreateContract
        .newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setUrl(ByteString.copyFrom(url));
    WitnessContract.WitnessCreateContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.createWitness(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }
    transaction = PublicMethed.signTransaction(ecKey, transaction);
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      return false;
    } else {
      return true;
    }

  }

}


