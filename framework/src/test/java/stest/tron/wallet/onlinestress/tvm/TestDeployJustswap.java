package stest.tron.wallet.onlinestress.tvm;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class TestDeployJustswap {

  private static final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private static final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private byte[] justswapFactoryContractAddress;
  private byte[] justswapExchangeContractAddress;
  private byte[] JstAddress;
  private byte[] TstAddress;
  private byte[] JstExchangeAddress;
  private byte[] TstExchangeAddress;
  long deadline;
  List<String> oracleContractAddressList = new ArrayList<String>();
  Optional<TransactionInfo> infoById = null;
  Long beforeTime;
  Long afterTime;
  Long beforeBlockNum;
  Long afterBlockNum;
  Block currentBlock;
  Long currentBlockNum;

  private static String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private static ManagedChannel channelFull = ManagedChannelBuilder.forTarget(fullnode)
      .usePlaintext(true)
      .build();
  private static WalletGrpc.WalletBlockingStub blockingStubFull = WalletGrpc
      .newBlockingStub(channelFull);
  private ManagedChannel channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
      .usePlaintext(true)
      .build();
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;


  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);

    DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    java.util.Date date = null;
    try {
      date = df.parse("2020-12-07");
    } catch (ParseException e) {
      e.printStackTrace();
    }
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    long timestamp = cal.getTimeInMillis();
    deadline = timestamp / 1000;
    System.out.println("deadline: " + deadline);

    deployJustswapFactory();
    deployJustswapExchange();
    initializeFactory();
    createExchange1();
    createExchange2();
    addLiquidity1();
    addLiquidity2();

  }

  @Test(enabled = true, threadPoolSize = 1, invocationCount = 1)
  public void triggerJustswap() {
    // JstAddress: TJH776bk81opDCwBNYpAQPPudHQ5gbexZ9
    // TstExchangeAddress: TSoF1QsR3jJrakVCyNQaTjQAeQrMWqhpbS

    while (true) {
      Random rand = new Random();
      Integer randNum = rand.nextInt(30) + 1;
      randNum = rand.nextInt(4000);

      String data =
          "\"100000\",\"1\",\"1\",\"" + deadline + "\",\"" + Base58.encode58Check(JstAddress)
              + "\"";
      String txid = PublicMethed
          .triggerContract(TstExchangeAddress,
              "tokenToTokenSwapInput(uint256,uint256,uint256,uint256,address)", data, false,
              randNum, maxFeeLimit, fromAddress, testKey002, blockingStubFull);
      /*PublicMethed.waitProduceNextBlock(blockingStubFull);
      logger.info(txid);
      Optional<TransactionInfo> infoById = PublicMethed
          .getTransactionInfoById(txid, blockingStubFull);
      logger.info("txid : --- " + txid);
      logger.info("infobyid : --- " + infoById);
      Assert.assertTrue(infoById.get().getResultValue() == 0);*/
    }


    /*String data =
        "\"10000000000\",\"1\",\"1\",\"" + deadline + "\",\"" + Base58.encode58Check(JstAddress)
            + "\"";
    String txid = PublicMethed
        .triggerContract(TstExchangeAddress,
            "trxToTokenSwapInput(uint256,uint256)", data, false,
            1, maxFeeLimit, fromAddress, testKey002, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info(txid);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("txid : --- " + txid);
    logger.info("infobyid : --- " + infoById);
    Assert.assertTrue(infoById.get().getResultValue() == 0);*/
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  public void deployJustswapFactory() {
    String filePath = "./src/test/resources/soliditycode/JustswapFactory.sol";
    String contractName = "JustswapFactory";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    logger.info("--------------------deployJustswapFactory----------------------------");
    logger.info("abi:" + abi);
    logger.info("code:" + code);
    justswapFactoryContractAddress = PublicMethed
        .deployContract(contractName, abi, code, "",
            maxFeeLimit, 0L, 100, null, testKey002, fromAddress, blockingStubFull);
    Assert.assertNotNull(justswapFactoryContractAddress);
    logger.info(
        "justswapFactoryContractAddress:" + Base58.encode58Check(justswapFactoryContractAddress));
  }

  public void deployJustswapExchange() {
    String filePath = "./src/test/resources/soliditycode/JustswapExchange.sol";
    String contractName = "JustswapExchange";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    logger.info("--------------------deployJustswapExchange----------------------------");
    logger.info("abi:" + abi);
    logger.info("code:" + code);
    justswapExchangeContractAddress = PublicMethed
        .deployContract(contractName, abi, code, "",
            maxFeeLimit, 0L, 100, null, testKey002, fromAddress, blockingStubFull);
    Assert.assertNotNull(justswapExchangeContractAddress);
    logger
        .info("justswapExchangeContractAddress: " + Base58
            .encode58Check(justswapExchangeContractAddress));

  }

  public void initializeFactory() {
    String data = "\"" + Base58.encode58Check(justswapExchangeContractAddress) + "\"";
    String txid = PublicMethed
        .triggerContract(justswapFactoryContractAddress, "initializeFactory(address)", data, false,
            0, maxFeeLimit, fromAddress, testKey002, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
  }

  public void createExchange1() {
    String filePath = "./src/test/resources/soliditycode/Jst.sol";
    String contractName = "Jst";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    logger.info("--------------------createExchange1----------------------------");
    logger.info("abi:" + abi);
    logger.info("code:" + code);
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
    JstAddress = info.get().getContractAddress().toByteArray();
    Assert.assertNotNull(JstAddress);
    logger.info("JstAddress: " + Base58.encode58Check(JstAddress));

    data = "\"" + Base58.encode58Check(JstAddress) + "\"";
    txid = PublicMethed
        .triggerContract(justswapFactoryContractAddress, "createExchange(address)", data, false,
            0, maxFeeLimit, fromAddress, testKey002, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    byte[] a = infoById.get().getContractResult(0).toByteArray();
    byte[] d = subByte(a, 12, 20);
    logger.info("41" + ByteArray.toHexString(d));
    String contractResult = "41" + ByteArray.toHexString(d);
    Assert.assertNotNull(contractResult);
    JstExchangeAddress = ByteArray.fromHexString(contractResult);
    logger.info(
        "JstExchangeAddress: " + Base58.encode58Check(JstExchangeAddress));
  }

  public void createExchange2() {
    String filePath = "./src/test/resources/soliditycode/Tst.sol";
    String contractName = "Tst";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    logger.info("--------------------createExchange2----------------------------");
    logger.info("abi:" + abi);
    logger.info("code:" + code);
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
    TstAddress = info.get().getContractAddress().toByteArray();
    Assert.assertNotNull(TstAddress);
    logger.info("TstAddress: " + Base58.encode58Check(TstAddress));

    data = "\"" + Base58.encode58Check(TstAddress) + "\"";
    txid = PublicMethed
        .triggerContract(justswapFactoryContractAddress, "createExchange(address)", data, false,
            0, maxFeeLimit, fromAddress, testKey002, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    TstExchangeAddress = infoById.get().getContractAddress().toByteArray();
    byte[] a = infoById.get().getContractResult(0).toByteArray();
    byte[] d = subByte(a, 12, 20);
    logger.info("41" + ByteArray.toHexString(d));
    String contractResult = "41" + ByteArray.toHexString(d);
    Assert.assertNotNull(contractResult);
    TstExchangeAddress = ByteArray.fromHexString(contractResult);
    logger.info(
        "TstExchangeAddress: " + Base58.encode58Check(TstExchangeAddress));
  }

  public void addLiquidity1() {
    String data = "\"" + Base58.encode58Check(JstExchangeAddress) + "\",\"-1\"";
    String txid = PublicMethed
        .triggerContract(JstAddress, "approve(address,uint256)", data, false,
            0, maxFeeLimit, fromAddress, testKey002, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

//    String data = "\"1\",\"10000000000000000000\",\"" + deadline + "\"";
    String hex = Integer.toHexString(Integer.parseInt(String.valueOf(deadline)));
    String s = PublicMethed.addZeroForNum(hex, 64);
    logger.info("s:" + s);
    String data1 = "0000000000000000000000000000000000000000000000000000000000000001"
        + "00000000000000000000000000000000000000000000152d02c7e14af6800000"
        + s;
    txid = PublicMethed
        .triggerContract(JstExchangeAddress, "addLiquidity(uint256,uint256,uint256)", data1, true,
            100000000000l, maxFeeLimit, fromAddress, testKey002, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info(txid);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

  }

  public void addLiquidity2() {
    String data = "\"" + Base58.encode58Check(TstExchangeAddress) + "\",\"-1\"";
    String txid = PublicMethed
        .triggerContract(TstAddress, "approve(address,uint256)", data, false,
            0, maxFeeLimit, fromAddress, testKey002, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
//    String data = "\"1\",\"100000000000\",\"" + deadline + "\"";
    String hex = Integer.toHexString(Integer.parseInt(String.valueOf(deadline)));
    String s = PublicMethed.addZeroForNum(hex, 64);
    logger.info("s:" + s);
    String data1 = "0000000000000000000000000000000000000000000000000000000000000001"
        + "00000000000000000000000000000000000000000000000000038d7ea4c68000"
        + s;
    txid = PublicMethed
        .triggerContract(TstExchangeAddress, "addLiquidity(uint256,uint256,uint256)", data1, true,
            100000000000l, maxFeeLimit, fromAddress, testKey002, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info(txid);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

  }

  public byte[] subByte(byte[] b, int off, int length) {
    byte[] b1 = new byte[length];
    System.arraycopy(b, off, b1, 0, length);
    return b1;

  }

}