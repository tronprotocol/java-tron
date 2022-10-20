package stest.tron.wallet.dailybuild.trctoken;

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
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;


@Slf4j
public class ContractTrcToken081 {

  private static final long now = System.currentTimeMillis();
  private static final long TotalSupply = 1000L;
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  private static ByteString assetAccountId = null;
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private byte[] tokenReceiver = null;
  private byte[] tokenSender = null;

  private String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  private String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");

  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] dev001Address = ecKey1.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {

    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    PublicMethed.printAddress(dev001Key);
    Assert.assertTrue(PublicMethed
        .sendcoin(dev001Address, 3100_000_000L, fromAddress, testKey002, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress,
        PublicMethed.getFreezeBalanceCount(dev001Address, dev001Key, 130000L, blockingStubFull), 0,
        1, ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));

    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress, 10_000_000L, 0, 0,
        ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    long start = System.currentTimeMillis() + 2000;
    long end = System.currentTimeMillis() + 1000000000;

    //Create a new AssetIssue success.
    Assert.assertTrue(PublicMethed
        .createAssetIssue(dev001Address, tokenName, TotalSupply, 1, 10000, start, end, 1,
            description, url, 100000L, 100000L, 1L, 1L, dev001Key, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    assetAccountId = PublicMethed.queryAccount(dev001Address, blockingStubFull).getAssetIssuedID();

    logger.info("The token name: " + tokenName);
    logger.info("The token ID: " + assetAccountId.toStringUtf8());

    Long devAssetCountBefore = PublicMethed
        .getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);

    logger.info("before AssetId: " + assetAccountId.toStringUtf8() + ", devAssetCountBefore: "
        + devAssetCountBefore);

    String filePath = "./src/test/resources/soliditycode/contractTrcToken081.sol";
    String contractName = "TokenReceiver";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    String tokenId = assetAccountId.toStringUtf8();

    tokenReceiver = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        500000000L, 100, null, dev001Key, dev001Address, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethed
        .getContract(tokenReceiver, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());


    contractName = "TokenSender";
    retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    tokenSender = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        500000000L, 100, 10000L, assetAccountId.toStringUtf8(),
        10L, null, dev001Key, dev001Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    smartContract = PublicMethed.getContract(tokenSender,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    Long contractAssetCount = PublicMethed
        .getAssetIssueValue(tokenSender, assetAccountId, blockingStubFull);
    logger.info("tokenSender has AssetId before: " + assetAccountId.toStringUtf8() + ", Count: "
        + contractAssetCount);

    Long devAssetCountAfterDeploy = PublicMethed
        .getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);
    logger.info("after deploy tokenSender AssetId: " + assetAccountId.toStringUtf8()
        + ", devAssetCountAfter: " + devAssetCountAfterDeploy);
    Assert.assertTrue(10 == devAssetCountBefore - devAssetCountAfterDeploy);
    Assert.assertTrue(10 == contractAssetCount);

  }


  @Test(enabled = true, description = "transfer 1 trc10 to contract by assembly")
  public void transferTokenToContract() {
    Long senderAssetCountBefore = PublicMethed
        .getAssetIssueValue(tokenSender, assetAccountId, blockingStubFull);
    logger.info("before trigger tokenSender has AssetId before: " + assetAccountId.toStringUtf8()
        + ", Count: " + senderAssetCountBefore);

    Long receiverAssetCountBefore = PublicMethed
        .getAssetIssueValue(tokenReceiver, assetAccountId, blockingStubFull);
    logger.info("before trigger tokenReceiver AssetId: " + assetAccountId.toStringUtf8()
        + ", Count: " + receiverAssetCountBefore);
    String args = "\"" + Base58.encode58Check(tokenReceiver) + "\"";
    logger.info("args: " + args);
    String triggerTxid = PublicMethed
        .triggerContract(tokenSender, "sendTRC10(address)", args, false, 0, 1000000000L,
            assetAccountId.toStringUtf8(), 0, dev001Address, dev001Key, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<Protocol.TransactionInfo>  infoById =
        PublicMethed.getTransactionInfoById(triggerTxid, blockingStubFull);

    if (infoById.get().getResultValue() != 0) {
      Assert.fail("transaction failed with message: " + infoById.get().getResMessage());
    }
    Long senderAssetCountAfter = PublicMethed
        .getAssetIssueValue(tokenSender, assetAccountId, blockingStubFull);
    logger.info("tokenSender has AssetId After trigger: " + assetAccountId.toStringUtf8()
        + ", Count: " + senderAssetCountAfter);

    Long receiverAssetCountAfterTrigger = PublicMethed
        .getAssetIssueValue(tokenReceiver, assetAccountId, blockingStubFull);
    logger.info("after trigger AssetId: " + assetAccountId.toStringUtf8() + ", Count: "
        + receiverAssetCountAfterTrigger);
    Assert.assertTrue(1 == senderAssetCountBefore - senderAssetCountAfter);
    Assert.assertTrue(1 == receiverAssetCountAfterTrigger - receiverAssetCountBefore);

  }

  @Test(enabled = true, description = "transfer 1 trc10 to normal address by assembly")
  public void transferTokenToNormalAddress() {
    long senderAssetCountBefore = PublicMethed
        .getAssetIssueValue(tokenSender, assetAccountId, blockingStubFull);
    logger.info("tokenSender has AssetId After trigger: " + assetAccountId.toStringUtf8()
        + ", Count: " + senderAssetCountBefore);

    long devAssetCountBeforeTrigger = PublicMethed
        .getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);
    logger.info("after trigger AssetId: " + assetAccountId.toStringUtf8()
        + ", devAssetCountAfterTrigger: " + devAssetCountBeforeTrigger);

    String args = "\"" + Base58.encode58Check(dev001Address) + "\"";
    logger.info("args: " + args);
    String triggerTxid = PublicMethed
        .triggerContract(tokenSender, "sendTRC10(address)", args, false, 0, 1000000000L,
            assetAccountId.toStringUtf8(), 0, dev001Address, dev001Key, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<Protocol.TransactionInfo>  infoById =
        PublicMethed.getTransactionInfoById(triggerTxid, blockingStubFull);

    if (infoById.get().getResultValue() != 0) {
      Assert.fail("transaction failed with message: " + infoById.get().getResMessage());
    }
    long senderAssetCountAfter = PublicMethed
        .getAssetIssueValue(tokenSender, assetAccountId, blockingStubFull);
    logger.info("tokenSender has AssetId After trigger: " + assetAccountId.toStringUtf8()
        + ", Count: " + senderAssetCountAfter);

    long devAssetCountAfterTrigger = PublicMethed
        .getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);
    logger.info("after trigger AssetId: " + assetAccountId.toStringUtf8()
        + ", devAssetCountAfterTrigger: " + devAssetCountAfterTrigger);
    Assert.assertTrue(1 == senderAssetCountBefore - senderAssetCountAfter);
    Assert.assertTrue(1 == devAssetCountAfterTrigger - devAssetCountBeforeTrigger);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed.freedResource(dev001Address, dev001Key, fromAddress, blockingStubFull);
    PublicMethed.unFreezeBalance(fromAddress, testKey002, 1, dev001Address, blockingStubFull);
    PublicMethed.unFreezeBalance(fromAddress, testKey002, 0, dev001Address, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


