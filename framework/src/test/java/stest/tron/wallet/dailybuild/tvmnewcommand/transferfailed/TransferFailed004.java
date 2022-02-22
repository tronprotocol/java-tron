package stest.tron.wallet.dailybuild.tvmnewcommand.transferfailed;

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
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class TransferFailed004 {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  byte[] contractAddress = null;
  byte[] contractAddress2 = null;
  byte[] contractAddress3 = null;
  private static final long now = System.currentTimeMillis();
  private static final long TotalSupply = 10000000L;
  private static ByteString assetAccountId = null;
  private static ByteString assetAccountId2 = null;
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress3 = ecKey3.getAddress();
  String contractExcKey3 = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] nonexistentAddress = ecKey2.getAddress();
  long energyUsageTotal = 0;
  long energyUsageTotal2 = 0;
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelSolidity = null;
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);



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

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);

    Assert.assertTrue(PublicMethed
        .sendcoin(contractExcAddress, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    long start = System.currentTimeMillis() + 2000;
    long end = System.currentTimeMillis() + 1000000000;

    //Create a new AssetIssue success.
    Assert
        .assertTrue(PublicMethed.createAssetIssue(contractExcAddress, tokenName, TotalSupply, 1,
            10000, start, end, 1, description, url, 100000L,
            100000L, 1L, 1L, contractExcKey, blockingStubFull));

    assetAccountId = PublicMethed.queryAccount(contractExcAddress, blockingStubFull)
        .getAssetIssuedID();

  }

  @Test(enabled = true, description = "Suicide nonexistent target")
  public void test1SuicideNonexistentTarget() {
    Assert.assertTrue(PublicMethed
        .sendcoin(contractExcAddress, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/TransferFailed001.sol";
    String contractName = "EnergyOfTransferFailedTest";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    contractAddress = PublicMethed.deployContractFallbackReceive(contractName, abi, code, "",
        maxFeeLimit, 1000000L, 100,1000L, assetAccountId.toStringUtf8(), 1000L,
        null, contractExcKey, contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account info;

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    Long testNetAccountCountBefore = PublicMethed
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
    Long contractAccountCountBefore = PublicMethed
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("testNetAccountCountBefore:" + testNetAccountCountBefore);
    logger.info("contractAccountCountBefore:" + contractAccountCountBefore);
    String txid = "";
    String num = "\"" + Base58.encode58Check(nonexistentAddress) + "\"";

    txid = PublicMethed.triggerContract(contractAddress,
        "testSuicideNonexistentTarget(address)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);

    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    final Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    final Long netFee = infoById.get().getReceipt().getNetFee();
    energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    Long testNetAccountCountAfter = PublicMethed
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
    Long contractAccountCountAfter = PublicMethed
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);
    logger.info("testNetAccountCountAfter:" + testNetAccountCountAfter);
    logger.info("contractAccountCountAfter:" + contractAccountCountAfter);

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertEquals(0, infoById.get().getResultValue());

    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Assert.assertEquals(testNetAccountCountBefore, testNetAccountCountAfter);
    Assert.assertEquals(0L, contractAccountCountAfter.longValue());
    Assert.assertNotEquals(10000000, energyUsageTotal);
    String assetIssueId = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getAssetIssuedID().toStringUtf8();
    logger.info("assetIssueId: " + assetIssueId);
    Assert.assertEquals(0, assetIssueId.length());

    Account nonexistentAddressAccountTrxBalance = PublicMethed
        .queryAccount(nonexistentAddress, blockingStubFull1);
    Assert.assertEquals(1000000L, nonexistentAddressAccountTrxBalance.getBalance());
    Long nonexistentAddressAccountTokenBalance = PublicMethed
        .getAssetIssueValue(nonexistentAddress, assetAccountId, blockingStubFull1);
    Assert.assertEquals(1000L, nonexistentAddressAccountTokenBalance.longValue());
  }

  @Test(enabled = true, description = "Suicide existent target")
  public void test2SuicideExistentTarget() {
    Assert.assertTrue(PublicMethed
        .sendcoin(contractExcAddress, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    String filePath = "src/test/resources/soliditycode/TransferFailed001.sol";
    String contractName = "EnergyOfTransferFailedTest";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    contractAddress2 = PublicMethed.deployContractFallbackReceive(contractName, abi, code, "",
        maxFeeLimit, 1000000L, 100,1000L, assetAccountId.toStringUtf8(),
        1000L,null, contractExcKey, contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account info;

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    Long testNetAccountCountBefore = PublicMethed
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
    Long contractAccountCountBefore = PublicMethed
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("testNetAccountCountBefore:" + testNetAccountCountBefore);
    logger.info("contractAccountCountBefore:" + contractAccountCountBefore);

    Assert.assertTrue(PublicMethed
        .sendcoin(nonexistentAddress, 1000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String txid = "";
    String num = "\"" + Base58.encode58Check(nonexistentAddress) + "\"";
    txid = PublicMethed.triggerContract(contractAddress2,
        "testSuicideNonexistentTarget(address)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    final Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    final Long netFee = infoById.get().getReceipt().getNetFee();
    energyUsageTotal2 = infoById.get().getReceipt().getEnergyUsageTotal();
    Long testNetAccountCountAfter = PublicMethed
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
    Long contractAccountCountAfter = PublicMethed
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal2);
    logger.info("testNetAccountCountAfter:" + testNetAccountCountAfter);
    logger.info("contractAccountCountAfter:" + contractAccountCountAfter);

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    Account contractafter = PublicMethed.queryAccount(contractAddress2, blockingStubFull1);
    long contractBalance = contractafter.getBalance();
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(contractBalance, 0);

    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Assert.assertEquals(testNetAccountCountBefore, testNetAccountCountAfter);
    Assert.assertEquals(0L, contractAccountCountAfter.longValue());
    Assert.assertEquals(energyUsageTotal, energyUsageTotal2);

    Account nonexistentAddressAccountTrxBalance = PublicMethed
        .queryAccount(nonexistentAddress, blockingStubFull1);
    Assert.assertEquals(3000000L, nonexistentAddressAccountTrxBalance.getBalance());
    Long nonexistentAddressAccountTokenBalance = PublicMethed
        .getAssetIssueValue(nonexistentAddress, assetAccountId, blockingStubFull1);
    Assert.assertEquals(2000L, nonexistentAddressAccountTokenBalance.longValue());

  }

  @Test(enabled = true, description = "Suicide nonexistent target, but revert")
  public void test3SuicideNonexistentTargetRevert() {
    Assert.assertTrue(PublicMethed
        .sendcoin(contractExcAddress, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    String filePath = "src/test/resources/soliditycode/TransferFailed001.sol";
    String contractName = "EnergyOfTransferFailedTest";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    contractAddress3 = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    //Assert.assertTrue(PublicMethed
    //.sendcoin(contractAddress, 1000000L, testNetAccountAddress, testNetAccountKey,
    //        blockingStubFull));
    //PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account info;

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    ECKey ecKey2 = new ECKey(Utils.getRandom());
    byte[] nonexistentAddress1 = ecKey2.getAddress();

    String txid = "";
    String num = "\"" + Base58.encode58Check(nonexistentAddress1) + "\"";

    txid = PublicMethed.triggerContract(contractAddress3,
        "testSuicideRevert(address)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);

    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    final Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    final Long netFee = infoById.get().getReceipt().getNetFee();
    energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertEquals(0, infoById.get().getResultValue());

    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Assert.assertTrue(energyUsageTotal < 1000000000L);

    Account nonexistentAddressAccount = PublicMethed
        .queryAccount(nonexistentAddress1, blockingStubFull1);
    Assert.assertEquals(1000000L, nonexistentAddressAccount.getBalance());
  }

  @Test(enabled = true, description = "transfer to a suicide contract address same token")
  public void test4transferToSuicideContractSameToken() {
    Assert.assertTrue(PublicMethed
        .sendcoin(contractExcAddress, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    String filePath = "src/test/resources/soliditycode/TransferFailed001.sol";
    String contractName = "EnergyOfTransferFailedTest";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    final byte[] contractAddress4 = PublicMethed.deployContract(contractName, abi, code, "",
        maxFeeLimit, 1000000L, 100, 1000000000L,
        assetAccountId.toStringUtf8(), 1000L,null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    Account info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    String txid = "";
    String num = "1" + ",\"" + Base58.encode58Check(contractAddress) + "\"";

    txid = PublicMethed.triggerContract(contractAddress4,
        "testTransferTrxNonexistentTarget(uint256,address)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);

    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("infoById:" + infoById);
    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals(contractResult.SUCCESS, infoById.get().getReceipt().getResult());

    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Assert.assertNotEquals(10000000, energyUsageTotal);

    Account nonexistentAddressAccount = PublicMethed
        .queryAccount(contractAddress, blockingStubFull1);
    Assert.assertEquals(1L, nonexistentAddressAccount.getBalance());

    num =
        "\"1" + "\",\"" + Base58.encode58Check(contractAddress) + "\",\"" + assetAccountId
            .toStringUtf8() + "\"";
    txid = PublicMethed.triggerContract(contractAddress4,
        "testTransferTokenNonexistentTarget(uint256,address,trcToken)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());

    Long contractAccountCountAfter = PublicMethed
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    Assert.assertEquals(1L, contractAccountCountAfter.longValue());
  }

  @Test(enabled = true, description = "transfer to a suicide contract address different token")
  public void test5transferToSuicideContractDifferentToken() {
    // create different token
    Assert.assertTrue(PublicMethed
        .sendcoin(contractExcAddress3, 10000_000_000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    long start = System.currentTimeMillis() + 2000;
    long end = System.currentTimeMillis() + 1000000000;
    //Create a new AssetIssue success.
    Assert
        .assertTrue(PublicMethed.createAssetIssue(contractExcAddress3, tokenName, TotalSupply, 1,
            10000, start, end, 1, description, url, 100000L,
            100000L, 1L, 1L, contractExcKey3, blockingStubFull));
    assetAccountId2 = PublicMethed.queryAccount(contractExcAddress3, blockingStubFull)
        .getAssetIssuedID();

    Assert.assertTrue(PublicMethed
        .sendcoin(contractExcAddress3, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    String filePath = "src/test/resources/soliditycode/TransferFailed001.sol";
    String contractName = "EnergyOfTransferFailedTest";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    final byte[] contractAddress4 = PublicMethed.deployContract(contractName, abi, code, "",
        maxFeeLimit, 1000000L, 100, 1000000000L,
        assetAccountId2.toStringUtf8(), 1000L,null, contractExcKey3,
        contractExcAddress3, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account info;
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(contractExcAddress3,
        blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey3, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    Account nonexistentAddressAccount = PublicMethed
        .queryAccount(contractAddress, blockingStubFull1);
    Assert.assertEquals(1L, nonexistentAddressAccount.getBalance());

    String num =
        "\"1" + "\",\"" + Base58.encode58Check(contractAddress) + "\",\"" + assetAccountId2
            .toStringUtf8() + "\"";
    String txid = PublicMethed.triggerContract(contractAddress4,
        "testTransferTokenNonexistentTarget(uint256,address,trcToken)", num, false,
        0, maxFeeLimit, contractExcAddress3, contractExcKey3, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());

    Long contractAccountCountTokenBalance = PublicMethed
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    Assert.assertEquals(1L, contractAccountCountTokenBalance.longValue());
    Long contractAccountCountTokenBalance2 = PublicMethed
        .getAssetIssueValue(contractAddress, assetAccountId2, blockingStubFull);
    Assert.assertEquals(1L, contractAccountCountTokenBalance2.longValue());

    String assetIssueId = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getAssetIssuedID().toStringUtf8();
    logger.info("assetIssueId: " + assetIssueId);
    Assert.assertEquals(0, assetIssueId.length());
  }


  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed
        .freedResource(contractExcAddress, contractExcKey, testNetAccountAddress, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}
