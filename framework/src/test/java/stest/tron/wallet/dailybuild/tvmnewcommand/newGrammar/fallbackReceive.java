package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;


@Slf4j
public class fallbackReceive {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  byte[] contractAddressCaller = null;
  byte[] contractAddressTest0 = null;
  byte[] contractAddressTest1 = null;
  byte[] contractAddressTest2 = null;
  byte[] contractAddressTestPayable = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
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

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

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
    PublicMethed
        .sendcoin(contractExcAddress, 1000_000_000_000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/fallbackUpgrade.sol";
    String contractName = "Caller";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddressCaller = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0L, 100,
            null, contractExcKey,
            contractExcAddress, blockingStubFull);
    contractName = "Test0";
    retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    contractAddressTest0 = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0L,
            100, null, contractExcKey,
            contractExcAddress, blockingStubFull);
    contractName = "Test1";
    retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    contractAddressTest1 = PublicMethed
        .deployContractFallback(contractName, abi, code, "", maxFeeLimit, 0L,
            100, null, contractExcKey,
            contractExcAddress, blockingStubFull);
    contractName = "Test2";
    retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    contractAddressTest2 = PublicMethed
        .deployContractFallback(contractName, abi, code, "", maxFeeLimit, 0L,
            100, null, contractExcKey,
            contractExcAddress, blockingStubFull);
    contractName = "TestPayable";
    retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    contractAddressTestPayable = PublicMethed
        .deployContractFallback(contractName, abi, code, "", maxFeeLimit, 0L,
            100, null, contractExcKey,
            contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "contract test0 has no fallback method")
  public void test001NoFallback() {
    String txid = "";
    String method = "hello()";
    txid = PublicMethed.triggerContract(contractAddressTest0,
        method, "#", false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("getResult: " + infoById.get().getResultValue());
    Assert.assertEquals("FAILED", infoById.get().getResult().toString());
  }

  @Test(enabled = true, description = "contract test0 has no fallback method")
  public void test002NoFallback() {
    String txid = "";
    String method = "callTest0(address)";
    String para = "\"" + Base58.encode58Check(contractAddressTest0) + "\"";
    txid = PublicMethed.triggerContract(contractAddressCaller,
        method, para, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("getResult: " + infoById.get().getResultValue());
    Assert.assertEquals("FAILED", infoById.get().getResult().toString());
  }

  @Test(enabled = true, description = "contract test01 has fallback method")
  public void test011Fallback() {
    String txid = "";
    String method = "callTest1(address)";
    String para = "\"" + Base58.encode58Check(contractAddressTest1) + "\"";
    txid = PublicMethed.triggerContract(contractAddressCaller,
        method, para, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("getResult: " + infoById.get().getResultValue());
    Assert.assertEquals("SUCESS", infoById.get().getResult().toString());
    List<Protocol.TransactionInfo.Log> logList = infoById.get().getLogList();
    if (!Objects.isNull(logList)) {
      for (Protocol.TransactionInfo.Log log : logList) {
        //logger.info("LOG data info:" + tmp);
        Assert.assertEquals("fallback",
            PublicMethed.getContractStringMsg(log.getData().toByteArray()));
      }
    }
  }

  @Test(enabled = true, description = "contract test01 has fallback method")
  public void test012Fallback() {
    String txid = "";
    String method = "callTest2(address)";
    String para = "\"" + Base58.encode58Check(contractAddressTest1) + "\"";
    txid = PublicMethed.triggerContract(contractAddressCaller,
        method, para, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("getResult: " + infoById.get().getResultValue());
    Assert.assertEquals("REVERT", infoById.get().getReceipt().getResult().toString());
  }

  @Test(enabled = true, description = "contract test01 has fallback method")
  public void test013Fallback() {
    String txid = "";
    txid = PublicMethed.triggerContract(contractAddressTest1,
        "hello()", "#", false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("getResult: " + infoById.get().getResultValue());
    Assert.assertEquals("SUCESS", infoById.get().getResult().toString());

    txid = PublicMethed.triggerContract(contractAddressTest1,
        "hello2()", "#", false,
        100000, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("result:" + infoById.get().getReceipt().getResult());
    Assert.assertEquals("REVERT", infoById.get().getReceipt().getResult().toString());
  }

  @Test(enabled = true, description = "contract test02 has fallback payable method")
  public void test021FallbackPayable() {
    Protocol.Account info;
    GrpcAPI.AccountResourceMessage resourceInfo = PublicMethed
        .getAccountResource(contractExcAddress, blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    logger.info("beforeBalance:" + beforeBalance);
    String txid = "";
    long value = 10000;
    txid = PublicMethed.triggerContract(contractAddressTest2, "hello()", "#", false, value,
        maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("result:" + infoById.get().getReceipt().getResult());
    Assert.assertEquals("SUCESS", infoById.get().getResult().toString());
    Long fee = infoById.get().getFee();
    logger.info("fee:" + fee);
    Protocol.Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull1);
    GrpcAPI.AccountResourceMessage resourceInfoafter = PublicMethed
        .getAccountResource(contractExcAddress, blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    logger.info("afterBalance:" + afterBalance);
    Assert.assertTrue(afterBalance + fee + value == beforeBalance);

    String method = "callTest2(address)";
    String para = "\"" + Base58.encode58Check(contractAddressTest2) + "\"";
    txid = PublicMethed.triggerContract(contractAddressCaller, method, para, false, value,
        maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("callTest2 result:" + infoById.get().getReceipt().getResult());
    Assert.assertEquals("SUCESS", infoById.get().getResult().toString());
    fee = infoById.get().getFee();
    logger.info("callTest2 fee:" + fee);
    infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull1);
    resourceInfoafter = PublicMethed
        .getAccountResource(contractExcAddress, blockingStubFull1);
    Long afterBalance2 = infoafter.getBalance();
    logger.info("callTest2 afterBalance:" + afterBalance);
    Assert.assertTrue(afterBalance2 + fee + value == afterBalance);
  }

  @Test(enabled = true, description = "contract TestPayable has fallback and receive")
  public void test041FallbackReceive() {
    String txid = "";
    String method = "callTestPayable1(address)";
    String para = "\"" + Base58.encode58Check(contractAddressTestPayable) + "\"";
    txid = PublicMethed.triggerContract(contractAddressCaller,
        method, para, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("getResult: " + infoById.get().getResultValue());
    Assert.assertEquals("SUCESS", infoById.get().getResult().toString());
    Assert.assertEquals("fallback",
        PublicMethed.getContractStringMsg(infoById.get().getLog(0).getData().toByteArray()));
    Assert.assertEquals("receive",
        PublicMethed.getContractStringMsg(infoById.get().getLog(1).getData().toByteArray()));
  }

  @Test(enabled = true, description = "contract TestPayable has fallback and receive")
  public void test042FallbackReceive() {
    Protocol.Account info;
    GrpcAPI.AccountResourceMessage resourceInfo = PublicMethed
        .getAccountResource(contractExcAddress, blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    String txid = "";
    Long beforeBalance = info.getBalance();
    logger.info("beforeBalance:" + beforeBalance);
    String method = "callTest2(address)";
    long value = 10000;
    String para = "\"" + Base58.encode58Check(contractAddressTestPayable) + "\"";
    txid = PublicMethed.triggerContract(contractAddressCaller,
        method, para, false,
        value, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals("fallback",
        PublicMethed.getContractStringMsg(infoById.get().getLog(0).getData().toByteArray()));

    Long fee = infoById.get().getFee();
    logger.info("fee:" + fee);
    Protocol.Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull1);
    GrpcAPI.AccountResourceMessage resourceInfoafter = PublicMethed
        .getAccountResource(contractExcAddress, blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    logger.info("afterBalance:" + afterBalance);
    Assert.assertTrue(afterBalance + fee + value == beforeBalance);
  }

  @Test(enabled = true, description = "contract TestPayable has fallback and receive")
  public void test05FallbackReceive() {
    String txid = "";
    long value = 10000;
    txid = PublicMethed.triggerContract(contractAddressTestPayable,
        "method()", "#", false,
        value, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals("SUCESS", infoById.get().getResult().toString());
    Assert.assertEquals("fallback",
        PublicMethed.getContractStringMsg(infoById.get().getLog(0).getData().toByteArray()));

    Protocol.Account infoafter = PublicMethed
        .queryAccount(contractAddressTestPayable, blockingStubFull);
    GrpcAPI.AccountResourceMessage resourceInfoafter = PublicMethed
        .getAccountResource(contractAddressTestPayable,
            blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    logger.info("contract balance:" + afterBalance.longValue());
    Assert.assertEquals(11000, afterBalance.longValue());

    txid = PublicMethed.triggerContract(contractAddressTestPayable,
        "#", "#", false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.get().getResult().toString());
    Assert.assertEquals("SUCESS", infoById.get().getResult().toString());
    Assert.assertEquals("receive",
        PublicMethed.getContractStringMsg(infoById.get().getLog(0).getData().toByteArray()));

  }

  //@AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed
        .freedResource(contractAddressTest0, contractExcKey, testNetAccountAddress,
            blockingStubFull);
    PublicMethed
        .freedResource(contractAddressTest1, contractExcKey, testNetAccountAddress,
            blockingStubFull);
    PublicMethed
        .freedResource(contractAddressTest2, contractExcKey, testNetAccountAddress,
            blockingStubFull);
    PublicMethed
        .freedResource(contractAddressTestPayable, contractExcKey, testNetAccountAddress,
            blockingStubFull);
    PublicMethed
        .freedResource(contractAddressCaller, contractExcKey, testNetAccountAddress,
            blockingStubFull);
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