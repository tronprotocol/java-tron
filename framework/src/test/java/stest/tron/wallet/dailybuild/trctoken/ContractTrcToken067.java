package stest.tron.wallet.dailybuild.trctoken;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractTrcToken067 {

  private static final long now = System.currentTimeMillis();
  private static final long TotalSupply = 1000L;
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  private static ByteString assetAccountId = null;
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private byte[] transferTokenContractAddress = null;
  private byte[] resultContractAddress = null;

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



  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {

    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    PublicMethed.printAddress(dev001Key);
    PublicMethed.printAddress(user001Key);
  }

  @Test(enabled = true, description = "TransferToken with 0 tokenValue, "
      + "and not existed tokenId, deploy transfer contract")
  public void test01DeployTransferTokenContract() {
    Assert.assertTrue(PublicMethed.sendcoin(dev001Address, 5048_000_000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(user001Address, 4048_000_000L, fromAddress,
        testKey002, blockingStubFull));

    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress,
        PublicMethed.getFreezeBalanceCount(dev001Address, dev001Key, 170000L,
            blockingStubFull), 0, 1,
        ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));

    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress, 10_000_000L,
        0, 0, ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    long start = System.currentTimeMillis() + 2000;
    long end = System.currentTimeMillis() + 1000000000;
    //Create a new AssetIssue success.
    Assert.assertTrue(PublicMethed.createAssetIssue(dev001Address, tokenName, TotalSupply, 1,
        10000, start, end, 1, description, url, 100000L, 100000L,
        1L, 1L, dev001Key, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    assetAccountId = PublicMethed.queryAccount(dev001Address, blockingStubFull).getAssetIssuedID();
    logger.info("The token name: " + tokenName);
    logger.info("The token ID: " + assetAccountId.toStringUtf8());

    Long devAssetCountBefore = PublicMethed
        .getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);

    logger.info("before AssetId: " + assetAccountId.toStringUtf8() + ", devAssetCountBefore: "
        + devAssetCountBefore);

    String filePath = "./src/test/resources/soliditycode/contractTrcToken067.sol";
    String contractName = "transferTokenContract";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    String transferTokenTxid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            assetAccountId.toStringUtf8(), 100, null, dev001Key,
            dev001Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(transferTokenTxid, blockingStubFull);
    logger.info("Deploy energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    if (transferTokenTxid == null || infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage());
    }

    transferTokenContractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethed.getContract(transferTokenContractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    Long devAssetCountAfter = PublicMethed
        .getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);

    logger.info("after AssetId: " + assetAccountId.toStringUtf8() + ", devAssetCountAfter: "
        + devAssetCountAfter);

    Long contractAssetCount = PublicMethed.getAssetIssueValue(transferTokenContractAddress,
        assetAccountId, blockingStubFull);
    logger.info("Contract has AssetId: " + assetAccountId.toStringUtf8() + ", Count: "
        + contractAssetCount);

    Assert.assertEquals(Long.valueOf(100), Long.valueOf(devAssetCountBefore - devAssetCountAfter));
    Assert.assertEquals(Long.valueOf(100), contractAssetCount);
  }

  @Test(enabled = true, description = "TransferToken with 0 tokenValue, "
      + "and not existed tokenId, deploy receive contract")
  public void test02DeployRevContract() {
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress,
        PublicMethed.getFreezeBalanceCount(dev001Address, dev001Key, 50000L,
            blockingStubFull), 0, 1,
        ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));

    Long devAssetCountBefore = PublicMethed
        .getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);
    logger.info("before AssetId: " + assetAccountId.toStringUtf8() + ", devAssetCountBefore: "
        + devAssetCountBefore);

    String filePath = "./src/test/resources/soliditycode/contractTrcToken067.sol";
    String contractName = "Result";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    String recieveTokenTxid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            0L, 100, 1000, assetAccountId.toStringUtf8(),
            100, null, dev001Key, dev001Address, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(recieveTokenTxid, blockingStubFull);
    logger.info("Deploy energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    if (recieveTokenTxid == null || infoById.get().getResultValue() != 0) {
      Assert.fail("deploy receive failed with message: " + infoById.get().getResMessage());
    }

    Long devAssetCountAfter = PublicMethed
        .getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);
    logger.info("after AssetId: " + assetAccountId.toStringUtf8() + ", devAssetCountAfter: "
        + devAssetCountAfter);

    resultContractAddress = infoById.get().getContractAddress().toByteArray();

    SmartContract smartContract = PublicMethed
        .getContract(resultContractAddress, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    Long contractAssetCount = PublicMethed.getAssetIssueValue(resultContractAddress,
        assetAccountId, blockingStubFull);
    logger.info("Contract has AssetId: " + assetAccountId.toStringUtf8() + ", Count: "
        + contractAssetCount);

    Assert.assertEquals(Long.valueOf(100), Long.valueOf(devAssetCountBefore - devAssetCountAfter));
    Assert.assertEquals(Long.valueOf(100), contractAssetCount);
  }

  @Test(enabled = true, description = "TransferToken with 0 tokenValue, "
      + "and not existed tokenId, trigger transfer contract")
  public void test03TriggerContract() {
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress,
        PublicMethed.getFreezeBalanceCount(user001Address, user001Key, 50000L,
            blockingStubFull), 0, 1,
        ByteString.copyFrom(user001Address), testKey002, blockingStubFull));

    Assert.assertTrue(PublicMethed.transferAsset(user001Address,
        assetAccountId.toByteArray(), 10L, dev001Address, dev001Key, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Long transferAssetBefore = PublicMethed.getAssetIssueValue(transferTokenContractAddress,
        assetAccountId, blockingStubFull);
    logger.info("before trigger, transferTokenContractAddress has AssetId "
        + assetAccountId.toStringUtf8() + ", Count is " + transferAssetBefore);

    Long receiveAssetBefore = PublicMethed.getAssetIssueValue(resultContractAddress, assetAccountId,
        blockingStubFull);
    logger.info("before trigger, resultContractAddress has AssetId "
        + assetAccountId.toStringUtf8() + ", Count is " + receiveAssetBefore);

    String tokenId = Long.toString(Long.MAX_VALUE);
    Long tokenValue = Long.valueOf(0);
    Long callValue = Long.valueOf(0);

    String param = "\"" + Base58.encode58Check(resultContractAddress)
        + "\",\"" + tokenValue + "\"," + tokenId;

    String triggerTxid = PublicMethed.triggerContract(transferTokenContractAddress,
        "transferTokenTest(address,uint256,trcToken)", param, false, callValue,
        1000000000L, assetAccountId.toStringUtf8(), 2, user001Address, user001Key,
        blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    logger.info("Trigger energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    if (triggerTxid == null || infoById.get().getResultValue() != 0) {
      Assert.fail("transaction failed with message: " + infoById.get().getResMessage());
    }

    TransactionInfo transactionInfo = infoById.get();
    logger.info(
        "the value: " + PublicMethed.getStrings(transactionInfo.getLogList().get(0).getData()
            .toByteArray()));

    List<String> retList = PublicMethed.getStrings(transactionInfo.getLogList().get(0)
        .getData().toByteArray());

    Long msgId = ByteArray.toLong(ByteArray.fromHexString(retList.get(0)));
    Long msgTokenValue = ByteArray.toLong(ByteArray.fromHexString(retList.get(1)));
    Long msgCallValue = ByteArray.toLong(ByteArray.fromHexString(retList.get(2)));

    logger.info("msgId: " + msgId);
    logger.info("msgTokenValue: " + msgTokenValue);
    logger.info("msgCallValue: " + msgCallValue);

    Assert.assertEquals(tokenId, msgId.toString());
    Assert.assertEquals(tokenValue, msgTokenValue);
    Assert.assertEquals(callValue, msgCallValue);

    Long transferAssetAfter = PublicMethed.getAssetIssueValue(transferTokenContractAddress,
        assetAccountId, blockingStubFull);
    logger.info("after trigger, transferTokenContractAddress has AssetId "
        + assetAccountId.toStringUtf8() + ", transferAssetAfter is " + transferAssetAfter);

    Long receiveAssetAfter = PublicMethed.getAssetIssueValue(resultContractAddress,
        assetAccountId, blockingStubFull);
    logger.info("after trigger, resultContractAddress has AssetId "
        + assetAccountId.toStringUtf8() + ", receiveAssetAfter is " + receiveAssetAfter);

    Assert.assertEquals(receiveAssetAfter - receiveAssetBefore,
        transferAssetBefore + 2L - transferAssetAfter);
  }

  @Test(enabled = true, description = "TransferToken with 0 tokenValue, "
      + "and not existed tokenId, get tokenBalance")
  public void test04TriggerTokenBalanceContract() {
    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(user001Address, 1000_000_000L,
        0, 1, user001Key, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String param = "\"" + Base58.encode58Check(resultContractAddress) + "\",\""
        + assetAccountId.toStringUtf8() + "\"";

    String triggerTxid = PublicMethed.triggerContract(transferTokenContractAddress,
        "getTokenBalnce(address,trcToken)",
        param, false, 0, 1000000000L, user001Address,
        user001Key, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed.getTransactionInfoById(triggerTxid,
        blockingStubFull);
    logger.info("Trigger energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    if (triggerTxid == null || infoById.get().getResultValue() != 0) {
      Assert.fail("transaction failed with message: " + infoById.get().getResMessage());
    }

    logger.info("the receivercontract token: " + ByteArray
        .toLong(infoById.get().getContractResult(0).toByteArray()));
    Long assetIssueCount = PublicMethed.getAssetIssueValue(resultContractAddress, assetAccountId,
        blockingStubFull);
    logger.info("the receivercontract token(getaccount): " + assetIssueCount);
    Assert.assertTrue(assetIssueCount == ByteArray
        .toLong(ByteArray.fromHexString(
            ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray()))));

  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed.freedResource(dev001Address, dev001Key, fromAddress, blockingStubFull);
    PublicMethed.freedResource(user001Address, user001Key, fromAddress, blockingStubFull);
    PublicMethed.unFreezeBalance(fromAddress, testKey002, 1, dev001Address, blockingStubFull);
    PublicMethed.unFreezeBalance(fromAddress, testKey002, 0, dev001Address, blockingStubFull);
    PublicMethed.unFreezeBalance(fromAddress, testKey002, 1, user001Address, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


