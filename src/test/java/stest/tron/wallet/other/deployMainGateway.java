package stest.tron.wallet.other;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
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
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class deployMainGateway {


  private final String testDepositTrx = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testDepositAddress = PublicMethed.getFinalAddress(testDepositTrx);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelSolidity = null;
  private static final long now = System.currentTimeMillis();
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  private static ByteString assetAccountId = null;
  private static final long TotalSupply = 1000L;
  private String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  private String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingSideStubFull = null;


  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;

  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);

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
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingSideStubFull = WalletGrpc.newBlockingStub(channelFull1);
  }

  @Test(enabled = true, description = "deploy Main Chain Gateway")
  public void test1DepositTrc20001() {

    PublicMethed.printAddress(testKeyFordeposit);

    Assert.assertTrue(PublicMethed
        .sendcoin(depositAddress, 1000_000_000L, testDepositAddress, testDepositTrx,
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

    String deployTxid = PublicMethed
        .deployContractWithConstantParame(contractName, abi, code, "constructor(address)",
            parame, "",
            maxFeeLimit,
            0L, 100, null, testKeyFordeposit, depositAddress
            , blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(deployTxid, blockingStubFull);
    byte[] mainChainGateway = infoById.get().getContractAddress().toByteArray();
    String mainChainGatewayAddress = WalletClient.encode58Check(mainChainGateway);
    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertNotNull(mainChainGateway);

    SmartContract smartContract = PublicMethed.getContract(mainChainGateway,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

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

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

}
