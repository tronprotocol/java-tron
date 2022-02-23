package stest.tron.wallet.onlinestress;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.HashMap;
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
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;

@Slf4j
public class SupportTronlinkAutoTest {

  private final String testKey002
      = "7400E3D0727F8A61041A8E8BF86599FE5597CE19DE451E59AED07D60967A5E25";
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
  //Mainnet fullnode
  private String fullnode = "47.252.19.181:50051";
  //dappchain fullnode
  //private String fullnode =       "47.252.7.241:50051";



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
          "{\"owner_permission\":{\"type\":0,"
              + "\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
              + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
              + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
              + "\",\"weight\":1}]},"
              + "\"active_permissions\":[{\"type\":2,"
              + "\"permission_name\":\"active\",\"threshold\":2,"
              + "\"operations\":\""
              + "7fff1fc0033e0b00000000000000000000000000000000000000000000000000\","
              + "\"keys\":["
              + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
              + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}"
              + "]}]}";

      //logger.info(accountPermissionJson);
      PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey,
          blockingStubFull, ownerKeyString);
      System.out.println("owner" + i + " --------------------------------------------------------");
      PublicMethed.printAddress(ownerKey);
      System.out.println("mutli sig address for owner "
          + i + " ----------------------------------");
      PublicMethed.printAddress(manager1Key);
      System.out.println("-------------------------------"
          + "-----------------------------------------");

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
      createWitness(ownerAddress, createUrl, ownerKey);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      System.out.println("witness " + i + " -----------------------------"
          + "---------------------------");
      PublicMethed.printAddress(ownerKey);
      System.out.println("witness url is : " + createWitnessUrl);
      System.out.println("-------------------------------------------"
          + "-----------------------------");

    }


  }


  @Test(enabled = true, threadPoolSize = 1, invocationCount = 1)
  public void test03MutiSignForAccount() {
    HashMap<String, String> muti = new HashMap();
    muti.put("9a2ba173645be8d37a82084f984ba873fbcf817b589c62a59b3ba1494c3406e0",
        "cefba96470224724bde255f3402fca3d67b6c7c5d34deb7a8524c9482c58fe8b");
    muti.put("36f5430b4003f41ee8969421d9366ab1414e62111aec07a73d06eefcda8aad14",
        "3adcd73ad1fa03ce2fd4d29e29a7c96ef2f78bece85cba6e58997826682c4c1e");
    muti.put("4b47cf37528724dc8bc99188063f5aec9a7bc32aadfad5a96a9e9cccba7cede1",
        "948d856ebeb787aabd495fc13627f7442e7c1f21e9ed784f795e14f13cbebb94");
    muti.put("75d0856799cf2b2c807ed0eb5bb091bb943f2caed830add4b8df14c537f86e9a",
        "7fb13ad0b62d4ff116ebd3d901d458697902ce81a8fc30c20c60aba1ca6964ec");
    muti.put("327bf1b4a3193c2bebf239c1c5bda09a8d375251361ea9c7418aa2adf2d17b7e",
        "a8236968966db785ffe63d613174ee25e1baff03817b64db905c5940ed3dcc4b");
    muti.put("cf83d9494a9268fd3a75bd76bcfabaa7ec766e9084129a20e1823f81fbdca933",
        "1e53c948e949e39f60a3be2382382f9a50f88b658ea79c418ece1c5f9b169441");
    muti.put("19ff919e92462f07c7eced256d4cb588a66ac900d544d0d4d16ae49732de79cb",
        "166ddc2cd6379e7971e2c65d224594b709ebb59b3c6051c156214c299129f420");
    muti.put("7901db57a410a26d333b6d7fe4e054ddffbdc646f94ca03577bfd5e87120b9af",
        "89d9a47b37f5625e14082b575d5e657b21f6dae125125bee51fafd1e8cdf0804");
    muti.put("e3c7204d652a6fdcda05cbce061904d441dece7bf0a1778c1ddf0906aa36a279",
        "7d308998f829c0581447831003d994216a3a003ba00eef6a4e48e28b3178fbb3");
    muti.put("826fc86d572ba9de06f20963fcbfb44f4c397875bd4d7b36fdcb83476df33f05",
        "25aa122142a52ea8ba7bdf832c39a194d498774d4f675b8bcb17280c33990a08");
    muti.put("9705dd852465d04be349e94865142fc636d038138a4bfe8f94abc9b49f1dc14a",
        "0b675f14c1e06a6473c517dded162472ce2bb5c8934f198df1a791b75c30f983");
    muti.put("075f86d3d4053929714ddedb3e458467e6d494c3d4b0c71dafb63279de1beb89",
        "4880695a6e31f4f69d6f261eedfa5dcb5fc1b9194483658f77625ec4e6b2e493");
    muti.put("91ae6c8a1bff0b71f6f2b9de54d3b39502bcab906f980569109ab8425cb0bdc5",
        "90ef4adb0772ee49089784ccad234867a10395064749788b461cbe91265424fb");
    muti.put("9acb90c4d15c87dd2a1f322eddaabdde22cd78fe5eab371bfcf0c8be80bef8a8",
        "951f03193e1d7d4bff016da100b74f8ac220aabfd9c2841438ee758702c8e3f4");
    muti.put("f8eae7be0fac4e9fab40139e58b405f7e5d5b13a83220a6e4955ffaacbbe2a7d",
        "66692c0aaad6cfd349bdffbf3fdd688558a6c7a95ff67f249e0e80847167013a");
    muti.put("2e20c1a4b9a3a79696cbf0d03dedc39d8021657028fbf3dbc5da85ea61ad5dff",
        "ae7ecb7fba0d77d116a23f96a4dfecdef09741e363f0be12f99c86b3815d8fff");
    muti.put("e5e60c52f3b11ce0cfbc4e86d078ab53435ebc2422fd851614a25b5063ae7040",
        "42c575d8848809082c6872b2dcdb0e81d5f06ca120c636b90d0b062965ea0871");
    muti.put("fd4ee3a678a749c2049d5b1cba757648386c84ac2481be8de02069d41e4fb312",
        "ef2095532f572be8d021886780f7e508665ef40c1499273b91813ddc27d1354b");
    muti.put("297f6e034e9366691922ff5394810f724094bd0a07b4ca60f0f281ec71562094",
        "4ab67f1c42db0b63bafc0f33cf59575998c3259e96f5f89ea379dac4298d2bd7");
    int i = 1;
    for (HashMap.Entry entry : muti.entrySet()) {
      //entry.getKey().toString();
      //entry.getValue().toString();

      ownerKey = entry.getKey().toString();
      ownerAddress = PublicMethed.getFinalAddress(ownerKey);

      manager1Key = entry.getValue().toString();
      manager1Address = PublicMethed.getFinalAddress(manager1Key);

      //System.out.println("ownerKey:" + ownerKey);
      //System.out.println("manager1Key:" + manager1Key);

      //System.exit(1);
      PublicMethed.sendcoin(ownerAddress, 20000000000L, fromAddress, testKey002,
          blockingStubFull);

      PublicMethed.waitProduceNextBlock(blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      permissionKeyString[0] = manager1Key;
      permissionKeyString[1] = ownerKey;
      ownerKeyString[0] = ownerKey;
      //ownerKeyString[1] = manager1Key;
      accountPermissionJson =
          "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\""
              + "threshold\":2,\"keys\":["
              + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
              + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
              + "\",\"weight\":1}]},"
              + "\"active_permissions\":[{\"type\":2,\"permission_name\":\""
              + "active\",\"threshold\":2,"
              + "\"operations\":\""
              + "7fff1fc0033e0b00000000000000000000000000000000000000000000000000\","
              + "\"keys\":["
              + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
              + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}"
              + "]}]}";

      //logger.info(accountPermissionJson);
      PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey,
          blockingStubFull, ownerKeyString);
      System.out.println("owner " + i++ + " -----------------"
          + "---------------------------------------");
      PublicMethed.printAddress(ownerKey);
      System.out.println("mutli sig address for owner " + i++ + " ------------"
          + "----------------------");
      PublicMethed.printAddress(manager1Key);
      System.out.println("-----------------------------------"
          + "-------------------------------------");

    }

  }


  @Test(enabled = true, threadPoolSize = 1, invocationCount = 1)
  public void test004CreateWitness() {

    String[] witnessKey = {

        "c74fb4d8101572041c6fab30e1602ba1ec8247e1ead19641fb985b3ed3a8261e",
        "25f98ac22c9fd02aa8a2ef354db0aa13ebc2a6c31377ea7e2b342f0d3898af0d",
        "939a2cec3768bd2d2834126c20d2b1c513e3711f085ce374f654a7b144aa409f",
        "39862f4dd51972ca22ce50b7b9e629043387000120c33bf263399ad9b334da1a",
        "79045aab0f3199ac456ce2039e809e6c942983ede0e3a398d571dedddb351348",
        "d50fe9c48e95289cde324ffeff095f8275f9ab07375e5e843167a0a54d3e1462",
        "61651f2b8a87e1ae0ced5e700807f2abb50e97fe7d3d3e6a8aa58f0a6b0149a6",
        "bb03d70e5187258ffb6cddb1becade5c1b2606b7ea84636b7dfaeef6216610a5",
        "25858c236634e353d018f310f61e077b78e1410766565ed56ff11ee7410dcf20",
        "ede941a01eb8234866f60c7e8e95db4614bb0d05298d82bae0abea81f1861046",

    };
    int i = 1;
    for (String ownerKey : witnessKey) {
      byte[] ownerAddress = PublicMethed.getFinalAddress(ownerKey);
      PublicMethed.sendcoin(ownerAddress, 20000000000L, fromAddress, testKey002,
          blockingStubFull);

      PublicMethed.waitProduceNextBlock(blockingStubFull);

      String createWitnessUrl = "IOS-UI-Witness-00" + i++;
      byte[] createUrl = createWitnessUrl.getBytes();
      createWitness(ownerAddress, createUrl, ownerKey);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      PublicMethed.printAddress(ownerKey);
      System.out.println("witness url is : " + createWitnessUrl);
      System.out.println("---------------------------------------------------------------------");

    }


  }


  @Test(enabled = true, threadPoolSize = 1, invocationCount = 1)
  public void test005SendTrc20() {

    String[] witnessKey = {
        "TR8CyAPJFMjCvphCVuWeeVxBh5iTG7VWxe",
        "TMhGDU7NiXwckCW64PqAvWFuC2kR1WSF5J",
        "TDf3JZtjDeEqsFdPGp6vT9meG3JxMwmXwA",
        "TEtG9fnVi2qythiog6owPrg4sD9rwFBQBN",
        "TUvda1oqrNLbqDKhZDxDnrPhiDCdxem218",
        "TKEH31jJ2YQ3Bteh1ngjwdT8667ztyYPSp",
        "TAzrJHKa57nXnn3dZGFG87PDuWx12dY97s",
        "TWhc6AAh6BWRr3k5dV8iMvkp8ys7NHzXCk",
        "TSsaSxHnb3xLTop2A8LrDk1P896yiDeupe",
        "TMDs8oTj8mVnakqiVyDKdp2ruWPdFeDgbq",
        "TWv2FEsoPp5XKxujVHffoNwksgJSxvf3QG",
        "TGamEmt6U9ZUg9bFsMq7KT9bRa3uvkdtHM",
        "TXhQk442CCGLydh6cfyfqvM6yJanEGeQj1",
        "TKktQcbjXsXZDKPYLvUm8sxox2cT83g5rP",
        "TBQUhYhdQpMRksBGAbpbTWSiE7WkGgy3Km",
        "TALf34yjuLZjF1WQqCaUkf73X8WbhfiEyM",
        "TCGp3JAFM5vQZpsdNiKRTci7fVb7A2TPcu",
        "TBExF3mNvnhmEFgHW4TmYXXdhevRchnQyb",
        "TS8o6WcHroSnzWNt4AiserAuVkye5Msvcm",
        "TBtMRD79NkLyAvMkCTTj5VC5KZnz2Po2XZ",
    };
    int i = 1;
    for (String ownerKey : witnessKey) {
      String triggerString = "\"" + ownerKey
          + "\"" + ", 20000000000";
      System.out.println(triggerString);
      //dapp chain trc20 tract TXkdXbzjoLpxGAD2strP1zwjJzR6osNfD7
      byte[] contractAddress = PublicMethed.decode58Check("TXkdXbzjoLpxGAD2strP1zwjJzR6osNfD7");
      //main chain TRC 20 contract TCCcBZEdTHmS1NfFtCYfwpjBKeTv515n71
      //byte[] contractAddress =  PublicMethed.decode58Check("TCCcBZEdTHmS1NfFtCYfwpjBKeTv515n71");

      PublicMethed
          .triggerContract(contractAddress, "transfer(address,uint256)", triggerString, false,
              0, 1000000000, "0", 0, fromAddress, testKey002, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);


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

  /**
   * constructor.
   */
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
    return response.getResult();
  }
}