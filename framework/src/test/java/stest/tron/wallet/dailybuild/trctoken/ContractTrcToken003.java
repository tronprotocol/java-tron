package stest.tron.wallet.dailybuild.trctoken;

import static org.tron.api.GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;


@Slf4j
public class ContractTrcToken003 {

  private static final long now = System.currentTimeMillis();
  private static final long TotalSupply = 1000L;
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  private static ByteString assetAccountDev = null;
  private static ByteString assetAccountUser = null;
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  private String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");

  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] dev001Address = ecKey1.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  private ECKey ecKey2 = new ECKey(Utils.getRandom());
  private byte[] user001Address = ecKey2.getAddress();
  private String user001Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

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

    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    PublicMethed.printAddress(dev001Key);

  }

  @Test(enabled = true, description = "DeployContract with exception condition")
  public void deployTransferTokenContract() {
    Assert.assertTrue(PublicMethed
        .sendcoin(dev001Address, 1100_000_000L, fromAddress, testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed
        .sendcoin(user001Address, 1100_000_000L, fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress,
        PublicMethed.getFreezeBalanceCount(dev001Address, dev001Key, 50000L, blockingStubFull), 0,
        1, ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress, 10_000_000L, 0, 0,
        ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    long start = System.currentTimeMillis() + 2000;
    long end = System.currentTimeMillis() + 1000000000;
    //dev Create a new AssetIssue
    Assert.assertTrue(PublicMethed
        .createAssetIssue(dev001Address, tokenName, TotalSupply, 1, 10000, start, end, 1,
            description, url, 100000L, 100000L, 1L, 1L, dev001Key, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    assetAccountDev = PublicMethed.queryAccount(dev001Address, blockingStubFull).getAssetIssuedID();
    logger.info("The assetAccountDev token name: " + tokenName);
    logger.info("The assetAccountDev token ID: " + assetAccountDev.toStringUtf8());

    start = System.currentTimeMillis() + 2000;
    end = System.currentTimeMillis() + 1000000000;
    //user Create a new AssetIssue
    Assert.assertTrue(PublicMethed
        .createAssetIssue(user001Address, tokenName, TotalSupply, 1, 10000, start, end, 1,
            description, url, 100000L, 100000L, 1L, 1L, user001Key, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    assetAccountUser = PublicMethed.queryAccount(user001Address, blockingStubFull)
        .getAssetIssuedID();
    logger.info("The assetAccountUser token name: " + tokenName);
    logger.info("The assetAccountUser token ID: " + assetAccountUser.toStringUtf8());

    //before deploy, check account resource
    AccountResourceMessage accountResource = PublicMethed
        .getAccountResource(dev001Address, blockingStubFull);
    long energyLimit = accountResource.getEnergyLimit();
    long energyUsage = accountResource.getEnergyUsed();
    long balanceBefore = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();
    Long devAssetCountBefore = PublicMethed
        .getAssetIssueValue(dev001Address, assetAccountDev, blockingStubFull);
    Long userAssetCountBefore = PublicMethed
        .getAssetIssueValue(dev001Address, assetAccountUser, blockingStubFull);

    logger.info("before energyLimit is " + Long.toString(energyLimit));
    logger.info("before energyUsage is " + Long.toString(energyUsage));
    logger.info("before balanceBefore is " + Long.toString(balanceBefore));
    logger.info(
        "before dev has AssetId: " + assetAccountDev.toStringUtf8() + ", devAssetCountBefore: "
            + devAssetCountBefore);
    logger.info(
        "before dev has AssetId: " + assetAccountUser.toStringUtf8() + ", userAssetCountBefore: "
            + userAssetCountBefore);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String filePath = "./src/test/resources/soliditycode/contractTrcToken003.sol";
    String contractName = "tokenTest";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    // the tokenId is not exist
    String fakeTokenId = Long.toString(Long.valueOf(assetAccountDev.toStringUtf8()) + 100);
    Long fakeTokenValue = 100L;

    GrpcAPI.Return response = PublicMethed
        .deployContractAndGetResponse(contractName, abi, code, "", maxFeeLimit, 0L, 0, 10000,
            fakeTokenId, fakeTokenValue, null, dev001Key, dev001Address, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert
        .assertEquals("contract validate error : No asset !", response.getMessage().toStringUtf8());

    // deployer didn't have any such token
    fakeTokenId = assetAccountUser.toStringUtf8();
    fakeTokenValue = 100L;

    response = PublicMethed
        .deployContractAndGetResponse(contractName, abi, code, "", maxFeeLimit, 0L, 0, 10000,
            fakeTokenId, fakeTokenValue, null, dev001Key, dev001Address, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : assetBalance must greater than 0.",
        response.getMessage().toStringUtf8());

    // deployer didn't have any Long.MAX_VALUE
    fakeTokenId = Long.toString(Long.MAX_VALUE);
    fakeTokenValue = 100L;

    response = PublicMethed
        .deployContractAndGetResponse(contractName, abi, code, "", maxFeeLimit, 0L, 0, 10000,
            fakeTokenId, fakeTokenValue, null, dev001Key, dev001Address, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert
        .assertEquals("contract validate error : No asset !", response.getMessage().toStringUtf8());

    // the tokenValue is not enough
    fakeTokenId = assetAccountDev.toStringUtf8();
    fakeTokenValue = devAssetCountBefore + 100;

    response = PublicMethed
        .deployContractAndGetResponse(contractName, abi, code, "", maxFeeLimit, 0L, 0, 10000,
            fakeTokenId, fakeTokenValue, null, dev001Key, dev001Address, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : assetBalance is not sufficient.",
        response.getMessage().toStringUtf8());

    // tokenid is -1
    fakeTokenId = Long.toString(-1);
    response = PublicMethed
        .deployContractAndGetResponse(contractName, abi, code, "", maxFeeLimit, 0L, 0, 10000,
            fakeTokenId, 100, null, dev001Key, dev001Address, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : tokenId must be > 1000000",
        response.getMessage().toStringUtf8());

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    // tokenid is 100_0000L
    fakeTokenId = Long.toString(100_0000L);
    response = PublicMethed
        .deployContractAndGetResponse(contractName, abi, code, "", maxFeeLimit, 0L, 0, 10000,
            fakeTokenId, 100, null, dev001Key, dev001Address, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : tokenId must be > 1000000",
        response.getMessage().toStringUtf8());

    // tokenid is Long.MIN_VALUE
    fakeTokenId = Long.toString(Long.MIN_VALUE);
    response = PublicMethed
        .deployContractAndGetResponse(contractName, abi, code, "", maxFeeLimit, 0L, 0, 10000,
            fakeTokenId, 100, null, dev001Key, dev001Address, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : tokenId must be > 1000000",
        response.getMessage().toStringUtf8());

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    // tokenid is 0
    fakeTokenId = Long.toString(0);

    response = PublicMethed
        .deployContractAndGetResponse(contractName, abi, code, "", maxFeeLimit, 0L, 0, 10000,
            fakeTokenId, 100, null, dev001Key, dev001Address, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals(
        "contract validate error : invalid arguments with tokenValue = 100, tokenId = 0",
        response.getMessage().toStringUtf8());

    // tokenvalue is less than 0
    fakeTokenValue = -1L;

    response = PublicMethed
        .deployContractAndGetResponse(contractName, abi, code, "", maxFeeLimit, 0L, 0, 10000,
            assetAccountDev.toStringUtf8(), fakeTokenValue, null, dev001Key, dev001Address,
            blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : tokenValue must be >= 0",
        response.getMessage().toStringUtf8());

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    // tokenvalue is long.min
    fakeTokenValue = Long.MIN_VALUE;

    response = PublicMethed
        .deployContractAndGetResponse(contractName, abi, code, "", maxFeeLimit, 0L, 0, 10000,
            assetAccountDev.toStringUtf8(), fakeTokenValue, null, dev001Key, dev001Address,
            blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : tokenValue must be >= 0",
        response.getMessage().toStringUtf8());

    String tokenId = Long.toString(-1);
    long tokenValue = 0;
    long callValue = 10;

    response = PublicMethed
        .deployContractAndGetResponse(contractName, abi, code, "", maxFeeLimit, callValue, 0, 10000,
            tokenId, tokenValue, null, dev001Key, dev001Address, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : tokenId must be > 1000000",
        response.getMessage().toStringUtf8());

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    tokenId = Long.toString(Long.MIN_VALUE);
    tokenValue = 0;
    callValue = 10;

    response = PublicMethed
        .deployContractAndGetResponse(contractName, abi, code, "", maxFeeLimit, callValue, 0, 10000,
            tokenId, tokenValue, null, dev001Key, dev001Address, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : tokenId must be > 1000000",
        response.getMessage().toStringUtf8());

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    tokenId = Long.toString(1000000);
    tokenValue = 0;
    callValue = 10;

    response = PublicMethed
        .deployContractAndGetResponse(contractName, abi, code, "", maxFeeLimit, callValue, 0, 10000,
            tokenId, tokenValue, null, dev001Key, dev001Address, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : tokenId must be > 1000000",
        response.getMessage().toStringUtf8());

    accountResource = PublicMethed.getAccountResource(dev001Address, blockingStubFull);
    energyLimit = accountResource.getEnergyLimit();
    energyUsage = accountResource.getEnergyUsed();
    long balanceAfter = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();
    Long devAssetCountAfter = PublicMethed
        .getAssetIssueValue(dev001Address, assetAccountDev, blockingStubFull);
    Long userAssetCountAfter = PublicMethed
        .getAssetIssueValue(dev001Address, assetAccountUser, blockingStubFull);

    logger.info("after energyLimit is " + Long.toString(energyLimit));
    logger.info("after energyUsage is " + Long.toString(energyUsage));
    logger.info("after balanceAfter is " + Long.toString(balanceAfter));
    logger.info(
        "after dev has AssetId: " + assetAccountDev.toStringUtf8() + ", devAssetCountAfter: "
            + devAssetCountAfter);
    logger.info(
        "after user has AssetId: " + assetAccountDev.toStringUtf8() + ", userAssetCountAfter: "
            + userAssetCountAfter);

    Assert.assertEquals(devAssetCountBefore, devAssetCountAfter);
    Assert.assertEquals(userAssetCountBefore, userAssetCountAfter);

    PublicMethed.unFreezeBalance(fromAddress, testKey002, 1, dev001Address, blockingStubFull);
    PublicMethed.unFreezeBalance(fromAddress, testKey002, 0, dev001Address, blockingStubFull);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed.freedResource(dev001Address, dev001Key, fromAddress, blockingStubFull);
    PublicMethed.freedResource(user001Address, user001Key, fromAddress, blockingStubFull);
    PublicMethed.unFreezeBalance(fromAddress, testKey002, 0, dev001Address, blockingStubFull);
    PublicMethed.unFreezeBalance(fromAddress, testKey002, 0, user001Address, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


