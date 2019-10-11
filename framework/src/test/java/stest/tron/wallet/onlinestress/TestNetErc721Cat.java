package stest.tron.wallet.onlinestress;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
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
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class TestNetErc721Cat {

  //testng001、testng002、testng003、testng004
  //testng001、testng002、testng003、testng004
  private final String testKey002 =
      //"7306c6044ad7c03709980aa188b8555288b7e0608f5edbf76ff2381c5a7a15a8";
      //"3a54ba30e3ee41b602eca8fb3a3ca1f99f49a3d3ab5d8d646a2ccdd3ffd9c21d";
      //fromAddress
      "FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  String kittyCoreAddressAndCut = "";
  byte[] kittyCoreContractAddress = null;
  byte[] saleClockAuctionContractAddress = null;
  byte[] siringClockAuctionContractAddress = null;
  byte[] geneScienceInterfaceContractAddress = null;
  Integer consumeUserResourcePercent = 20;
  String txid = "";
  Optional<TransactionInfo> infoById = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] deployAddress = ecKey1.getAddress();
  String deployKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] triggerAddress = ecKey2.getAddress();
  String triggerKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = false)
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
    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(deployAddress, 100000000L,
        3, 1, deployKey, blockingStubFull));
    /*    Assert.assertTrue(PublicMethed.freezeBalanceGetCpu(triggerAddress,100000000L,
        3,1,triggerKey,blockingStubFull));*/
    /*Assert.assertTrue(PublicMethed.buyStorage(500000000L,deployAddress,deployKey,
        blockingStubFull));
    Assert.assertTrue(PublicMethed.buyStorage(500000000L,triggerAddress,triggerKey,
        blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalance(deployAddress,100000000L,3,
        deployKey,blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalance(triggerAddress,100000000L,3,
        triggerKey,blockingStubFull));*/

  }

  @Test(enabled = false)
  public void deployErc721KittyCore() {
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(deployAddress,
        blockingStubFull);
    Long cpuLimit = accountResource.getEnergyLimit();
    //Long storageLimit = accountResource.getStorageLimit();
    Long cpuUsage = accountResource.getEnergyUsed();
    //Long storageUsage = accountResource.getStorageUsed();
    Account account = PublicMethed.queryAccount(deployAddress, blockingStubFull);
    logger.info("before balance is " + Long.toString(account.getBalance()));
    logger.info("before cpu limit is " + Long.toString(cpuLimit));
    logger.info("before cpu usage is " + Long.toString(cpuUsage));
    //logger.info("before storage limit is " + Long.toString(storageLimit));
    //logger.info("before storage usaged is " + Long.toString(storageUsage));
    Long maxFeeLimit = 3900000000L;
    String contractName = "KittyCore";
    String code = Configuration.getByPath("testng.conf")
        .getString("code.code_TestNetErc721Cat_deployErc721KittyCore");
    String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_TestNetErc721Cat_deployErc721KittyCore");
    logger.info("Kitty Core");
    kittyCoreContractAddress = PublicMethed.deployContract(contractName, abi, code, "",
        maxFeeLimit, 0L, consumeUserResourcePercent, null, deployKey,
        deployAddress, blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(kittyCoreContractAddress,
        blockingStubFull);

    Assert.assertTrue(smartContract.getAbi() != null);
    accountResource = PublicMethed.getAccountResource(deployAddress, blockingStubFull);
    cpuLimit = accountResource.getEnergyLimit();
    //storageLimit = accountResource.getStorageLimit();
    cpuUsage = accountResource.getEnergyUsed();
    //storageUsage = accountResource.getStorageUsed();
    account = PublicMethed.queryAccount(deployKey, blockingStubFull);
    logger.info("after balance is " + Long.toString(account.getBalance()));
    logger.info("after cpu limit is " + Long.toString(cpuLimit));
    logger.info("after cpu usage is " + Long.toString(cpuUsage));
    //logger.info("after storage limit is " + Long.toString(storageLimit));
    //logger.info("after storage usaged is " + Long.toString(storageUsage));
    logger.info(ByteArray.toHexString(kittyCoreContractAddress));
    logger.info(ByteArray.toHexString(kittyCoreContractAddress).substring(2));

    kittyCoreAddressAndCut = "000000000000000000000000" + ByteArray
        .toHexString(kittyCoreContractAddress).substring(2);
    kittyCoreAddressAndCut = kittyCoreAddressAndCut + "0000000000000000000000000000000000000000000"
        + "000000000000000000100";
  }

  @Test(enabled = false)
  public void deploySaleClockAuction() {
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(deployAddress,
        blockingStubFull);
    Long cpuLimit = accountResource.getEnergyLimit();
    //Long storageLimit = accountResource.getStorageLimit();
    Long cpuUsage = accountResource.getEnergyUsed();
    //Long storageUsage = accountResource.getStorageUsed();
    Account account = PublicMethed.queryAccount(deployKey, blockingStubFull);
    logger.info("before balance is " + Long.toString(account.getBalance()));
    logger.info("before cpu limit is " + Long.toString(cpuLimit));
    logger.info("before cpu usage is " + Long.toString(cpuUsage));
    //logger.info("before storage limit is " + Long.toString(storageLimit));
    //logger.info("before storage usaged is " + Long.toString(storageUsage));
    Long maxFeeLimit = 3900000000L;
    String contractName = "SaleClockAuction";
    logger.info("Sale Clock Auction");
    String code = Configuration.getByPath("testng.conf")
        .getString("code.code_TestNetErc721Cat_deploySaleClockAuction");
    String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_TestNetErc721Cat_deploySaleClockAuction");
    saleClockAuctionContractAddress = PublicMethed.deployContract(contractName, abi, code,
        "", maxFeeLimit, 0L, consumeUserResourcePercent, null, deployKey,
        deployAddress, blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(saleClockAuctionContractAddress,
        blockingStubFull);
    Assert.assertTrue(smartContract.getAbi() != null);
    accountResource = PublicMethed.getAccountResource(deployAddress, blockingStubFull);
    cpuLimit = accountResource.getEnergyLimit();
    //storageLimit = accountResource.getStorageLimit();
    cpuUsage = accountResource.getEnergyUsed();
    //storageUsage = accountResource.getStorageUsed();
    account = PublicMethed.queryAccount(deployKey, blockingStubFull);
    logger.info("after balance is " + Long.toString(account.getBalance()));
    logger.info("after cpu limit is " + Long.toString(cpuLimit));
    logger.info("after cpu usage is " + Long.toString(cpuUsage));
    //logger.info("after storage limit is " + Long.toString(storageLimit));
    //logger.info("after storage usaged is " + Long.toString(storageUsage));
  }

  @Test(enabled = false)
  public void deploySiringClockAuction() {
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(deployAddress,
        blockingStubFull);
    Long cpuLimit = accountResource.getEnergyLimit();
    //Long storageLimit = accountResource.getStorageLimit();
    Long cpuUsage = accountResource.getEnergyUsed();
    //Long storageUsage = accountResource.getStorageUsed();
    Account account = PublicMethed.queryAccount(deployKey, blockingStubFull);
    logger.info("before balance is " + Long.toString(account.getBalance()));
    logger.info("before cpu limit is " + Long.toString(cpuLimit));
    logger.info("before cpu usage is " + Long.toString(cpuUsage));
    //logger.info("before storage limit is " + Long.toString(storageLimit));
    //logger.info("before storage usaged is " + Long.toString(storageUsage));
    Long maxFeeLimit = 3900000000L;
    String contractName = "SiringClockAuction";
    String code = Configuration.getByPath("testng.conf")
        .getString("code.code_TestNetErc721Cat_deploySiringClockAuction");
    String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_TestNetErc721Cat_deploySiringClockAuction");
    logger.info("Siring Clock Auction");
    siringClockAuctionContractAddress = PublicMethed.deployContract(contractName, abi, code,
        "", maxFeeLimit, 0L, consumeUserResourcePercent, null, deployKey,
        deployAddress, blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(siringClockAuctionContractAddress,
        blockingStubFull);
    Assert.assertTrue(smartContract.getAbi() != null);
    accountResource = PublicMethed.getAccountResource(deployAddress, blockingStubFull);
    cpuLimit = accountResource.getEnergyLimit();
    //storageLimit = accountResource.getStorageLimit();
    cpuUsage = accountResource.getEnergyUsed();
    //storageUsage = accountResource.getStorageUsed();
    account = PublicMethed.queryAccount(deployKey, blockingStubFull);
    logger.info("after balance is " + Long.toString(account.getBalance()));
    logger.info("after cpu limit is " + Long.toString(cpuLimit));
    logger.info("after cpu usage is " + Long.toString(cpuUsage));
    //logger.info("after storage limit is " + Long.toString(storageLimit));
    //logger.info("after storage usaged is " + Long.toString(storageUsage));
  }

  @Test(enabled = false)
  public void deployGeneScienceInterface() {
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(deployAddress,
        blockingStubFull);
    Long cpuLimit = accountResource.getEnergyLimit();
    //Long storageLimit = accountResource.getStorageLimit();
    Long cpuUsage = accountResource.getEnergyUsed();
    //Long storageUsage = accountResource.getStorageUsed();
    Account account = PublicMethed.queryAccount(deployKey, blockingStubFull);
    logger.info("before balance is " + Long.toString(account.getBalance()));
    logger.info("before cpu limit is " + Long.toString(cpuLimit));
    logger.info("before cpu usage is " + Long.toString(cpuUsage));
    //logger.info("before storage limit is " + Long.toString(storageLimit));
    //logger.info("before storage usaged is " + Long.toString(storageUsage));
    Long maxFeeLimit = 3900000000L;
    String contractName = "GeneScienceInterface";
    String code = Configuration.getByPath("testng.conf")
        .getString("code.code_TestNetErc721Cat_deployGeneScienceInterface");
    String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_TestNetErc721Cat_deployGeneScienceInterface");
    logger.info("gene Science Interface");
    geneScienceInterfaceContractAddress = PublicMethed.deployContract(contractName, abi, code,
        "", maxFeeLimit,
        0L, consumeUserResourcePercent, null, deployKey, deployAddress, blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(geneScienceInterfaceContractAddress,
        blockingStubFull);
    Assert.assertTrue(smartContract.getAbi() != null);
    accountResource = PublicMethed.getAccountResource(deployAddress, blockingStubFull);
    cpuLimit = accountResource.getEnergyLimit();
    //storageLimit = accountResource.getStorageLimit();
    cpuUsage = accountResource.getEnergyUsed();
    //storageUsage = accountResource.getStorageUsed();
    account = PublicMethed.queryAccount(deployKey, blockingStubFull);
    logger.info("after balance is " + Long.toString(account.getBalance()));
    logger.info("after cpu limit is " + Long.toString(cpuLimit));
    logger.info("after cpu usage is " + Long.toString(cpuUsage));
    //logger.info("after storage limit is " + Long.toString(storageLimit));
    //logger.info("after storage usaged is " + Long.toString(storageUsage));
  }

  @Test(enabled = false)
  public void triggerToSetThreeContractAddressToKittyCore() {
    //Set SaleAuctionAddress to kitty core.
    String saleContractString = "\"" + Base58.encode58Check(saleClockAuctionContractAddress) + "\"";
    txid = PublicMethed.triggerContract(kittyCoreContractAddress, "setSaleAuctionAddress(address)",
        saleContractString, false, 0, 10000000L, deployAddress, deployKey, blockingStubFull);
    logger.info(txid);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    //Assert.assertTrue(infoById.get().getReceipt().getStorageDelta() > 50);

    //Set SiringAuctionAddress to kitty core.
    String siringContractString = "\"" + Base58.encode58Check(siringClockAuctionContractAddress)
        + "\"";
    txid = PublicMethed
        .triggerContract(kittyCoreContractAddress, "setSiringAuctionAddress(address)",
            siringContractString, false, 0, 10000000L,
            deployAddress, deployKey, blockingStubFull);
    logger.info(txid);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    //Assert.assertTrue(infoById.get().getReceipt().getStorageDelta() > 50);

    //Set gen contract to kitty core
    String genContractString = "\"" + Base58.encode58Check(geneScienceInterfaceContractAddress)
        + "\"";
    txid = PublicMethed.triggerContract(kittyCoreContractAddress,
        "setGeneScienceAddress(address)", genContractString,
        false, 0, 10000000L, deployAddress, deployKey, blockingStubFull);
    logger.info(txid);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    //Assert.assertTrue(infoById.get().getReceipt().getStorageDelta() > 50);

    //Start the game.
    txid = PublicMethed.triggerContract(kittyCoreContractAddress, "unpause()", "", false, 0,
        10000000L, deployAddress, deployKey, blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    logger.info("start the game " + txid);

    //Create one gen0 cat.
    txid = PublicMethed.triggerContract(kittyCoreContractAddress,
        "createGen0Auction(uint256)", "-1000000000000000", false,
        0, 100000000L, deployAddress, deployKey, blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    txid = PublicMethed.triggerContract(kittyCoreContractAddress,
        "gen0CreatedCount()", "#", false,
        0, 100000000L, deployAddress, deployKey, blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    /*      txid = PublicMethed.triggerContract(kittyCoreContractAddress,
          "name()","#",false,0,10000000,triggerAddress,
          triggerKey,blockingStubFull);
      logger.info("getname " + txid);*/

    txid = PublicMethed.triggerContract(kittyCoreContractAddress,
        "getKitty(uint256)", "1", false, 0, 10000000, triggerAddress,
        triggerKey, blockingStubFull);
    logger.info("getKitty " + txid);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    String newCxoAddress = "\"" + Base58.encode58Check(triggerAddress)
        + "\"";

    txid = PublicMethed.triggerContract(kittyCoreContractAddress,
        "setCOO(address)", newCxoAddress, false, 0, 10000000, deployAddress,
        deployKey, blockingStubFull);
    logger.info("COO " + txid);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    txid = PublicMethed.triggerContract(kittyCoreContractAddress,
        "setCFO(address)", newCxoAddress, false, 0, 10000000, deployAddress,
        deployKey, blockingStubFull);
    logger.info("CFO " + txid);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    txid = PublicMethed.triggerContract(kittyCoreContractAddress,
        "setCEO(address)", newCxoAddress, false, 0, 1000000, deployAddress,
        deployKey, blockingStubFull);
    logger.info("CEO " + txid);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
  }

  @Test(enabled = false, threadPoolSize = 1, invocationCount = 1)
  public void unCreateKitty() {
    Integer times = 0;
    logger.info("In create kitty, kitty core address is " + ByteArray
        .toHexString(kittyCoreContractAddress));
    while (times++ < 20) {
      txid = PublicMethed.triggerContract(kittyCoreContractAddress,
          "createGen0Auction(uint256)", "0", false,
          0, 100000000L, triggerAddress, triggerKey, blockingStubFull);
      logger.info("createGen0 " + txid);
      infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
      //Assert.assertTrue(infoById.get().getResultValue() == 0);
      /*      String promoKitty = "\"" + times.toString() + "\",\""
          +  Base58.encode58Check(kittyCoreContractAddress) + "\"";
      logger.info(promoKitty);
      txid = PublicMethed.triggerContract(kittyCoreContractAddress,
          "createPromoKitty(uint256,address)", promoKitty,false,
          0,10000000L,triggerAddress,triggerKey,blockingStubFull);
      logger.info("createPromoKitty " + txid);
      infoById = PublicMethed.getTransactionInfoById(txid,blockingStubFull);
      Assert.assertTrue(infoById.get().getResultValue() == 0);*/
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

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
  }
}


