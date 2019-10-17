package stest.tron.wallet.contract.linkage;

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
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractLinkage004 {

  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey003);
  String contractName;
  String code;
  String abi;
  Long currentFee;
  Account info;
  Long beforeBalance;
  Long beforeNetLimit;
  Long beforeFreeNetLimit;
  Long beforeFreeNetUsed;
  Long beforeNetUsed;
  Long beforeEnergyLimit;
  Long beforeEnergyUsed;
  Long afterBalance;
  Long afterNetLimit;
  Long afterFreeNetLimit;
  Long afterFreeNetUsed;
  Long afterNetUsed;
  Long afterEnergyLimit;
  Long afterEnergyUsed;
  Long energyUsed;
  Long netUsed;
  Long energyFee;
  Long fee;
  Long energyUsageTotal;
  Long netFee;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] linkage004Address = ecKey1.getAddress();
  String linkage004Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

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
    PublicMethed.printAddress(linkage004Key);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
  }

  @Test(enabled = true)
  public void test1GetTransactionInfoById() {
    ecKey1 = new ECKey(Utils.getRandom());
    linkage004Address = ecKey1.getAddress();
    linkage004Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    Assert.assertTrue(PublicMethed.sendcoin(linkage004Address, 2000000000000L, fromAddress,
        testKey003, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalance(linkage004Address, 10000000L,
        3, linkage004Key, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(linkage004Address,
        blockingStubFull);
    info = PublicMethed.queryAccount(linkage004Address, blockingStubFull);
    beforeBalance = info.getBalance();
    beforeEnergyLimit = resourceInfo.getEnergyLimit();
    beforeEnergyUsed = resourceInfo.getEnergyUsed();
    beforeFreeNetLimit = resourceInfo.getFreeNetLimit();
    beforeNetLimit = resourceInfo.getNetLimit();
    beforeNetUsed = resourceInfo.getNetUsed();
    beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyLimit:" + beforeEnergyLimit);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeFreeNetLimit:" + beforeFreeNetLimit);
    logger.info("beforeNetLimit:" + beforeNetLimit);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    String filePath = "./src/test/resources/soliditycode/contractLinkage004.sol";
    String contractName = "divideIHaveArgsReturnStorage";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    //use freezeBalanceGetNet,Balance .No freezeBalanceGetenergy
    String txid = PublicMethed.deployContractAndGetTransactionInfoById(contractName, abi, code,
        "", maxFeeLimit, 0L, 50, null, linkage004Key, linkage004Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull1);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    fee = infoById.get().getFee();
    currentFee = fee;
    energyFee = infoById.get().getReceipt().getEnergyFee();
    netUsed = infoById.get().getReceipt().getNetUsage();
    energyUsed = infoById.get().getReceipt().getEnergyUsage();
    netFee = infoById.get().getReceipt().getNetFee();
    logger.info("energyUsageTotal:" + energyUsageTotal);
    logger.info("fee:" + fee);
    logger.info("energyFee:" + energyFee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);

    Account infoafter = PublicMethed.queryAccount(linkage004Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(linkage004Address,
        blockingStubFull1);
    afterBalance = infoafter.getBalance();
    afterEnergyLimit = resourceInfoafter.getEnergyLimit();
    afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    afterFreeNetLimit = resourceInfoafter.getFreeNetLimit();
    afterNetLimit = resourceInfoafter.getNetLimit();
    afterNetUsed = resourceInfoafter.getNetUsed();
    afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyLimit:" + afterEnergyLimit);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterFreeNetLimit:" + afterFreeNetLimit);
    logger.info("afterNetLimit:" + afterNetLimit);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("---------------:");
    Assert.assertTrue(infoById.isPresent());
    Assert.assertTrue((beforeBalance - fee) == afterBalance);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterEnergyUsed == 0);
    Assert.assertTrue(afterFreeNetUsed > 0);
  }

  @Test(enabled = true)
  public void test2FeeLimitIsTooSmall() {
    //When the fee limit is only short with 1 sun,failed.use freezeBalanceGetNet.
    maxFeeLimit = currentFee - 1L;
    AccountResourceMessage resourceInfo1 = PublicMethed.getAccountResource(linkage004Address,
        blockingStubFull);
    Account info1 = PublicMethed.queryAccount(linkage004Address, blockingStubFull);
    Long beforeBalance1 = info1.getBalance();
    Long beforeEnergyLimit1 = resourceInfo1.getEnergyLimit();
    Long beforeEnergyUsed1 = resourceInfo1.getEnergyUsed();
    Long beforeFreeNetLimit1 = resourceInfo1.getFreeNetLimit();
    Long beforeNetLimit1 = resourceInfo1.getNetLimit();
    Long beforeNetUsed1 = resourceInfo1.getNetUsed();
    Long beforeFreeNetUsed1 = resourceInfo1.getFreeNetUsed();
    logger.info("beforeBalance1:" + beforeBalance1);
    logger.info("beforeEnergyLimit1:" + beforeEnergyLimit1);
    logger.info("beforeEnergyUsed1:" + beforeEnergyUsed1);
    logger.info("beforeFreeNetLimit1:" + beforeFreeNetLimit1);
    logger.info("beforeNetLimit1:" + beforeNetLimit1);
    logger.info("beforeNetUsed1:" + beforeNetUsed1);
    logger.info("beforeFreeNetUsed1:" + beforeFreeNetUsed1);

    String filePath = "./src/test/resources/soliditycode/contractLinkage004.sol";
    String contractName = "divideIHaveArgsReturnStorage";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    String txid = PublicMethed.deployContractAndGetTransactionInfoById(contractName, abi, code,
        "", maxFeeLimit, 0L, 50, null, linkage004Key, linkage004Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull1);

    Optional<TransactionInfo> infoById1 = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Long energyUsageTotal1 = infoById1.get().getReceipt().getEnergyUsageTotal();
    Long fee1 = infoById1.get().getFee();
    Long energyFee1 = infoById1.get().getReceipt().getEnergyFee();
    Long netUsed1 = infoById1.get().getReceipt().getNetUsage();
    Long energyUsed1 = infoById1.get().getReceipt().getEnergyUsage();
    Long netFee1 = infoById1.get().getReceipt().getNetFee();
    logger.info("energyUsageTotal1:" + energyUsageTotal1);
    logger.info("fee1:" + fee1);
    logger.info("energyFee1:" + energyFee1);
    logger.info("netUsed1:" + netUsed1);
    logger.info("energyUsed1:" + energyUsed1);
    logger.info("netFee1:" + netFee1);

    Account infoafter1 = PublicMethed.queryAccount(linkage004Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter1 = PublicMethed.getAccountResource(linkage004Address,
        blockingStubFull1);
    Long afterBalance1 = infoafter1.getBalance();
    Long afterEnergyLimit1 = resourceInfoafter1.getEnergyLimit();
    Long afterEnergyUsed1 = resourceInfoafter1.getEnergyUsed();
    Long afterFreeNetLimit1 = resourceInfoafter1.getFreeNetLimit();
    Long afterNetLimit1 = resourceInfoafter1.getNetLimit();
    Long afterNetUsed1 = resourceInfoafter1.getNetUsed();
    Long afterFreeNetUsed1 = resourceInfoafter1.getFreeNetUsed();
    logger.info("afterBalance1:" + afterBalance1);
    logger.info("afterEnergyLimit1:" + afterEnergyLimit1);
    logger.info("afterEnergyUsed1:" + afterEnergyUsed1);
    logger.info("afterFreeNetLimit1:" + afterFreeNetLimit1);
    logger.info("afterNetLimit1:" + afterNetLimit1);
    logger.info("afterNetUsed1:" + afterNetUsed1);
    logger.info("afterFreeNetUsed1:" + afterFreeNetUsed1);

    Assert.assertTrue((beforeBalance1 - fee1) == afterBalance1);
    Assert.assertTrue(infoById1.get().getResultValue() == 1);
    Assert.assertTrue(energyUsageTotal1 > 0);
    Assert.assertTrue(afterEnergyUsed1 == 0);
    Assert.assertTrue(beforeNetUsed1 < afterNetUsed1);

    //When the fee limit is just ok.use energyFee,freezeBalanceGetNet,balance change.
    maxFeeLimit = currentFee;
    AccountResourceMessage resourceInfo2 = PublicMethed.getAccountResource(linkage004Address,
        blockingStubFull);
    Account info2 = PublicMethed.queryAccount(linkage004Address, blockingStubFull);
    Long beforeBalance2 = info2.getBalance();
    Long beforeEnergyLimit2 = resourceInfo2.getEnergyLimit();
    Long beforeEnergyUsed2 = resourceInfo2.getEnergyUsed();
    Long beforeFreeNetLimit2 = resourceInfo2.getFreeNetLimit();
    Long beforeNetLimit2 = resourceInfo2.getNetLimit();
    Long beforeNetUsed2 = resourceInfo2.getNetUsed();
    Long beforeFreeNetUsed2 = resourceInfo2.getFreeNetUsed();
    logger.info("beforeBalance2:" + beforeBalance2);
    logger.info("beforeEnergyLimit2:" + beforeEnergyLimit2);
    logger.info("beforeEnergyUsed2:" + beforeEnergyUsed2);
    logger.info("beforeFreeNetLimit2:" + beforeFreeNetLimit2);
    logger.info("beforeNetLimit2:" + beforeNetLimit2);
    logger.info("beforeNetUsed2:" + beforeNetUsed2);
    logger.info("beforeFreeNetUsed2:" + beforeFreeNetUsed2);
    txid = PublicMethed.deployContractAndGetTransactionInfoById(contractName, abi, code,
        "", maxFeeLimit, 0L, 50, null, linkage004Key, linkage004Address, blockingStubFull);
    //logger.info("testFeeLimitIsTooSmall, the txid is " + txid);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById2 = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Long energyUsageTotal2 = infoById2.get().getReceipt().getEnergyUsageTotal();
    Long fee2 = infoById2.get().getFee();
    Long energyFee2 = infoById2.get().getReceipt().getEnergyFee();
    Long netUsed2 = infoById2.get().getReceipt().getNetUsage();
    Long energyUsed2 = infoById2.get().getReceipt().getEnergyUsage();
    Long netFee2 = infoById2.get().getReceipt().getNetFee();
    logger.info("energyUsageTotal2:" + energyUsageTotal2);
    logger.info("fee2:" + fee2);
    logger.info("energyFee2:" + energyFee2);
    logger.info("netUsed2:" + netUsed2);
    logger.info("energyUsed2:" + energyUsed2);
    logger.info("netFee2:" + netFee2);
    Account infoafter2 = PublicMethed.queryAccount(linkage004Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter2 = PublicMethed.getAccountResource(linkage004Address,
        blockingStubFull1);
    Long afterBalance2 = infoafter2.getBalance();
    Long afterEnergyLimit2 = resourceInfoafter2.getEnergyLimit();
    Long afterEnergyUsed2 = resourceInfoafter2.getEnergyUsed();
    Long afterFreeNetLimit2 = resourceInfoafter2.getFreeNetLimit();
    Long afterNetLimit2 = resourceInfoafter2.getNetLimit();
    Long afterNetUsed2 = resourceInfoafter2.getNetUsed();
    Long afterFreeNetUsed2 = resourceInfoafter2.getFreeNetUsed();
    logger.info("afterBalance2:" + afterBalance2);
    logger.info("afterEnergyLimit2:" + afterEnergyLimit2);
    logger.info("afterEnergyUsed2:" + afterEnergyUsed2);
    logger.info("afterFreeNetLimit2:" + afterFreeNetLimit2);
    logger.info("afterNetLimit2:" + afterNetLimit2);
    logger.info("afterNetUsed2:" + afterNetUsed2);
    logger.info("afterFreeNetUsed2:" + afterFreeNetUsed2);

    Assert.assertTrue(infoById2.get().getResultValue() == 0);
    Assert.assertTrue(infoById2.get().getReceipt().getEnergyUsageTotal() > 0);
    Assert.assertTrue((beforeBalance2 - fee2) == afterBalance2);
    Assert.assertTrue((beforeNetUsed2 + netUsed2) >= afterNetUsed2);

    currentFee = fee2;
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}


