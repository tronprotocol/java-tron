package stest.tron.wallet.other;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.spongycastle.util.encoders.Hex;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.SmartContract;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.AbiUtil;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;

@Slf4j
public class deployMainGateway {

  private final String testDepositTrx = "324a2052e491e99026442d81df4d2777292840c1b3949e20696c49096c6bacb7";
  private final byte[] testDepositAddress = PublicMethed.getFinalAddress(testDepositTrx);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private final String foundationKey001 = Configuration.getByPath("testng.conf").
      getString("foundationAccount.key1");
  private final byte[] foundationAddress001 = PublicMethed.getFinalAddress(foundationKey001);
  private final String foundationKey002 = Configuration.getByPath("testng.conf").
      getString("foundationAccount.key2");
  private final byte[] foundationAddress002 = PublicMethed.getFinalAddress(foundationKey002);


  private static final long now = System.currentTimeMillis();
  private static final long TotalSupply = 1000000000000000L;
  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  final String[] AssertAccount = {
    "13ecfac7f8ca3fce74abda0452a38a1b3383d9c4470044b7a9e8567604db33f9",
    "5f6194a1b855552a6ff81e99c441f0a43c5eb223dc6fae46d136cffb8b974978",
    "d2614c199183c7f88562c3f0a9036a038149a3e3e76260954e125ec78064dd31",
    "2c379393417ced659d9f0b93fc1c9d7ca8baaa079d8555c4b00618f96fe307ef",
    "c44cbe6e2212d6961daee8ef0c9dab9340028cd8dbd03efc6d1f382fcbd774f9",
    "d3242e7dec6fe5c94f114ac3aaf024a5648e98fa1f0fbd982e4c569849922dbd"
  };



  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;

  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] depositAddress = ecKey1.getAddress();
  String testKeyFordeposit = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

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
//    PublicMethed.printAddress(testKeyFordeposit);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = true, description = "deploy Main Chain Gateway")
  public void deplyMainChainGateway() {

    PublicMethed.printAddress(testKeyFordeposit);

    Assert.assertTrue(PublicMethed
        .sendcoin(depositAddress, 10000000_000_000L, testDepositAddress, testDepositTrx,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account accountOralce = PublicMethed.queryAccount(depositAddress, blockingStubFull);
    long OralceBalance = accountOralce.getBalance();
    logger.info("OralceBalance: " + OralceBalance);

    String contractName = "gateWayContract";
    String code = Configuration.getByPath("testng.conf")
        .getString("code.code_MainGateway");
    String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_MainGateway");
    String parame = "\"" + Base58.encode58Check(testDepositAddress) + "\"";
    String mainChainGatewayAddress = "3QJmnh";

    int tryCount = 0;
    while (tryCount++ < 3) {
      String deployTxid = PublicMethed
          .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
              maxFeeLimit,
              0L, 100, null, testKeyFordeposit, depositAddress
              , blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);

      Optional<TransactionInfo> infoById = PublicMethed
          .getTransactionInfoById(deployTxid, blockingStubFull);
      byte[] mainChainGateway = infoById.get().getContractAddress().toByteArray();
      mainChainGatewayAddress = WalletClient.encode58Check(mainChainGateway);

      if((!mainChainGatewayAddress.equals("3QJmnh"))&& !mainChainGatewayAddress.isEmpty()){
        String triggerTxid1 = PublicMethed
            .triggerContract(
                WalletClient.decodeFromBase58Check(mainChainGatewayAddress),"addOracle(address)",parame,false,0L, maxFeeLimit,
                depositAddress, testKeyFordeposit, blockingStubFull);
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        Optional<TransactionInfo> infoById1 = PublicMethed
            .getTransactionInfoById(triggerTxid1, blockingStubFull);
        break;
      }
    }


    String outputPath = "./src/test/resources/mainChainGatewayAddress" ;
    try {
      File mainChainFile = new File(outputPath);
      Boolean cun = mainChainFile.createNewFile();
      FileWriter writer = new FileWriter(mainChainFile);
      BufferedWriter out = new BufferedWriter(writer);
      out.write(mainChainGatewayAddress);

      out.close();
      writer.close();
    }catch (Exception e){
      e.printStackTrace();
    }

  }

  @Test(enabled = true, description = "deploy Main Chain Gateway")
  public void createTokenFoundation(){
    logger.info("foundationAccount 001 : ");
    PublicMethed.printAddress(foundationKey001);
    logger.info("foundationAccount 002 : ");
    PublicMethed.printAddress(foundationKey002);

    long start = System.currentTimeMillis() + 2000;
    long end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethed.createAssetIssue(foundationAddress001,"testAssetIssue_001",TotalSupply,
        1,1, start,end,1,description,url,maxFeeLimit,1000L,
        1L,1L,foundationKey001,blockingStubFull));

    start = System.currentTimeMillis() + 2000;
    end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethed.createAssetIssue(foundationAddress002,"testAssetIssue_002",TotalSupply,
        1,1, start,end,1,description,url,maxFeeLimit,1000L,
        1L,1L,foundationKey002,blockingStubFull));

    for(int keycount=0;keycount<AssertAccount.length;keycount++){

      String AssertKey = AssertAccount[keycount];
      byte[] AssertAddress = PublicMethed.getFinalAddress(AssertKey);
      Assert.assertTrue(PublicMethed.sendcoin(AssertAddress,10000000000000L,
          foundationAddress001,foundationKey001,blockingStubFull));
      PublicMethed.waitProduceNextBlock(blockingStubFull);

      String AssertName = "testAssetIssue_00" + keycount;
      start = System.currentTimeMillis() + 2000;
      end = System.currentTimeMillis() + 1000000000;
      Assert.assertTrue(PublicMethed.createAssetIssue(AssertAddress,AssertName,TotalSupply,
          1,1, start,end,1,description,url,maxFeeLimit,1000L,
          1L,1L,AssertKey,blockingStubFull));
      try {
        Thread.sleep(1000);
      }catch (Exception e){
        e.printStackTrace();
      }

    }

  }
  /**
   * constructor
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

}
