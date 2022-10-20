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
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class TransferFailed008 {

  private static final long now = System.currentTimeMillis();
  private static final long TotalSupply = 10000000L;
  private static ByteString assetAccountId = null;
  private static ByteString assetAccountId2 = null;
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");
  byte[] contractAddress = null;
  byte[] contractAddress2 = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress3 = ecKey3.getAddress();
  String contractExcKey3 = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] nonexistentAddress = ecKey2.getAddress();
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
        .sendcoin(contractExcAddress, 10000_000_000L, testNetAccountAddress, testNetAccountKey,
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
    System.out.println("assetAccountId:" + assetAccountId.toStringUtf8());

    Long testNetAccountCountBefore = PublicMethed
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
    logger.info("testNetAccountCountBefore:" + testNetAccountCountBefore);
  }


  @Test(enabled = false, description = "TransferToken to old contractAddress")
  public void test1TransferToOldContractAddress() {
    String filePath = "src/test/resources/soliditycode/accountAssert.sol";
    String contractName = "transferTokenTestB";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, 1000000000L,
        assetAccountId.toStringUtf8(), 1000L, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long contractAccountTokenBalance = PublicMethed
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    Assert.assertEquals(1000L, contractAccountTokenBalance.longValue());

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
    // TUtbvvfggwQLrDZCNqYpfieCvCaKgKk5k9 selfdestruct contractAddress
    // TQ1sSomxqmgqKiGqL3Lt8iybHt28FvUTwN exist accountAddress have token
    // TWvKUjxH37F9BoeBrdD1hhWf7Es4CDTsRP exist contractAddress haven't token
    // TKK8PhmACsJVX9T7Jkwr2QuWmhB8LjvwUW exist accountAddress haven't token
    // v4.1.2 contract address ----Manual input
    String oldContractAddress = "TV1ExzvFmSTMj67sxnzHrkZmjpsG5QWSne";
    String num =
        "\"" + oldContractAddress + "\",\"1\",\"" + assetAccountId.toStringUtf8() + "\"";
    txid = PublicMethed.triggerContract(contractAddress,
        "transferTokenTest(address,uint256,trcToken)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infoById:" + infoById);
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
    Long testNetAccountCountAfter = PublicMethed
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
    Long contractAccountCountAfter = PublicMethed
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("testNetAccountCountAfter:" + testNetAccountCountAfter);
    logger.info("contractAccountCountAfter:" + contractAccountCountAfter);

    Assert.assertEquals(0, infoById.get().getResultValue());

    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertEquals(testNetAccountCountBefore, testNetAccountCountAfter);
    Assert.assertEquals(contractAccountCountBefore - 1, contractAccountCountAfter.longValue());

    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Assert.assertNotEquals(10000000, energyUsageTotal);

    Long oldContractAddressAccount = PublicMethed.getAssetIssueValue(
        PublicMethed.decode58Check(oldContractAddress), assetAccountId, blockingStubFull1);
    Assert.assertEquals(1L, oldContractAddressAccount.longValue());
  }

  @Test(enabled = true, description = "TransferToken to new contract")
  public void test1TransferToNewContract() {
    String filePath = "src/test/resources/soliditycode/accountAssert.sol";
    String contractName = "transferTokenTestB";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, 1000000000L,
        assetAccountId.toStringUtf8(), 1000L, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long contractAccountTokenBalance = PublicMethed
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    Assert.assertEquals(1000L, contractAccountTokenBalance.longValue());

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
    String num = "\"1" + "\",\"" + assetAccountId.toStringUtf8() + "\"";
    txid = PublicMethed.triggerContract(contractAddress,
        "createContractTest(uint256,trcToken)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infoById:" + infoById);
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
    Long testNetAccountCountAfter = PublicMethed
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
    Long contractAccountCountAfter = PublicMethed
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("testNetAccountCountAfter:" + testNetAccountCountAfter);
    logger.info("contractAccountCountAfter:" + contractAccountCountAfter);

    Assert.assertEquals(0, infoById.get().getResultValue());

    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertEquals(testNetAccountCountBefore, testNetAccountCountAfter);
    Assert.assertEquals(contractAccountCountBefore - 1, contractAccountCountAfter.longValue());

    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Assert.assertNotEquals(10000000, energyUsageTotal);

    String addressHex =
        "41" + ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())
            .substring(24);
    logger.info("address_hex: " + addressHex);
    byte[] contractAddressA = ByteArray.fromHexString(addressHex);
    logger.info("contractAddressA: " + Base58.encode58Check(contractAddressA));
    Long nonexistentAddressAccount = PublicMethed
        .getAssetIssueValue(contractAddressA, assetAccountId, blockingStubFull1);
    Assert.assertEquals(1L, nonexistentAddressAccount.longValue());
  }

  @Test(enabled = true, description = "TransferToken nonexistent target in constructor")
  public void test2TransferToNonexistentTargetInConstructor() {
    Assert.assertTrue(PublicMethed
        .sendcoin(contractExcAddress, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Long testNetAccountCountBefore = PublicMethed
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
    Long contractAccountCountBefore = PublicMethed
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("testNetAccountCountBefore:" + testNetAccountCountBefore);
    logger.info("contractAccountCountBefore:" + contractAccountCountBefore);

    String filePath = "src/test/resources/soliditycode/accountAssert.sol";
    String contractName = "transferTokenTestA";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    String constructorStr = "constructor(address,uint256,trcToken)";
    String argsStr =
        "\"" + Base58.encode58Check(nonexistentAddress) + "\",\"1\",\"" + assetAccountId
            .toStringUtf8() + "\"";

    String deplTxid = PublicMethed.deployContractWithConstantParame(contractName, abi, code,
        constructorStr, argsStr, "", maxFeeLimit, 1000000L, 100,1000L,
        assetAccountId.toStringUtf8(), 1000L, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info = PublicMethed
        .getTransactionInfoById(deplTxid, blockingStubFull);
    Assert.assertTrue(info.get().getResultValue() == 0);
    contractAddress = info.get().getContractAddress().toByteArray();

    Long testNetAccountCountAfter = PublicMethed
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
    Long contractAccountCountAfter = PublicMethed
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("testNetAccountCountAfter:" + testNetAccountCountAfter);
    logger.info("contractAccountCountAfter:" + contractAccountCountAfter);

    Assert.assertEquals(testNetAccountCountBefore.longValue() - 1000L,
        testNetAccountCountAfter.longValue());
    Assert.assertEquals(999L, contractAccountCountAfter.longValue());

    Account nonexistentAddressAccountTrxBalance = PublicMethed
        .queryAccount(nonexistentAddress, blockingStubFull1);
    Assert.assertEquals(0L, nonexistentAddressAccountTrxBalance.getBalance());
    Long nonexistentAddressAccountTokenBalance = PublicMethed
        .getAssetIssueValue(nonexistentAddress, assetAccountId, blockingStubFull1);
    Assert.assertEquals(1L, nonexistentAddressAccountTokenBalance.longValue());
  }

  @Test(enabled = true, description = "TransferToken existent target in constructor")
  public void test3TransferToExistentTargetInConstructor() {
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
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Long testNetAccountCountBefore = PublicMethed
        .getAssetIssueValue(contractExcAddress3, assetAccountId2, blockingStubFull);
    Long contractAccountCountBefore = PublicMethed
        .getAssetIssueValue(contractAddress, assetAccountId2, blockingStubFull);
    logger.info("testNetAccountCountBefore:" + testNetAccountCountBefore);
    logger.info("contractAccountCountBefore:" + contractAccountCountBefore);

    String filePath = "src/test/resources/soliditycode/accountAssert.sol";
    String contractName = "transferTokenTestA";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    String constructorStr = "constructor(address,uint256,trcToken)";
    String argsStr =
        "\"" + Base58.encode58Check(nonexistentAddress) + "\",\"1\",\"" + assetAccountId2
            .toStringUtf8() + "\"";

    String deplTxid = PublicMethed.deployContractWithConstantParame(contractName, abi, code,
        constructorStr, argsStr, "", maxFeeLimit, 1000000L, 100,
        1000L, assetAccountId2.toStringUtf8(), 1000L, null, contractExcKey3,
        contractExcAddress3, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info = PublicMethed
        .getTransactionInfoById(deplTxid, blockingStubFull);
    Assert.assertTrue(info.get().getResultValue() == 0);
    contractAddress = info.get().getContractAddress().toByteArray();

    Long testNetAccountCountAfter = PublicMethed
        .getAssetIssueValue(contractExcAddress3, assetAccountId2, blockingStubFull);
    Long contractAccountCountAfter = PublicMethed
        .getAssetIssueValue(contractAddress, assetAccountId2, blockingStubFull);
    logger.info("testNetAccountCountAfter:" + testNetAccountCountAfter);
    logger.info("contractAccountCountAfter:" + contractAccountCountAfter);

    Assert.assertEquals(testNetAccountCountBefore.longValue() - 1000L,
        testNetAccountCountAfter.longValue());
    Assert.assertEquals(999L, contractAccountCountAfter.longValue());

    Account nonexistentAddressAccountTrxBalance = PublicMethed
        .queryAccount(nonexistentAddress, blockingStubFull1);
    Assert.assertEquals(0L, nonexistentAddressAccountTrxBalance.getBalance());
    Long nonexistentAddressAccountTokenBalance = PublicMethed
        .getAssetIssueValue(nonexistentAddress, assetAccountId, blockingStubFull1);
    Assert.assertEquals(1L, nonexistentAddressAccountTokenBalance.longValue());
    Long nonexistentAddressAccountTokenBalance2 = PublicMethed
        .getAssetIssueValue(nonexistentAddress, assetAccountId2, blockingStubFull1);
    Assert.assertEquals(1L, nonexistentAddressAccountTokenBalance2.longValue());
  }

  @Test(enabled = true, description = "TransferToken existent target in constructor")
  public void test4GetTokenBalanceInConstructor() {
    Assert.assertTrue(PublicMethed
        .sendcoin(contractExcAddress, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String filePath = "src/test/resources/soliditycode/accountAssert.sol";
    String contractName = "transferTokenTestC";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    String constructorStr = "constructor(trcToken)";
    String argsStr = "\"" + assetAccountId.toStringUtf8() + "\"";

    String deplTxid = PublicMethed.deployContractWithConstantParame(contractName, abi, code,
        constructorStr, argsStr, "", maxFeeLimit, 1000000L, 100,1000L,
        "0", 0L, null, contractExcKey, contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info = PublicMethed
        .getTransactionInfoById(deplTxid, blockingStubFull);
    Assert.assertTrue(info.get().getResultValue() == 0);
    contractAddress = info.get().getContractAddress().toByteArray();

    Long contractAccountCountAfter = PublicMethed
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("contractAccountCountAfter:" + contractAccountCountAfter);

    Assert.assertEquals(0L, contractAccountCountAfter.longValue());
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
