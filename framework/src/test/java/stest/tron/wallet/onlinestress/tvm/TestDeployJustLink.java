package stest.tron.wallet.onlinestress.tvm;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.WalletGrpc;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class TestDeployJustLink {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private byte[] jstContractAddress;
  private byte[] jstMidContractAddress;
  private byte[] aggContractAddress;
  List<String> oracleContractAddressList = new ArrayList<String>();
  Optional<TransactionInfo> infoById = null;
  int success = 0;
  int timeout = 0;
  int dup = 0;
  Long beforeTime;
  Long afterTime;
  Long beforeBlockNum;
  Long afterBlockNum;
  Block currentBlock;
  Long currentBlockNum;
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);

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
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
    currentBlock = blockingStubFull1.getNowBlock(EmptyMessage.newBuilder().build());
    beforeBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();

    // justlink
    //Only execute once
    deployJst();
    deployJustmid();
    deployOracles();
    deployAgg();
    updateRequestDetails();
    transferJst();

    beforeTime = System.currentTimeMillis();
  }

  @Test(enabled = true, threadPoolSize = 1, invocationCount = 1)
  public void triggerJustlink() {
//    while (true) {
    Random rand = new Random();
    Integer randNum = rand.nextInt(30) + 1;
    randNum = rand.nextInt(4000);

    // agg: TYF3ayAEDQho7r6XhmKfqDb8tpwaAGhoTN
    String txid = PublicMethed
        .triggerContract(aggContractAddress,
            "requestRateUpdate()", "#", false,
            randNum, 100000000L, fromAddress, testKey002, blockingStubFull);
      /*PublicMethed.waitProduceNextBlock(blockingStubFull);
      logger.info(txid);
      Optional<TransactionInfo> infoById = PublicMethed
          .getTransactionInfoById(txid, blockingStubFull);
      logger.info("txid : --- " + txid);
      //    logger.info("infobyid : --- " + infoById);
      if (infoById.get().getReceipt().getResult().toString().equals("SUCCESS")) {
        success += 1;
      } else if (infoById.get().getResMessage().toStringUtf8().contains("CPU timeout")) {
        timeout += 1;
      }*/
    //    Assert.assertTrue(infoById.get().getResultValue() == 0);
//    }
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    afterTime = System.currentTimeMillis();
    Long costTime = (afterTime - beforeTime) / 1000;
    int totalNum = success + timeout;
    double sucTps = success / costTime;
    double tps =
        (double) (Math.round(totalNum * 100) / 100.0) / (double) (Math.round(costTime * 100)
            / 100.0);
    logger.info("success:" + success + ",timeout:" + timeout);
    logger.info(
        "costTime:" + costTime + ",totalNum:" + totalNum + ",sucTps:" + sucTps + ",tps:" + tps);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  public void deployJst() {
    String filePath = "./src/test/resources/soliditycode/Jst.sol";
    String contractName = "Jst";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    logger.info("--------------------deployJst----------------------------");
    logger.info("code:" + code + "abi:" + abi);
    String constructorStr = "constructor(address)";
    String data = "\"" + Base58.encode58Check(fromAddress) + "\"";
    String txid = PublicMethed
        .deployContractWithConstantParame(contractName, abi, code, constructorStr, data, "",
            maxFeeLimit, 0L, 100, null, testKey002, fromAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    System.out.println(info);
    Assert.assertTrue(info.get().getResultValue() == 0);
    jstContractAddress = info.get().getContractAddress().toByteArray();
    Assert.assertNotNull(jstContractAddress);
  }

  public void deployJustmid() {
    String filePath = "./src/test/resources/soliditycode/JustMid.sol";
    String contractName = "JustMid";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    logger.info("--------------------deployJustmid----------------------------");
    logger.info("code:" + code + "abi:" + abi);
    String constructorStr = "constructor(address)";
    String data = "\"" + Base58.encode58Check(jstContractAddress) + "\"";
    String txid = PublicMethed
        .deployContractWithConstantParame(contractName, abi, code, constructorStr, data, "",
            maxFeeLimit, 0L, 100, null, testKey002, fromAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    System.out.println(info);
    Assert.assertTrue(info.get().getResultValue() == 0);
    jstMidContractAddress = info.get().getContractAddress().toByteArray();
    Assert.assertNotNull(jstMidContractAddress);
  }

  public void deployOracles() {
    String filePath = "./src/test/resources/soliditycode/TronOracles.sol";
    String contractName = "Oracle";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    logger.info("--------------------deployOracles----------------------------");
    logger.info("code:" + code + "abi:" + abi);
    String constructorStr = "constructor(address,address)";
    String data = "\"" + Base58.encode58Check(jstContractAddress) + "\",\"" + Base58
        .encode58Check(jstMidContractAddress) + "\"";
    for (int i = 0; i < 7; i++) {
      String txid = PublicMethed
          .deployContractWithConstantParame(contractName, abi, code, constructorStr, data, "",
              maxFeeLimit, 0L, 100, null, testKey002, fromAddress, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      Optional<TransactionInfo> info = PublicMethed
          .getTransactionInfoById(txid, blockingStubFull);
      System.out.println(info);
      Assert.assertTrue(info.get().getResultValue() == 0);
      String oracleContractAddress = Base58
          .encode58Check(info.get().getContractAddress().toByteArray());
      Assert.assertNotNull(oracleContractAddress);
      oracleContractAddressList.add(oracleContractAddress);
    }
  }

  public void deployAgg() {
    String filePath = "./src/test/resources/soliditycode/TronUser.sol";
    String contractName = "Aggregator";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    logger.info("--------------------deployAgg----------------------------");
    logger.info("code:" + code + "abi:" + abi);
    String constructorStr = "constructor(address,address)";
    String data = "\"" + Base58.encode58Check(jstContractAddress) + "\",\"" + Base58
        .encode58Check(jstMidContractAddress) + "\"";
    String txid = PublicMethed
        .deployContractWithConstantParame(contractName, abi, code, constructorStr, data, "",
            maxFeeLimit, 0L, 100, null, testKey002, fromAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    System.out.println(info);
    Assert.assertTrue(info.get().getResultValue() == 0);
    aggContractAddress = info.get().getContractAddress().toByteArray();
    Assert.assertNotNull(aggContractAddress);
    logger.info("aggContractAddress: " + Base58.encode58Check(aggContractAddress));
  }

  public void updateRequestDetails() {
    String methodStr = "updateRequestDetails(uint128,uint128,address[],bytes32[])";
    String oracleAddressParam = "[";
    for (int i = 0; i < oracleContractAddressList.size(); i++) {
      if (i == 0) {
        oracleAddressParam += "\"" + oracleContractAddressList.get(i) + "\"";
      } else {
        oracleAddressParam += ",\"" + oracleContractAddressList.get(i) + "\"";
      }
    }
    oracleAddressParam += "],";
    String data = ""
        + "\"1\","
        + "\"7\","
        + oracleAddressParam
        + "[\"bb347a9a63324fd995a7159cb0c8348a\",\"40691f5fd4b64ab4a5442477ed484d80\",\"f7ccb652cc254a19b0b954c49af25926\",\"38cd68072a6c4a0ca05e9b91976cf4f1\",\"328697ef599043e1a301ae985d06aabf\",\"239ff4228974435ea33f7c32cb46d297\",\"10ee483bad154f41ac58fdb4010c2c63\"]";
    logger.info("data: " + data);
    String txid = PublicMethed
        .triggerContract(aggContractAddress, methodStr, data, false, 0, maxFeeLimit,
            fromAddress, testKey002, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo option = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull).get();
    long energyCost = option.getReceipt().getEnergyUsageTotal();
    logger.info("energyCost: " + energyCost);
    Assert.assertTrue(option.getResultValue() == 0);
  }

  public void transferJst() {
    String data = "\"" + Base58.encode58Check(aggContractAddress) + "\"," + 5000000000000000000l;
    String txid = PublicMethed
        .triggerContract(jstContractAddress, "transfer(address,uint256)", data, false,
            0, 100000000L, fromAddress, testKey002, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info(txid);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

  }

  public void triggerAgg() {
    String txid = PublicMethed
        .triggerContract(aggContractAddress, "requestRateUpdate()", "#", false,
            0, 100000000L, fromAddress, testKey002, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info(txid);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("txid : --- " + txid);
    logger.info("infobyid : --- " + infoById);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

  }
}