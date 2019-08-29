package stest.tron.wallet.onlinestress;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.WalletGrpc;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.ChainParameters;
import org.tron.protos.Protocol.SmartContract;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Sha256Hash;

@Slf4j
public class TestStorageAndCpu {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("witness.key5");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("witness.key4");
  private final byte[] testAddress003 = PublicMethed.getFinalAddress(testKey003);

  private final String testKey004 = Configuration.getByPath("testng.conf")
      .getString("witness.key3");
  private final byte[] testAddress004 = PublicMethed.getFinalAddress(testKey004);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  ArrayList<String> txidList = new ArrayList<String>();

  Optional<TransactionInfo> infoById = null;
  Long beforeTime;
  Long afterTime;
  Long beforeBlockNum;
  Long afterBlockNum;
  Block currentBlock;
  Long currentBlockNum;


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
    currentBlock = blockingStubFull1.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    beforeBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    beforeTime = System.currentTimeMillis();
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

  String contract1Address = "TCgcqd7GvHu7bxw9SxWqQMvL1TK45zkbpZ";
  String contract1Result = "0101010101010101010101010101010101010101010101010101010101010101";
  String contract2Address = "TG6r7fNqMPtHmrF6Hdq1UmRQXjth9Vjv5N";
  String contract2Result = "0100010101010101010101010101010101010101010101010101010101010100";
  String contract3Address = "TXutmpeSzTYjhuXNhQekQMUgSEvPvX8NmW";
  String contract3Result = "0101010101010101000100010101010101010101010101010101010001010100";
  String contract4Address = "TAXNdVtoZ9bhHLe3ZCg4EecBwgBVMVVNei";
  String contract4Result = "0000000000000000000000000000000000000000000000000000000000000000";
  int contract1TransactionNum = 0;
  int contract1SuccessNum = 0;
  int contract2TransactionNum = 0;
  int contract2SuccessNum = 0;
  int contract3TransactionNum = 0;
  int contract3SuccessNum = 0;
  int contract4TransactionNum = 0;
  int contract4SuccessNum = 0;
  List<String> contract1ExceptionList = new ArrayList<>();
  List<String> contract2ExceptionList = new ArrayList<>();
  List<String> contract3ExceptionList = new ArrayList<>();
  List<String> contract4ExceptionList = new ArrayList<>();


  @Test(enabled = true, threadPoolSize = 1, invocationCount = 1)
  public void storageAndCpu1() {
    Long startBlockNum = 24110L;
    Long endBlockNum = 24210L;
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    Block nowBlock;
    while (startBlockNum <= endBlockNum) {
      builder.setNum(startBlockNum);
      nowBlock = blockingStubFull.getBlockByNum(builder.build());
      queryBlockTransactions(nowBlock);
      startBlockNum++;
    }

    logger.info(
        "contract1 success num:" + contract1SuccessNum + " , total num:" + contract1SuccessNum);
    logger.info(
        "contract2 success num:" + contract2SuccessNum + " , total num:" + contract2SuccessNum);
    logger.info(
        "contract3 success num:" + contract3SuccessNum + " , total num:" + contract3SuccessNum);
    logger.info(
        "contract4 success num:" + contract4SuccessNum + " , total num:" + contract4SuccessNum);

    logger.info("-----------List1-------------");
    printList(contract1ExceptionList);
    logger.info("-----------List2-----------");
    printList(contract2ExceptionList);
    logger.info("------------List3-----------");
    printList(contract3ExceptionList);
    logger.info("------------List4----------");
    printList(contract4ExceptionList);


  }

  public void printList(List<String> list) {
    for (int i = 0; i < list.size(); i++) {
      logger.info("txid:" + list.get(i));
    }
  }

  public void queryResult(String txid) {
    ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txid));
    BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
    TransactionInfo transactionInfo = blockingStubFull.getTransactionInfoById(request);

    if (Wallet.encode58Check(transactionInfo.getContractAddress().toByteArray())
        .equals(contract1Address)) {
      contract1TransactionNum++;
      if (ByteArray
          .toHexString(transactionInfo.getContractResult(0).toByteArray())
          .equals(contract1Result)) {
        contract1SuccessNum++;
        return;
      }
      if (!ByteArray
          .toHexString(transactionInfo.getContractResult(0).toByteArray()).isEmpty()) {
        contract1ExceptionList.add(txid);
        logger.info(txid);
      }
      return;
    }

    if (Wallet.encode58Check(transactionInfo.getContractAddress().toByteArray())
        .equals(contract2Address)) {
      contract2TransactionNum++;
      if (ByteArray
          .toHexString(transactionInfo.getContractResult(0).toByteArray())
          .equals(contract2Result)) {
        contract2SuccessNum++;
        return;
      }
      if (!ByteArray
          .toHexString(transactionInfo.getContractResult(0).toByteArray()).isEmpty()) {
        contract2ExceptionList.add(txid);
      }
      return;
    }

    if (Wallet.encode58Check(transactionInfo.getContractAddress().toByteArray())
        .equals(contract3Address)) {
      contract3TransactionNum++;
      if (ByteArray
          .toHexString(transactionInfo.getContractResult(0).toByteArray())
          .equals(contract3Result)) {
        contract3SuccessNum++;
        return;
      }
      if (!ByteArray
          .toHexString(transactionInfo.getContractResult(0).toByteArray()).isEmpty()) {
        contract3ExceptionList.add(txid);
      }
      return;
    }

    if (Wallet.encode58Check(transactionInfo.getContractAddress().toByteArray())
        .equals(contract4Address)) {
      contract4TransactionNum++;
      if (ByteArray
          .toHexString(transactionInfo.getContractResult(0).toByteArray())
          .equals(contract4Result)) {
        contract4SuccessNum++;
        return;
      }
      if (!ByteArray
          .toHexString(transactionInfo.getContractResult(0).toByteArray()).isEmpty()) {
        contract4ExceptionList.add(txid);
      }
      return;
    }

  }


  public void queryBlockTransactions(Block block) {
    for (Transaction transaction : block.getTransactionsList()) {
      queryResult(ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray())));
      //queryResult(ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray())));
      //queryResult(ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray())));
      //queryResult(ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray())));

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