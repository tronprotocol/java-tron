package stest.tron.wallet.solidityadd;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;



@Slf4j
public class addMsg001Nonpayable {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
        .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
          .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelSolidity = null;

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;


  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;

  private String fullnode = Configuration.getByPath("testng.conf")
            .getStringList("fullnode.ip.list").get(1);
  private String fullnode1 = Configuration.getByPath("testng.conf")
            .getStringList("fullnode.ip.list").get(0);
  private static final long now = System.currentTimeMillis();
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  private static ByteString assetAccountId = null;
  private static final long TotalSupply = 1000L;
  private String description = Configuration.getByPath("testng.conf")
          .getString("defaultParameter.assetDescription");
  private String url = Configuration.getByPath("testng.conf")
          .getString("defaultParameter.assetUrl");

  byte[] contractAddress = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] toAddress = ecKey2.getAddress();
  String toAddressKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(contractExcKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
                .usePlaintext(true)
                .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
                .usePlaintext(true)
                .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
  }

  @Test(enabled = true, description = "Support function type")
  public void test1Grammar001() {
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed
         .sendcoin(contractExcAddress, 100000000000L, testNetAccountAddress,
                 testNetAccountKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    long start = System.currentTimeMillis() + 2000;
    long end = System.currentTimeMillis() + 1000000000;
    //Create a new AssetIssue success.
    Assert.assertTrue(PublicMethed.createAssetIssue(contractExcAddress, tokenName, TotalSupply, 1,
            10000, start, end, 1, description, url, 100000L, 100000L,
            1L, 1L, contractExcKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Protocol.Account getAssetIdFromThisAccount = PublicMethed
            .queryAccount(contractExcAddress, blockingStubFull);
    assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();
    String filePath = "src/test/resources/soliditycode/addMsg001Nonpayable.sol";
    String contractName = "IllegalDecorate";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    logger.info("code:" + code);
    logger.info("abi:" + abi);


    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
             0L, 100, null, contractExcKey,
                contractExcAddress, blockingStubFull);
    Protocol.Account info;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed
              .sendcoin(contractAddress, 100000000000L, testNetAccountAddress,
                      testNetAccountKey, blockingStubFull));
    Assert.assertTrue(PublicMethed.transferAsset(contractAddress,
            assetAccountId.toByteArray(), 100L, contractExcAddress,
            contractExcKey, blockingStubFull));
    GrpcAPI.AccountResourceMessage resourceInfo =
            PublicMethed.getAccountResource(contractExcAddress, blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);


    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    String txid = "";
    Long tokenvalue = 10L;
    String tokenid = assetAccountId.toStringUtf8();
    String para = "\"" + Base58.encode58Check(toAddress)
            + "\",\"" + tokenvalue + "\"";
    txid = PublicMethed.triggerContract(contractAddress,
            "transferTokenWithOutPayable(address,uint256)", para, false,0,maxFeeLimit,assetAccountId.toStringUtf8(),10, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull1);
    Optional<Protocol.TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsageTotal();
    Long netFee = infoById.get().getReceipt().getNetFee();
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
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
