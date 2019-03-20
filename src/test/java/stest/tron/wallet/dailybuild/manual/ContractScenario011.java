package stest.tron.wallet.dailybuild.manual;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.springframework.util.StringUtils;
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
import org.tron.protos.Protocol.SmartContract;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractScenario011 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");


  String kittyCoreAddressAndCut = "";
  byte[] kittyCoreContractAddress = null;
  byte[] saleClockAuctionContractAddress = null;
  byte[] siringClockAuctionContractAddress = null;
  byte[] geneScienceInterfaceContractAddress = null;
  Integer consumeUserResourcePercent = 50;
  String txid = "";
  Optional<TransactionInfo> infoById = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] deployAddress = ecKey1.getAddress();
  String deployKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] triggerAddress = ecKey2.getAddress();
  String triggerKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

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
    PublicMethed.printAddress(deployKey);
    PublicMethed.printAddress(triggerKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    Assert.assertTrue(PublicMethed.sendcoin(deployAddress, 50000000000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(triggerAddress, 50000000000L, fromAddress,
        testKey002, blockingStubFull));
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
  }

  @Test(enabled = true,description = "Deploy Erc721 contract \"Kitty Core\"")
  public void deployErc721KittyCore() {
    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(deployAddress, 100000000L,
        0, 1, deployKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull1);
    Assert.assertTrue(PublicMethed.freezeBalance(deployAddress, 100000000L, 0,
        deployKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull1);
    Assert.assertTrue(PublicMethed.freezeBalance(triggerAddress, 100000000L, 0,
        triggerKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull1);
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(deployAddress,
        blockingStubFull);
    Long cpuLimit = accountResource.getEnergyLimit();
    Long cpuUsage = accountResource.getEnergyUsed();
    Account account = PublicMethed.queryAccount(deployAddress, blockingStubFull);
    logger.info("before balance is " + Long.toString(account.getBalance()));
    logger.info("before cpu limit is " + Long.toString(cpuLimit));
    logger.info("before cpu usage is " + Long.toString(cpuUsage));
    String contractName = "KittyCore";
    String code = Configuration.getByPath("testng.conf")
        .getString("code.code_ContractScenario011_deployErc721KittyCore");
    String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_ContractScenario011_deployErc721KittyCore");
    logger.info("Kitty Core");
    kittyCoreContractAddress = PublicMethed.deployContract(contractName, abi, code, "",
        maxFeeLimit, 0L, consumeUserResourcePercent, null, deployKey,
        deployAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(kittyCoreContractAddress,
        blockingStubFull);
    Assert.assertFalse(StringUtils.isEmpty(smartContract.getBytecode()));

    Assert.assertTrue(smartContract.getAbi() != null);
    accountResource = PublicMethed.getAccountResource(deployAddress, blockingStubFull);
    cpuLimit = accountResource.getEnergyLimit();
    cpuUsage = accountResource.getEnergyUsed();
    account = PublicMethed.queryAccount(deployKey, blockingStubFull);
    logger.info("after balance is " + Long.toString(account.getBalance()));
    logger.info("after cpu limit is " + Long.toString(cpuLimit));
    logger.info("after cpu usage is " + Long.toString(cpuUsage));
    logger.info(ByteArray.toHexString(kittyCoreContractAddress));
    logger.info(ByteArray.toHexString(kittyCoreContractAddress).substring(2));

    kittyCoreAddressAndCut = "000000000000000000000000" + ByteArray
        .toHexString(kittyCoreContractAddress).substring(2);
    kittyCoreAddressAndCut = kittyCoreAddressAndCut + "0000000000000000000000000000000000000000000"
        + "000000000000000000100";
  }

  @Test(enabled = true,description = "Deploy Erc721 contract \"Sale Clock Auction\"")
  public void deploySaleClockAuction() {
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(deployAddress,
        blockingStubFull);
    Long cpuLimit = accountResource.getEnergyLimit();
    Long cpuUsage = accountResource.getEnergyUsed();
    Account account = PublicMethed.queryAccount(deployKey, blockingStubFull);
    logger.info("before balance is " + Long.toString(account.getBalance()));
    logger.info("before cpu limit is " + Long.toString(cpuLimit));
    logger.info("before cpu usage is " + Long.toString(cpuUsage));
    String contractName = "SaleClockAuction";
    String code = Configuration.getByPath("testng.conf")
        .getString("code.code_ContractScenario011_deploySaleClockAuction")
        + kittyCoreAddressAndCut;
    String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_ContractScenario011_deploySaleClockAuction");
    logger.info("Sale Clock Auction");
    saleClockAuctionContractAddress = PublicMethed.deployContract(contractName, abi, code,
        "", maxFeeLimit, 0L, consumeUserResourcePercent, null, deployKey,
        deployAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(saleClockAuctionContractAddress,
        blockingStubFull);
    Assert.assertFalse(StringUtils.isEmpty(smartContract.getBytecode()));
    Assert.assertTrue(smartContract.getAbi() != null);
    accountResource = PublicMethed.getAccountResource(deployAddress, blockingStubFull);
    cpuLimit = accountResource.getEnergyLimit();
    cpuUsage = accountResource.getEnergyUsed();
    account = PublicMethed.queryAccount(deployKey, blockingStubFull);
    logger.info("after balance is " + Long.toString(account.getBalance()));
    logger.info("after cpu limit is " + Long.toString(cpuLimit));
    logger.info("after cpu usage is " + Long.toString(cpuUsage));
  }

  @Test(enabled = true,description = "Deploy Erc721 contract \"Siring Clock Auction\"")
  public void deploySiringClockAuction() {
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(deployAddress,
        blockingStubFull);
    Long cpuLimit = accountResource.getEnergyLimit();
    Long cpuUsage = accountResource.getEnergyUsed();
    Account account = PublicMethed.queryAccount(deployKey, blockingStubFull);
    logger.info("before balance is " + Long.toString(account.getBalance()));
    logger.info("before cpu limit is " + Long.toString(cpuLimit));
    logger.info("before cpu usage is " + Long.toString(cpuUsage));
    String contractName = "SiringClockAuction";
    String code = Configuration.getByPath("testng.conf")
        .getString("code.code_ContractScenario011_deploySiringClockAuction")
        + kittyCoreAddressAndCut;
    String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_ContractScenario011_deploySiringClockAuction");
    logger.info("Siring Clock Auction");
    siringClockAuctionContractAddress = PublicMethed.deployContract(contractName, abi, code,
        "", maxFeeLimit, 0L, consumeUserResourcePercent, null, deployKey,
        deployAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(siringClockAuctionContractAddress,
        blockingStubFull);
    Assert.assertFalse(StringUtils.isEmpty(smartContract.getBytecode()));
    Assert.assertTrue(smartContract.getAbi() != null);
    accountResource = PublicMethed.getAccountResource(deployAddress, blockingStubFull);
    cpuLimit = accountResource.getEnergyLimit();
    cpuUsage = accountResource.getEnergyUsed();
    account = PublicMethed.queryAccount(deployKey, blockingStubFull);
    logger.info("after balance is " + Long.toString(account.getBalance()));
    logger.info("after cpu limit is " + Long.toString(cpuLimit));
    logger.info("after cpu usage is " + Long.toString(cpuUsage));
  }

  @Test(enabled = true,description = "Deploy Erc721 contract \"Gene Science Interface\"")
  public void deployGeneScienceInterface() {
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(deployAddress,
        blockingStubFull);
    Long cpuLimit = accountResource.getEnergyLimit();
    Long cpuUsage = accountResource.getEnergyUsed();
    Account account = PublicMethed.queryAccount(deployKey, blockingStubFull);
    logger.info("before balance is " + Long.toString(account.getBalance()));
    logger.info("before cpu limit is " + Long.toString(cpuLimit));
    logger.info("before cpu usage is " + Long.toString(cpuUsage));
    String contractName = "GeneScienceInterface";
    String code = Configuration.getByPath("testng.conf")
        .getString("code.code_ContractScenario011_deployGeneScienceInterface")
        + kittyCoreAddressAndCut;
    String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_ContractScenario011_deployGeneScienceInterface");
    logger.info("gene Science Interface");
    geneScienceInterfaceContractAddress = PublicMethed.deployContract(contractName, abi, code,
        "", maxFeeLimit,
        0L, consumeUserResourcePercent, null, deployKey, deployAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(geneScienceInterfaceContractAddress,
        blockingStubFull);
    Assert.assertFalse(StringUtils.isEmpty(smartContract.getBytecode()));
    Assert.assertTrue(smartContract.getAbi() != null);
    accountResource = PublicMethed.getAccountResource(deployAddress, blockingStubFull);
    cpuLimit = accountResource.getEnergyLimit();
    cpuUsage = accountResource.getEnergyUsed();
    account = PublicMethed.queryAccount(deployKey, blockingStubFull);
    logger.info("after balance is " + Long.toString(account.getBalance()));
    logger.info("after cpu limit is " + Long.toString(cpuLimit));
    logger.info("after cpu usage is " + Long.toString(cpuUsage));
  }

  @Test(enabled = true,description = "Set three contract address for Kitty Core, "
      + "set three CXO roles")
  public void triggerToSetThreeContractAddressToKittyCore() {
    //Set SaleAuctionAddress to kitty core.
    String saleContractString = "\"" + Base58.encode58Check(saleClockAuctionContractAddress) + "\"";
    txid = PublicMethed.triggerContract(kittyCoreContractAddress, "setSaleAuctionAddress(address)",
        saleContractString, false, 0, 10000000L, deployAddress, deployKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info(txid);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);

    //Set SiringAuctionAddress to kitty core.
    String siringContractString = "\"" + Base58.encode58Check(siringClockAuctionContractAddress)
        + "\"";
    txid = PublicMethed
        .triggerContract(kittyCoreContractAddress, "setSiringAuctionAddress(address)",
            siringContractString, false, 0, 10000000L, deployAddress, deployKey, blockingStubFull);
    logger.info(txid);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);

    //Set gen contract to kitty core
    String genContractString = "\"" + Base58.encode58Check(geneScienceInterfaceContractAddress)
        + "\"";
    txid = PublicMethed.triggerContract(kittyCoreContractAddress,
        "setGeneScienceAddress(address)", genContractString,
        false, 0, 10000000L, deployAddress, deployKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info(txid);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);

    //Start the game.
    Integer result = 1;
    Integer times = 0;
    while (result == 1) {
      txid = PublicMethed.triggerContract(kittyCoreContractAddress, "unpause()", "", false, 0,
          10000000L, deployAddress, deployKey, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
      result = infoById.get().getResultValue();
      if (times++ == 3) {
        break;
      }
    }

    Assert.assertTrue(result == 0);
    logger.info("start the game " + txid);

    //Create one gen0 cat.
    txid = PublicMethed.triggerContract(kittyCoreContractAddress,
        "createGen0Auction(uint256)", "-1000000000000000", false,
        0, 100000000L, deployAddress, deployKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    txid = PublicMethed.triggerContract(kittyCoreContractAddress,
        "gen0CreatedCount()", "#", false,
        0, 100000000L, deployAddress, deployKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    txid = PublicMethed.triggerContract(kittyCoreContractAddress,
        "getKitty(uint256)", "1", false, 0, 10000000, triggerAddress,
        triggerKey, blockingStubFull);
    logger.info("getKitty " + txid);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    String newCxoAddress = "\"" + Base58.encode58Check(triggerAddress)
        + "\"";

    txid = PublicMethed.triggerContract(kittyCoreContractAddress,
        "setCOO(address)", newCxoAddress, false, 0, 10000000, deployAddress,
        deployKey, blockingStubFull);
    logger.info("COO " + txid);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    txid = PublicMethed.triggerContract(kittyCoreContractAddress,
        "setCFO(address)", newCxoAddress, false, 0, 10000000, deployAddress,
        deployKey, blockingStubFull);
    logger.info("CFO " + txid);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    txid = PublicMethed.triggerContract(kittyCoreContractAddress,
        "setCEO(address)", newCxoAddress, false, 0, 1000000, deployAddress,
        deployKey, blockingStubFull);
    logger.info("CEO " + txid);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
  }

  @Test(enabled = true,description = "Create Gen0 cat")
  public void triggerUseTriggerEnergyUsage() {
    ECKey ecKey3 = new ECKey(Utils.getRandom());
    byte[] triggerUseTriggerEnergyUsageAddress = ecKey3.getAddress();
    final String triggerUseTriggerEnergyUsageKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
    Assert.assertTrue(
        PublicMethed.sendcoin(triggerUseTriggerEnergyUsageAddress, 100000000000L,
            fromAddress, testKey002, blockingStubFull));
    String newCxoAddress = "\"" + Base58.encode58Check(triggerUseTriggerEnergyUsageAddress)
        + "\"";
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    final String txid1;
    final String txid2;
    final String txid3;
    txid1 = PublicMethed.triggerContract(kittyCoreContractAddress,
        "setCOO(address)", newCxoAddress, false, 0, maxFeeLimit, triggerAddress,
        triggerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("COO " + txid);

    txid2 = PublicMethed.triggerContract(kittyCoreContractAddress,
        "setCFO(address)", newCxoAddress, false, 0, maxFeeLimit, triggerAddress,
        triggerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("CFO " + txid);

    txid3 = PublicMethed.triggerContract(kittyCoreContractAddress,
        "setCEO(address)", newCxoAddress, false, 0, maxFeeLimit, triggerAddress,
        triggerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("CEO " + txid);

    infoById = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    infoById = PublicMethed.getTransactionInfoById(txid2, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    infoById = PublicMethed.getTransactionInfoById(txid3, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long beforeBalance = PublicMethed
        .queryAccount(triggerUseTriggerEnergyUsageKey, blockingStubFull).getBalance();
    logger.info("before balance is " + Long.toString(beforeBalance));
    txid = PublicMethed.triggerContract(kittyCoreContractAddress,
        "createGen0Auction(uint256)", "0", false,
        0, 100000000L, triggerUseTriggerEnergyUsageAddress, triggerUseTriggerEnergyUsageKey,
        blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull1);
    logger.info("Q " + Long
        .toString(infoById.get().getReceipt().getEnergyFee()));
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsage() == 0);
    Assert.assertTrue(infoById.get().getReceipt().getEnergyFee() > 10000);
    //    Assert.assertTrue(infoById.get().getReceipt().getOriginEnergyUsage() > 10000);
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal()
        == infoById.get().getReceipt().getEnergyFee() / 100 + infoById.get().getReceipt()
        .getOriginEnergyUsage());

    Long fee = infoById.get().getFee();
    Long afterBalance = PublicMethed
        .queryAccount(triggerUseTriggerEnergyUsageKey, blockingStubFull1).getBalance();
    logger.info("after balance is " + Long.toString(afterBalance));
    logger.info("fee is " + Long.toString(fee));
    Assert.assertTrue(beforeBalance == afterBalance + fee);

    logger.info("before EnergyUsage is " + infoById.get().getReceipt().getEnergyUsage());
    logger.info("before EnergyFee is " + infoById.get().getReceipt().getEnergyFee());
    logger.info("before OriginEnergyUsage is " + infoById.get().getReceipt()
        .getOriginEnergyUsage());
    logger.info("before EnergyTotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    Assert.assertTrue(
        PublicMethed.freezeBalanceGetEnergy(triggerUseTriggerEnergyUsageAddress, 100000000L,
            0, 1, triggerUseTriggerEnergyUsageKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    beforeBalance = PublicMethed.queryAccount(triggerUseTriggerEnergyUsageKey, blockingStubFull)
        .getBalance();
    logger.info("before balance is " + Long.toString(beforeBalance));

    AccountResourceMessage accountResource = PublicMethed
        .getAccountResource(triggerUseTriggerEnergyUsageAddress, blockingStubFull);
    Long energyLimit = accountResource.getEnergyLimit();
    logger.info("before EnergyLimit is " + Long.toString(energyLimit));

    txid = PublicMethed.triggerContract(kittyCoreContractAddress,
        "createGen0Auction(uint256)", "0", false,
        0, 100000000L, triggerUseTriggerEnergyUsageAddress, triggerUseTriggerEnergyUsageKey,
        blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull1);
    logger.info("after EnergyUsage is " + infoById.get().getReceipt().getEnergyUsage());
    logger.info("after EnergyFee is " + infoById.get().getReceipt().getEnergyFee());
    logger.info("after OriginEnergyUsage is " + infoById.get().getReceipt().getOriginEnergyUsage());
    logger.info("after EnergyTotal is " + infoById.get().getReceipt().getEnergyUsageTotal());
    fee = infoById.get().getFee();
    afterBalance = PublicMethed.queryAccount(triggerUseTriggerEnergyUsageKey, blockingStubFull1)
        .getBalance();
    logger.info("after balance is " + Long.toString(afterBalance));
    logger.info("fee is " + Long.toString(fee));

    accountResource = PublicMethed
        .getAccountResource(triggerUseTriggerEnergyUsageAddress, blockingStubFull1);
    energyLimit = accountResource.getEnergyLimit();

    logger.info("after EnergyLimit is " + Long.toString(energyLimit));

    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsage() > 10000);
    Assert.assertTrue(infoById.get().getReceipt().getEnergyFee() == 0);

    //Assert.assertTrue(infoById.get().getReceipt().getOriginEnergyUsage() > 10000);
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() == infoById.get()
        .getReceipt().getEnergyUsage() + infoById.get().getReceipt().getOriginEnergyUsage());
    //    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsage() == infoById.get()
    //        .getReceipt().getOriginEnergyUsage());

    Assert.assertTrue(beforeBalance == afterBalance + fee);
    PublicMethed.unFreezeBalance(deployAddress, deployKey, 1,
        deployAddress, blockingStubFull);
    PublicMethed.unFreezeBalance(triggerAddress, triggerKey, 1,
        triggerAddress, blockingStubFull);

    PublicMethed
        .unFreezeBalance(triggerUseTriggerEnergyUsageAddress, triggerUseTriggerEnergyUsageKey, 1,
            triggerUseTriggerEnergyUsageAddress, blockingStubFull);
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


