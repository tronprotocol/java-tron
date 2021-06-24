package stest.tron.wallet.onlinestress.tvm;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.WalletGrpc;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.ChainParameters;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Sha256Hash;

@Slf4j
public class StatisticTransactionTimeoutRate {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("witness.key5");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("witness.key4");
  private final byte[] testAddress003 = PublicMethed.getFinalAddress(testKey003);

  private final String testKey004 = Configuration.getByPath("testng.conf")
      .getString("witness.key3");
  private final byte[] testAddress004 = PublicMethed.getFinalAddress(testKey004);
  private final String testKey006 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress6 = PublicMethed.getFinalAddress(testKey006);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  ArrayList<String> txidList = new ArrayList<String>();
  Optional<TransactionInfo> infoById = null;
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
  private String fullnode = "47.94.243.150:50051";
  private String fullnode1 = "47.94.243.150:50051";
  private static byte[] unitrAddress =
      WalletClient.decodeFromBase58Check("TEgfD7cGY7xHuoGuyDBdMicysbbZhpjwx9");
  private static byte[] priceAddress =
      WalletClient.decodeFromBase58Check("TSjNzNRKdQMZtazPD7dkMi1Tt8Zj4ZC3wA");
  private static byte[] trxAddress =
      WalletClient.decodeFromBase58Check("TVEvT4vNh3oAabHjxPhgU11Se9riqatQ9h");
  private static byte[] sunAddress =
      WalletClient.decodeFromBase58Check("TZAe6nHgHRU9BwHax1Do8MAeLQg8ikXzLZ");
  private static byte[] usdtAddress =
      WalletClient.decodeFromBase58Check("TVrvkKyDqrrveLkZ67qDYJi29QYdG8iAJi");
  private static byte[] usdjAddress =
      WalletClient.decodeFromBase58Check("TNP36KeZaAzCCnjgNHm7w3dCFkKDsQ9Wh3");
  private static byte[] winAddress =
      WalletClient.decodeFromBase58Check("TUU67mPrNxJS53AtpySji5LQqB5eZgnxu4");
  private static byte[] btcAddress =
      WalletClient.decodeFromBase58Check("TDAJq9iNagWNScz18xLx6HfyJf8ktp1aru");
  private static byte[] jstAddress =
      WalletClient.decodeFromBase58Check("TMxwHannVrcp1HteiJTW1ZDDAS6iEsPJ3s");
  private static byte[] wbttAddress =
      WalletClient.decodeFromBase58Check("TMDx1DFX62v7wfpByMKRbWX8xZhiU6w5s7");
  private static byte[] ethAddress =
      WalletClient.decodeFromBase58Check("TEsaQVSi84WSASUvUxqN81b6kUGghV5dzH");
  private static byte[] eth2Address =
      WalletClient.decodeFromBase58Check("TL1yMm8YUbW8TkwC3J1Qfy3L6uGQvaDzrY");
  private static byte[] eth3Address =
      WalletClient.decodeFromBase58Check("TQeWYubxpW1h6Kc5L289r26FGUK4hd6xS1");
  private static byte[] eth4Address =
      WalletClient.decodeFromBase58Check("TVT81JPzTXsQCWCAydVPDecn2xFnXGkAV9");
  private static byte[] eth5Address =
      WalletClient.decodeFromBase58Check("TCr81A6pA9qZATBz36GQUBySc74j77HQg8");

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
    PublicMethed.printAddress(testKey002);
    PublicMethed.printAddress(testKey003);
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
    beforeTime = System.currentTimeMillis();
  }

  private String toStr() {
    HashMap<String, byte[]> addressMap = new HashMap<>();
    addressMap.put("trx", trxAddress);
    addressMap.put("usdj", usdjAddress);
    addressMap.put("jst", jstAddress);
    addressMap.put("usdt", usdtAddress);
    addressMap.put("sun", sunAddress);
    addressMap.put("win", winAddress);
    addressMap.put("btc", btcAddress);
    addressMap.put("wbtt", wbttAddress);
    addressMap.put("eth", ethAddress);
    addressMap.put("eth2", eth2Address);
    addressMap.put("eth3", eth3Address);
    addressMap.put("eth4", eth4Address);
    String addressStr = "";
    for (String ctoken:addressMap.keySet()) {
      addressStr += Base58.encode58Check(addressMap.get(ctoken))+", ";
    }
    System.out.println("addressStr: "+addressStr);
    return addressStr;
  }

  @Test(enabled = true,threadPoolSize = 1, invocationCount = 1)
  public void scanBlock() {
    String addressStr = toStr();
    Long startNum = 31165351L;
    Long endNum = 31165543L;
    Integer totalNum = 0;
    Integer successNum = 0;
    Integer failedNum = 0;
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    String contractAddresses = "TPSxiJuCgyZUjLs8PhV9L3vM2ZUqkxqoee,\n"
        + "TPu8gN2jBKakVtK1tRZE1fzF4ugV5S8emr,\n"
        + "TTScnCpJ4rvDYTAYLQSb1TfUe6n9rNVV5r,\n"
        + "TQTfdhUbX6CYMjdJeMRNEwatibF8jiPsFt,\n"
        + "TBG8YFY23a6pd47BKw1TDYJqeW1TTy9HZp,\n"
        + "TM5fhZbqth2Cbr1Z8TEWZMFCJqXUJQYp8Z,\n"
        + "TNyBXHVbgUjF9zB7NGpcJ2suq7os1UByDC,\n"
        + "TVmD8sH38KvBWH6N4rnSFjmxbTmPEmiTMo,\n"
        + "TVx8jELiVpNvgj2mxd6nbQ8EQV59ovgE3f,\n"
        + "TTuzqgesoQAd5VkTHXtVahM3sJApaEbLNs,\n"
        + "TQYaDoPzAUUs5eCBbM2PrLMbNSRXqQvmkh,\n"
        + "TYTeYCQWpg49TV7VogNw38KvpxzHoQ37yz,\n"
        + "TLiNbBysgaA4V84HaUttczRXP4coGurbnM,\n"
        + "TDs6Eh6XjcCV91gpUWAF7tzCW2wrJv3gd3";
//    String contractAddresses = "TUeun3aBssMSLhrqBV9XQM1DkT9jYzGYY9";
//    String contractAddresses = "TQ7bt9bdEFfMkuY3MAnLekvndLdev7r3un";
//    String contractAddresses = "TUeun3aBssMSLhrqBV9XQM1DkT9jYzGYY9,TQ7bt9bdEFfMkuY3MAnLekvndLdev7r3un";
    while (startNum <= endNum) {
      logger.info("scan block num:" + startNum);
      builder.setNum(startNum);
      List<Transaction>  transactionList = blockingStubFull
          .getBlockByNum(builder.build()).getTransactionsList();
      Integer transactionNumInThisBlock = transactionList.size();
      for (Transaction transaction : transactionList) {
        String txid = ByteArray.toHexString(Sha256Hash
            .hash(CommonParameter.getInstance().isECKeyCryptoEngine(),
                transaction.getRawData().toByteArray()));
        Optional<TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
//        System.out.println("transaction.getRawData().getContract(0).getType().toString():  "+transaction.getRawData().getContract(0).getType().toString());
        if (transaction.getRawData().getContract(0).getType().toString().equals("TriggerSmartContract")){
//          System.out.println("Base58.encode58Check(infoById.get().getContractAddress().toByteArray()):  "+Base58.encode58Check(infoById.get().getContractAddress().toByteArray()));
          if (contractAddresses.contains(Base58.encode58Check(infoById.get().getContractAddress().toByteArray()))) {
            totalNum ++;
            if (transaction.getRet(0).getContractRet().name().equals("SUCCESS") && infoById.get().getReceipt().getResult().equals(
                contractResult.SUCCESS) && (ByteArray.toInt(infoById.get().getContractResult(0).toByteArray())==0)) {
            /*if (transaction.getRet(0).getContractRet().name().equals("SUCCESS") && infoById.get().getReceipt().getResult().equals(
                contractResult.SUCCESS)) {*/
              successNum ++;
//              System.out.println("contractAddress: "+Base58.encode58Check(infoById.get().getContractAddress().toByteArray()));
            } else if (infoById.get().getReceipt().getResult().equals("OUT_OF_TIME"))  {
              System.out.println("failed ----- "+infoById.get().getReceipt().getResult());
              failedNum ++;
            } else {
              System.out.println("failed !!!!!!"+infoById.get().getReceipt().getResult()+"id: "+txid+"contractAddress: "+Base58.encode58Check(infoById.get().getContractAddress().toByteArray()));
              failedNum ++;
            }
          } else {
            System.out.println("addressStr not contains txid:"+txid);
          }
        }
      }
      startNum++;
    }
    logger.info("successNum:" + successNum);
    logger.info("failedNum:" + failedNum);
    logger.info("totalNum:" + totalNum);
    logger.info("failed rate:" + (double)failedNum / (double)totalNum);
  }

  public void write(String msg) {
    FileWriter fw = null;
    try {
      //如果文件存在，则追加内容；如果文件不存在，则创建文件
      File f=new File("/Users/wqq/Documents/Trc20-exchange.txt");
      fw = new FileWriter(f, true);
    } catch (IOException e) {
      e.printStackTrace();
    }
    PrintWriter pw = new PrintWriter(fw);
    pw.println(msg);
    pw.flush();
    try {
      fw.flush();
      pw.close();
      fw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test(enabled = true, threadPoolSize = 1, invocationCount = 1)
  public void storageAndCpu() {
    Random rand = new Random();
    Integer randNum = rand.nextInt(30) + 1;
    randNum = rand.nextInt(4000);

    Long maxFeeLimit = 1000000000L;
    String contractName = "StorageAndCpu" + Integer.toString(randNum);
    String code = Configuration.getByPath("testng.conf")
        .getString("code.code_TestStorageAndCpu_storageAndCpu");
    String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_TestStorageAndCpu_storageAndCpu");
    PublicMethed
        .freezeBalanceGetEnergy(fromAddress, 1000000000000L, 3, 1, testKey002, blockingStubFull);
    byte[] contractAddress = PublicMethed.deployContract(contractName, abi, code,
        "", maxFeeLimit,
        0L, 100, null, testKey002, fromAddress, blockingStubFull);
    try {
      Thread.sleep(30000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    String txid;

    ChainParameters chainParameters = blockingStubFull
        .getChainParameters(EmptyMessage.newBuilder().build());
    Optional<ChainParameters> getChainParameters = Optional.ofNullable(chainParameters);

    Integer i = 1;
    while (i++ < 8000) {
      String initParmes = "\"" + "930" + "\"";
      txid = PublicMethed.triggerContract(contractAddress,
          "testUseCpu(uint256)", "9100", false,
          0, maxFeeLimit, fromAddress, testKey002, blockingStubFull);
      txid = PublicMethed.triggerContract(contractAddress,
          "storage8Char()", "", false,
          0, maxFeeLimit, fromAddress, testKey002, blockingStubFull);
      //storage 9 EnergyUsageTotal is  211533, 10 is 236674, 5 is 110969,21 is 500000
      txid = PublicMethed.triggerContract(contractAddress,
          "testUseStorage(uint256)", "21", false,
          0, maxFeeLimit, fromAddress, testKey002, blockingStubFull);
      //logger.info("i is " +Integer.toString(i) + " " + txid);
      //txidList.add(txid);
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      if (i % 10 == 0) {
        chainParameters = blockingStubFull
            .getChainParameters(EmptyMessage.newBuilder().build());
        getChainParameters = Optional.ofNullable(chainParameters);
        logger.info(getChainParameters.get().getChainParameter(22).getKey());
        logger.info(Long.toString(getChainParameters.get().getChainParameter(22).getValue()));
        logger.info(getChainParameters.get().getChainParameter(23).getKey());
        logger.info(Long.toString(getChainParameters.get().getChainParameter(23).getValue()));

      }
    }
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    /*
    afterTime = System.currentTimeMillis();
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    currentBlock = blockingStubFull1.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    afterBlockNum = currentBlock.getBlockHeader().getRawData().getNumber() + 2;
    Long blockNum = beforeBlockNum;
    Integer txsNum = 0;
    Integer topNum = 0;
    Integer totalNum = 0;
    Long energyTotal = 0L;
    String findOneTxid = "";

    NumberMessage.Builder builder = NumberMessage.newBuilder();
    while (blockNum <= afterBlockNum) {
      builder.setNum(blockNum);
      txsNum = blockingStubFull1.getBlockByNum(builder.build()).getTransactionsCount();
      totalNum = totalNum + txsNum;
      if (topNum < txsNum) {
        topNum = txsNum;
        findOneTxid = ByteArray.toHexString(Sha256Hash.hash(blockingStubFull1
            .getBlockByNum(builder.build()).getTransactionsList().get(2)
            .getRawData().toByteArray()));
        //logger.info("find one txid is " + findOneTxid);
      }

      blockNum++;
    }
    Long costTime = (afterTime - beforeTime - 31000) / 1000;
    logger.info("Duration block num is  " + (afterBlockNum - beforeBlockNum - 11));
    logger.info("Cost time are " + costTime);
    logger.info("Top block txs num is " + topNum);
    logger.info("Total transaction is " + (totalNum - 30));
    logger.info("Average Tps is " + (totalNum / costTime));

    infoById = PublicMethed.getTransactionInfoById(findOneTxid, blockingStubFull1);
    Long oneEnergyTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("EnergyTotal is " + oneEnergyTotal);
    logger.info("Average energy is " + oneEnergyTotal * (totalNum / costTime));
*/

    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}