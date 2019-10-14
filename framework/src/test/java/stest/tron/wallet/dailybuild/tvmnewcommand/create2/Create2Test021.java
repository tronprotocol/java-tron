package stest.tron.wallet.dailybuild.tvmnewcommand.create2;

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
public class Create2Test021 {

  private static final long now = System.currentTimeMillis();
  private static final String name = "Asset008_" + Long.toString(now);
  private static final long totalSupply = now;
  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  byte[] contractAddress = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] bytes;
  String description = "just-test";
  String url = "https://github.com/tronprotocol/wallet-cli/";
  ByteString assetAccountId = null;
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] resourceOnwerAddress = ecKey2.getAddress();
  String resourceOnwerKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
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
  private byte[] contractExcAddress = PublicMethed
      .getFinalAddress("9fc9b78370cdeab1bc11ba5e387e5e4f205f17d1957b1bebf4ce6d0330a448a4");
  private String contractExcKey =
      "9fc9b78370cdeab1bc11ba5e387e5e4f205f17d1957b1bebf4ce6d0330a448a4";

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
    PublicMethed.printAddress(contractExcKey);
    PublicMethed.printAddress(resourceOnwerKey);
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

  }

  @Test(enabled = true, description = "TriggerContract a constant function created by create2")
  public void test1TriggerContract() {
    Assert.assertTrue(PublicMethed
        .sendcoin(contractExcAddress, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    Assert.assertTrue(PublicMethed
        .sendcoin(resourceOnwerAddress, 1000000000L + 1024000000L, testNetAccountAddress,
            testNetAccountKey,
            blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    //Create 3 the same name token.
    Long start = System.currentTimeMillis() + 2000;
    Long end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethed.createAssetIssue(resourceOnwerAddress,
        name, totalSupply, 1, 1, start, end, 1, description, url,
        2000L, 2000L, 1L, 1L, resourceOnwerKey, blockingStubFull));
    String filePath = "src/test/resources/soliditycode/create2contractn.sol";
    String contractName = "Factory";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
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

    bytes = ByteArray.fromHexString("416CED4D6BF0AE10676347961BEFB7F47A8664AE36");

    String param2 = "\"" + Base58.encode58Check(contractExcAddress) + "\"";
    String txidn = PublicMethed
        .triggerContract(bytes,
            "testSuicideNonexistentTarget(address)", param2, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed
        .sendcoin(bytes, 1000000L, contractExcAddress, contractExcKey, blockingStubFull));
    //Trigger contract to transfer trx and token.
    Account getAssetIdFromAccount = PublicMethed
        .queryAccount(resourceOnwerAddress, blockingStubFull);
    assetAccountId = getAssetIdFromAccount.getAssetIssuedID();
    Long contractBeforeBalance = PublicMethed.queryAccount(bytes, blockingStubFull).getBalance();

    Assert.assertTrue(
        PublicMethed.transferAsset(bytes, assetAccountId.toByteArray(), 100, resourceOnwerAddress,
            resourceOnwerKey,
            blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account account1 = PublicMethed.queryAccount(bytes, blockingStubFull);
    int typeValue1 = account1.getTypeValue();
    Assert.assertEquals(0, typeValue1);
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(resourceOnwerAddress, 1000000L, 0, 0,
        ByteString.copyFrom(bytes), resourceOnwerKey, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(resourceOnwerAddress, 1000000L, 0, 1,
        ByteString.copyFrom(bytes), resourceOnwerKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    final Long beforeExcAccountBalance = PublicMethed
        .queryAccount(resourceOnwerAddress, blockingStubFull).getBalance();
    Assert.assertTrue(PublicMethed.getAccountResource(bytes, blockingStubFull).getNetLimit() > 0);
    Assert
        .assertTrue(PublicMethed.getAccountResource(bytes, blockingStubFull).getEnergyLimit() > 0);

    String contractName1 = "TestConstract";
    HashMap retMap1 = PublicMethed.getBycodeAbi(filePath, contractName1);
    String code1 = "6080604052600160005534801561001557600080fd5b50d3801561002257600080fd5b50"
        + "d2801561002f57600080fd5b506101fd8061003f6000396000f3fe60806040526004361061005b577"
        + "c01000000000000000000000000000000000000000000000000000000006000350463040821fc8114"
        + "61006057806317b6ad5b1461007f578063cc133e94146100b2578063e5aa3d58146100d5575b60008"
        + "0fd5b61007d6004803603602081101561007657600080fd5b5035610116565b005b61007d60048036"
        + "03602081101561009557600080fd5b503573ffffffffffffffffffffffffffffffffffffffff16610"
        + "147565b61007d600480360360408110156100c857600080fd5b5080359060200135610160565b3480"
        + "156100e157600080fd5b50d380156100ee57600080fd5b50d280156100fb57600080fd5b506101046"
        + "101cb565b60408051918252519081900360200190f35b604051339082156108fc0290839060008181"
        + "81858888f19350505050158015610143573d6000803e3d6000fd5b5050565b8073fffffffffffffff"
        + "fffffffffffffffffffffffff16ff5b3382156108fc0283838015801561017657600080fd5b508067"
        + "80000000000000001115801561018e57600080fd5b5080620f4240101580156101a157600080fd5b5"
        + "0604051600081818185878a8ad09450505050501580156101c6573d6000803e3d6000fd5b50505056"
        + "5b6000548156fea165627a7a72305820485b773c60fed3b76621350dd3da7ecf152a2d37ca02dc195"
        + "d6f8a26aec196850029";
    String abi1 = retMap1.get("abI").toString();
    String txid = "";
    String num = "\"" + code1 + "\"" + "," + 1;
    txid = PublicMethed
        .triggerContract(contractAddress,
            "deploy(bytes,uint256)", num, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertFalse(PublicMethed.freezeBalanceForReceiver(resourceOnwerAddress, 5000000L, 0, 0,
        ByteString.copyFrom(bytes), resourceOnwerKey, blockingStubFull));
    Assert.assertFalse(PublicMethed.freezeBalanceForReceiver(resourceOnwerAddress, 5000000L, 0, 1,
        ByteString.copyFrom(bytes), resourceOnwerKey, blockingStubFull));
    Long afterExcAccountBalance = PublicMethed.queryAccount(resourceOnwerAddress, blockingStubFull)
        .getBalance();
    Assert.assertTrue(PublicMethed.getAccountResource(bytes, blockingStubFull).getNetLimit() == 0);
    Assert
        .assertTrue(PublicMethed.getAccountResource(bytes, blockingStubFull).getEnergyLimit() == 0);
    logger.info("afterExcAccountBalance: " + afterExcAccountBalance);
    logger.info("beforeExcAccountBalance:" + beforeExcAccountBalance);

    Assert.assertTrue(afterExcAccountBalance - beforeExcAccountBalance == 0);

    Assert.assertTrue(PublicMethed.unFreezeBalance(resourceOnwerAddress, resourceOnwerKey,
        0, bytes, blockingStubFull));
    Assert.assertTrue(PublicMethed.unFreezeBalance(resourceOnwerAddress, resourceOnwerKey,
        1, bytes, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long afterUnfreezeBalance = PublicMethed.queryAccount(resourceOnwerAddress, blockingStubFull)
        .getBalance();
    Assert.assertTrue(afterUnfreezeBalance == beforeExcAccountBalance + 1000000L * 2);

    Optional<TransactionInfo> infoById = null;
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

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);

    byte[] returnAddressBytes = infoById.get().getInternalTransactions(0).getTransferToAddress()
        .toByteArray();
    String returnAddress = Base58.encode58Check(returnAddressBytes);
    Assert.assertEquals(Base58.encode58Check(bytes), returnAddress);
    logger.info("returnAddress:" + returnAddress);
    txid = PublicMethed
        .triggerContract(returnAddressBytes,
            "i()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee1 = infoById1.get().getFee();
    Long netUsed1 = infoById1.get().getReceipt().getNetUsage();
    Long energyUsed1 = infoById1.get().getReceipt().getEnergyUsage();
    Long netFee1 = infoById1.get().getReceipt().getNetFee();
    long energyUsageTotal1 = infoById1.get().getReceipt().getEnergyUsageTotal();

    logger.info("fee1:" + fee1);
    logger.info("netUsed1:" + netUsed1);
    logger.info("energyUsed1:" + energyUsed1);
    logger.info("netFee1:" + netFee1);
    logger.info("energyUsageTotal1:" + energyUsageTotal1);

    Account infoafter1 = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter1 = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance1 = infoafter1.getBalance();
    Long afterEnergyUsed1 = resourceInfoafter1.getEnergyUsed();
    Long afterNetUsed1 = resourceInfoafter1.getNetUsed();
    Long afterFreeNetUsed1 = resourceInfoafter1.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance1);
    logger.info("afterEnergyUsed:" + afterEnergyUsed1);
    logger.info("afterNetUsed:" + afterNetUsed1);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed1);

    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance1 + fee1 == afterBalance);
    Assert.assertTrue(afterEnergyUsed + energyUsed1 >= afterEnergyUsed1);
    Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById1.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnnumber);
    Account account = PublicMethed.queryAccount(returnAddressBytes, blockingStubFull);
    int typeValue = account.getTypeValue();
    Assert.assertEquals(2, typeValue);
    Assert.assertEquals(account.getBalance(), 1000000);
  }

  @Test(enabled = true, description = "Create2 contract can transfer trx and token.")
  public void test2TriggerContract() {
    Account accountbefore = PublicMethed.queryAccount(bytes, blockingStubFull);
    int typeValue = accountbefore.getTypeValue();
    Assert.assertEquals(2, typeValue);
    long accountbeforeBalance = accountbefore.getBalance();
    Assert.assertEquals(accountbeforeBalance, 1000000);
    Account contractExcAddressbefore = PublicMethed
        .queryAccount(contractExcAddress, blockingStubFull);
    long contractExcAddressbeforeBalance = contractExcAddressbefore.getBalance();

    String num = "1";

    String txid = PublicMethed
        .triggerContract(bytes,
            "testTransfer(uint256)", num, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> transactionInfoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(transactionInfoById.get().getResultValue() == 0);
    Long fee1 = transactionInfoById.get().getFee();

    Account accountafter = PublicMethed.queryAccount(bytes, blockingStubFull);
    long accountafterBalance = accountafter.getBalance();
    Assert.assertTrue(accountbeforeBalance - 1 == accountafterBalance);

    Account contractExcAddressafter = PublicMethed
        .queryAccount(contractExcAddress, blockingStubFull);
    long contractExcAddressafterBalance = contractExcAddressafter.getBalance();
    Assert.assertTrue(contractExcAddressbeforeBalance + 1 - fee1 == contractExcAddressafterBalance);

    num = "1" + ",\"" + assetAccountId.toStringUtf8() + "\"";
    Long returnAddressBytesAccountCountBefore = PublicMethed
        .getAssetIssueValue(bytes, assetAccountId, blockingStubFull);
    Long contractExcAddressAccountCountBefore = PublicMethed
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
    String txid1 = PublicMethed
        .triggerContract(bytes,
            "testTransferToken(uint256,trcToken)", num, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> transactionInfoById1 = PublicMethed
        .getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(transactionInfoById1.get().getResultValue() == 0);
    Long returnAddressBytesAccountCountAfter = PublicMethed
        .getAssetIssueValue(bytes, assetAccountId, blockingStubFull);

    Long contractExcAddressAccountCountAfter = PublicMethed
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
    Assert.assertTrue(
        returnAddressBytesAccountCountBefore - 1 == returnAddressBytesAccountCountAfter);
    Assert.assertTrue(
        contractExcAddressAccountCountBefore + 1 == contractExcAddressAccountCountAfter);
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
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}