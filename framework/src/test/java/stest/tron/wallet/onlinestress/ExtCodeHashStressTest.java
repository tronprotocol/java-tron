package stest.tron.wallet.onlinestress;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
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
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ExtCodeHashStressTest {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);
  private AtomicLong count = new AtomicLong();
  private AtomicLong errorCount = new AtomicLong();
  private long startTime = System.currentTimeMillis();
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private byte[] extCodeHashContractAddress = null;
  private byte[] normalContractAddress = null;
  private byte[] testContractAddress = null;
  private byte[] dev001Address = fromAddress;
  private String dev001Key = testKey002;
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

    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    PublicMethed.printAddress(dev001Key);
    PublicMethed.printAddress(user001Key);
  }

  @Test(enabled = true, description = "Deploy a normal contract to be used for stress testing.")
  public void test01DeployNormalContract() {
    Assert.assertTrue(PublicMethed.sendcoin(user001Address, 100_000_000L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //before deploy, check account resource
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    long energyLimit = accountResource.getEnergyLimit();
    long energyUsage = accountResource.getEnergyUsed();
    long balanceBefore = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();
    logger.info("before energyLimit is " + Long.toString(energyLimit));
    logger.info("before energyUsage is " + Long.toString(energyUsage));
    logger.info("before balanceBefore is " + Long.toString(balanceBefore));

    String filePath = "./src/test/resources/soliditycode/extCodeHashStress.sol";
    String contractName = "TriggerNormal";  //TBVEkA72g1wFoBBVLSXFZ2Bp944oL17NeU
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    normalContractAddress = deployContract(code, abi, contractName, blockingStubFull);

    SmartContract smartContract = PublicMethed.getContract(normalContractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
  }

  @Test(enabled = true, description = "Deploy a extcodehash contract.")
  public void test02DeployExtCodeHashContract() {
    Assert.assertTrue(PublicMethed.sendcoin(user001Address, 100_000_000L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //before deploy, check account resource
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    long energyLimit = accountResource.getEnergyLimit();
    long energyUsage = accountResource.getEnergyUsed();
    long balanceBefore = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();
    logger.info("before energyLimit is " + Long.toString(energyLimit));
    logger.info("before energyUsage is " + Long.toString(energyUsage));
    logger.info("before balanceBefore is " + Long.toString(balanceBefore));

    String filePath = "./src/test/resources/soliditycode/extCodeHashStress.sol";
    String contractName = "Trigger"; //THAx2PcAtRCerwrLGN237dahqSUfq5wLnR
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    extCodeHashContractAddress = deployContract(code, abi, contractName, blockingStubFull);

    SmartContract smartContract = PublicMethed.getContract(extCodeHashContractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
  }

  /**
   * trigger.
   */
  public byte[] deployContract(String bytecode, String abi, String contractName,
      WalletGrpc.WalletBlockingStub blockingStubFull) {

    final String transferTokenTxid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, bytecode, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, dev001Key,
            dev001Address, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(transferTokenTxid, blockingStubFull);

    TransactionInfo transactionInfo = infoById.get();
    logger.info("EnergyUsageTotal: " + transactionInfo.getReceipt().getEnergyUsageTotal());
    logger.info("NetUsage: " + transactionInfo.getReceipt().getNetUsage());

    if (infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage()
          .toStringUtf8());
    }

    return infoById.get().getContractAddress().toByteArray();
  }

  /**
   * trigger.
   */
  public String triggerContractWithMaxFeeLimit(byte[] testAddress, byte[] user001Address,
      String user001Key, long maxFeeLimit) {
    Assert.assertTrue(PublicMethed.sendcoin(user001Address, 10000_000_000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress,
        PublicMethed.getFreezeBalanceCount(user001Address, user001Key, 50000L,
            blockingStubFull), 0, 1,
        ByteString.copyFrom(user001Address), testKey002, blockingStubFull));

    Long callValue = Long.valueOf(0);

    String param = "\"" + Base58.encode58Check(testAddress) + "\"";
    final String triggerTxid = PublicMethed.triggerContract(extCodeHashContractAddress,
        "test(address)", param, false, callValue,
        maxFeeLimit, "0", 0, user001Address, user001Key,
        blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    scheduledExecutorService
        .schedule(new CheckTask(triggerTxid, blockingStubFull), 15, TimeUnit.SECONDS);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);

    TransactionInfo transactionInfo = infoById.get();
    logger.info("EnergyUsageTotal: " + transactionInfo.getReceipt().getEnergyUsageTotal());
    logger.info("NetUsage: " + transactionInfo.getReceipt().getNetUsage());

    logger
        .info("transaction failed with message: " + infoById.get().getResMessage().toStringUtf8());

    if (infoById.get().getResMessage().toStringUtf8().toLowerCase().contains("cpu")) {
      throw new IllegalArgumentException();
    }
    if (infoById.get().getResMessage().toStringUtf8().toLowerCase().contains("timeout")) {
      throw new IllegalArgumentException();
    }
    return "ok";
  }

  /**
   * trigger.
   */
  public String triggerAndGetExtCodeHashList(List<byte[]> testAddress, byte[] user001Address,
      String user001Key, long maxFeeLimit, WalletGrpc.WalletBlockingStub blockingStubFull) {

    Long callValue = Long.valueOf(0);
    List<String> params = new ArrayList<>();
    for (int i = 0; i < testAddress.size(); i++) {
      params.add(Base58.encode58Check(testAddress.get(i)));
    }
    final String triggerTxid = PublicMethed.triggerParamListContract(extCodeHashContractAddress,
        "test(address[])", Arrays.asList(params), false, callValue,
        maxFeeLimit, "0", 0, user001Address, user001Key,
        blockingStubFull);

    scheduledExecutorService
        .schedule(new CheckTask(triggerTxid, blockingStubFull), 15, TimeUnit.SECONDS);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);

    TransactionInfo transactionInfo = infoById.get();
    logger.info("EnergyUsageTotal: " + transactionInfo.getReceipt().getEnergyUsageTotal());
    logger.info("NetUsage: " + transactionInfo.getReceipt().getNetUsage());
    logger
        .info(
            "transaction failed with message: " + infoById.get().getResMessage().toStringUtf8());

    return "ok";

  }

  /**
   * trigger.
   */
  public void triggerAndGetExtCodeHash(byte[] testAddress, byte[] user001Address,
      String user001Key, long maxFeeLimit, WalletGrpc.WalletBlockingStub blockingStubFull) {

    Long callValue = Long.valueOf(0);

    String param = "\"" + Base58.encode58Check(testAddress) + "\"";
    final String triggerTxid = PublicMethed.triggerContract(normalContractAddress,
        "test(address)", param, false, callValue,
        314982000, "0", 0, user001Address, user001Key,
        blockingStubFull);

    scheduledExecutorService
        .schedule(new CheckTask(triggerTxid, blockingStubFull), 15, TimeUnit.SECONDS);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);

    TransactionInfo transactionInfo = infoById.get();
    logger.info("EnergyUsageTotal: " + transactionInfo.getReceipt().getEnergyUsageTotal());
    logger.info("NetUsage: " + transactionInfo.getReceipt().getNetUsage());
    logger
        .info(
            "transaction failed with message: " + infoById.get().getResMessage().toStringUtf8());
  }

  private synchronized void wirteLine(String fileName, String line) {
    try {
      BufferedWriter out = new BufferedWriter(new FileWriter(fileName, true));
      out.write(line);
      out.newLine();
      out.flush();
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test(enabled = true, description = "Deploy multiple long bytecode contract "
      + "and write address to file.")
  public void test03DeployMultiLongByteCodeContract() {

    ExecutorService pool = Executors.newFixedThreadPool(30);
    Map<String, Boolean> addressMap = new ConcurrentHashMap<>();
    int size = 50_0_000;
    int stubSize = 30;
    List<WalletGrpc.WalletBlockingStub> stubs = new ArrayList<>();
    for (int i = 0; i < stubSize; i++) {
      stubs.add(WalletGrpc.newBlockingStub(channelFull));
    }

    CountDownLatch count = new CountDownLatch(size);
    for (int i = 0; i < size; i++) {

      String contractName = "extcodehashContract" + i;
      logger.info("[" + i + "]contractName: " + contractName);
      pool.submit(new DeployTask(addressMap, i, count, stubs.get(i % stubSize)));

    }

    try {
      count.await();
      for (String s : addressMap.keySet()) {
        System.out.println(s);
      }

    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Test(enabled = true, description = "Calculate the contract maxfeelimit.",
      threadPoolSize = 1, invocationCount = 1)
  public void test04StressGetExtCodeHashContract() {

    ECKey ecKey2 = new ECKey(Utils.getRandom());
    byte[] user001Address = ecKey2.getAddress();
    String user001Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    extCodeHashContractAddress = WalletClient
        .decodeFromBase58Check("TEsdDpJQrLBDPmJfDF2Ex53iMfzetqHvn9");

    // long bytecode contract
    testContractAddress = WalletClient.decodeFromBase58Check("TDqSAv8gLFXQRfug5Pr1Ev6zrEj1efC8qe");

    HashMap<String, String> retMap = new HashMap<>();

    long feeLimit = 314982000;
    //    long feeLimit = 393624800;
    //    long feeLimit = 406731800;

    long base = 100;
    long lastSuccess = feeLimit;
    int failed = 0;

    for (int i = 0; i < 1000000000; i++) {
      try {
        String retCode = triggerContractWithMaxFeeLimit(testContractAddress, user001Address,
            user001Key,
            feeLimit);
        logger.info("[" + i + "]retCode: " + retCode);
        logger.info("[" + i + "]feeLimit: " + feeLimit);
        lastSuccess = feeLimit;
        base *= 2;
        feeLimit += base;
        failed = 0;
      } catch (Exception e) {
        failed++;
        if (failed > 3) {
          break;
        }
        logger.error("cpu timeout");
        feeLimit = lastSuccess;
        base = 100;
      }
    }
  }

  @Test(enabled = true, description = "Trigger extcodeHash contract stress.")
  public void test05TriggerContract() throws FileNotFoundException {

    BufferedReader reader = null;
    List<String> addresses = new ArrayList<>();
    try {
      reader = new BufferedReader(new FileReader("src/test/resources/address2"));
      String line = reader.readLine();
      while (line != null) {
        System.out.println(line);
        // read next line
        line = reader.readLine();
        addresses.add(line);
      }
      reader.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    ExecutorService pool = Executors.newFixedThreadPool(50);
    int stubSize = 50;
    List<WalletGrpc.WalletBlockingStub> stubs = new ArrayList<>();
    for (int i = 0; i < stubSize; i++) {
      stubs.add(WalletGrpc.newBlockingStub(channelFull));
    }

    int paramsSize = 75; // the address count per trigger
    int trigger = 0;
    for (int i = 0; i + paramsSize < addresses.size(); i += paramsSize) {
      System.err.println(trigger++);
      System.err.println(i + " " + (i + paramsSize));
      pool.submit(new TriggerTask(addresses.subList(i, i + paramsSize), stubs.get(
          (int) (Math.random() * 100 % stubSize))));
    }

    try {
      Thread.sleep(100000000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * trigger.
   */
  public void triggerContact(String[] testList, WalletGrpc.WalletBlockingStub stub) {

    final byte[] user001Address = fromAddress;
    final String user001Key = testKey002;

    extCodeHashContractAddress = WalletClient
        .decodeFromBase58Check("TJGYcUspHrwPgy72YeaVjD4Skep9Ji8Pnn");

    final long feeLimit = 102471600;
    count.getAndAdd(1);
    if (count.get() % 100 == 0) {
      long cost = (System.currentTimeMillis() - startTime) / 1000;
      logger.info("Count:" + count.get() + ", cost:" + cost
          + ", avg:" + count.get() / cost + ", errCount:" + errorCount);
    }

    List<byte[]> addressList = new ArrayList<>();

    for (int k = 0; k < testList.length; k++) {
      addressList.add(WalletClient.decodeFromBase58Check(testList[k]));
    }
    triggerAndGetExtCodeHashList(addressList, user001Address, user001Key, feeLimit, stub);
  }

  @Test(enabled = true, description = "Trigger normal contract stress.")
  public void test06TriggerNormalContract() throws FileNotFoundException {

    BufferedReader reader = null;
    List<String> addresses = new ArrayList<>();
    try {
      reader = new BufferedReader(new FileReader(
          "src/test/resources/address2"));
      String line = reader.readLine();
      while (line != null) {
        System.out.println(line);
        // read next line
        line = reader.readLine();
        addresses.add(line);
      }
      reader.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    ExecutorService pool = Executors.newFixedThreadPool(50);
    int stubSize = 50;
    List<WalletGrpc.WalletBlockingStub> stubs = new ArrayList<>();
    for (int i = 0; i < stubSize; i++) {
      stubs.add(WalletGrpc.newBlockingStub(channelFull));
    }

    int paramsSize = 50;
    int trigger = 0;
    for (int i = 0; i + paramsSize < addresses.size(); i += 1) {
      System.err.println(trigger++);
      System.err.println(i + " " + (i + paramsSize));
      pool.submit(
          new TriggerNormalTask(addresses.subList(0, 0 + paramsSize), stubs.get(
              (int) (Math.random() * 100 % stubSize))));
    }

    try {
      Thread.sleep(100000000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * trigger.
   */
  public void triggerNormalContact(String[] testList, WalletGrpc.WalletBlockingStub stub) {

    final byte[] user001Address = fromAddress;
    final String user001Key = testKey002;

    normalContractAddress = WalletClient
        .decodeFromBase58Check("TFUSarvJtCSQhDifdRaioytThohLSLCjq4");

    final long feeLimit = 51079600;
    count.getAndAdd(1);
    if (count.get() % 100 == 0) {
      long cost = (System.currentTimeMillis() - startTime) / 1000;
      logger.info("Count:" + count.get() + ", cost:" + cost
          + ", avg:" + count.get() / cost + ", errCount:" + errorCount);
    }

    List<byte[]> addressList = new ArrayList<>();

    for (int k = 0; k < testList.length; k++) {
      addressList.add(WalletClient.decodeFromBase58Check(testList[k]));
    }

    triggerAndGetExtCodeHash(normalContractAddress, user001Address,
        user001Key, feeLimit, stub);
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

  class DeployTask implements Runnable {

    Map<String, Boolean> addressList;
    CountDownLatch countDownLatch;
    WalletGrpc.WalletBlockingStub stub;
    int index;

    DeployTask(Map<String, Boolean> addressList, int index, CountDownLatch countDownLatch,
        WalletGrpc.WalletBlockingStub stub) {
      this.index = index;
      this.addressList = addressList;
      this.countDownLatch = countDownLatch;
      this.stub = stub;
    }

    @Override
    public void run() {
      logger.info("depoying :" + index);
      String code = Configuration.getByPath("testng.conf")
          .getString("code.code_veryLarge");
      String abi = Configuration.getByPath("testng.conf")
          .getString("abi.abi_veryLarge");
      try {
        byte[] deployedAddress = deployContract(code, abi, "test" + index, stub);
        String address = Base58.encode58Check(deployedAddress);
        wirteLine(
            "src/test/resources/addresses2",
            address);
        logger.info("deployed : " + index + " " + address);
      } catch (Throwable e) {
        logger.error("deploy error: ", e);
      } finally {
        countDownLatch.countDown();
      }
    }
  }

  class CheckTask implements Runnable {

    String txid;
    WalletGrpc.WalletBlockingStub client;

    CheckTask(String txid, WalletGrpc.WalletBlockingStub client) {
      this.txid = txid;
      this.client = client;
    }

    @Override
    public void run() {

      Optional<TransactionInfo> infoById = PublicMethed
          .getTransactionInfoById(this.txid, blockingStubFull);

      TransactionInfo transactionInfo = infoById.get();
      if (infoById.get().getResultValue() != 0) {
        logger.error("txid:" + this.txid);
        logger.error(
            "transaction failed with message: " + infoById.get().getResMessage().toStringUtf8());
      }
      logger.info("infoById" + infoById);
    }
  }

  class TriggerTask implements Runnable {

    List<String> addresses;
    WalletGrpc.WalletBlockingStub stub;

    TriggerTask(List<String> addresses, WalletGrpc.WalletBlockingStub stub) {
      this.addresses = addresses;
      this.stub = stub;
    }

    @Override
    public void run() {
      triggerContact(this.addresses.toArray(new String[0]), stub);
    }
  }

  class TriggerNormalTask implements Runnable {

    List<String> addresses;
    WalletGrpc.WalletBlockingStub stub;

    TriggerNormalTask(List<String> addresses, WalletGrpc.WalletBlockingStub stub) {
      this.addresses = addresses;
      this.stub = stub;
    }

    @Override
    public void run() {
      triggerNormalContact(this.addresses.toArray(new String[0]), stub);
    }
  }
}


