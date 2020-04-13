package stest.tron.wallet.dailybuild.assetissue.grammar;

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
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractGrammar002 {


  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  byte[] contractAddress = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] grammarAddress2 = ecKey1.getAddress();
  String testKeyForGrammarAddress2 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelSolidity = null;
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);

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
    PublicMethed.printAddress(testKeyForGrammarAddress2);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

    logger.info(Long.toString(PublicMethed.queryAccount(testNetAccountKey, blockingStubFull)
        .getBalance()));
  }


  @Test(enabled = true, description = "Interface type function")
  public void test1Grammar007() {
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed
        .sendcoin(grammarAddress2, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/contractGrammar002test1Grammar007_1.sol";
    String contractName = "Doug";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress2,
        grammarAddress2, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String initParmes = ByteArray.toHexString(contractAddress);
    String filePath1 = "src/test/resources/soliditycode/contractGrammar002test1Grammar007_2.sol";
    String contractName1 = "main";
    HashMap retMap1 = PublicMethed.getBycodeAbi(filePath1, contractName1);
    String code1 = retMap1.get("byteCode").toString() + "0000000000000000000000"
        + initParmes;
    String abi1 = retMap1.get("abI").toString();
    byte[] contractAddress1 = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            0L, 100, null, testKeyForGrammarAddress2,
            grammarAddress2, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull1);
    String txid = "";
    String number = "1";
    String txid1 = PublicMethed.triggerContract(contractAddress1,
        "dougOfage(uint256)", number, false,
        0, maxFeeLimit, grammarAddress2, testKeyForGrammarAddress2, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull1);
    Optional<TransactionInfo> infoById1 = PublicMethed
        .getTransactionInfoById(txid1, blockingStubFull1);

    Assert.assertTrue(infoById1.get().getResultValue() == 0);

    String number1 = "687777";
    String txid2 = PublicMethed.triggerContract(contractAddress1,
        "uintOfName(bytes32)", number1, false,
        0, maxFeeLimit, grammarAddress2, testKeyForGrammarAddress2, blockingStubFull);
    //    PublicMethed.waitProduceNextBlock(blockingStubFull);
    //    PublicMethed.waitProduceNextBlock(blockingStubFull1);
    Optional<TransactionInfo> infoById2 = PublicMethed
        .getTransactionInfoById(txid2, blockingStubFull1);

    Assert.assertTrue(infoById2.get().getResultValue() == 0);
  }

  @Test(enabled = true, description = "Abstract function")
  public void test2Grammar008() {
    String filePath = "src/test/resources/soliditycode/contractGrammar002test2Grammar008.sol";
    String contractName = "Cat";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress2,
        grammarAddress2, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String txid = "";
    txid = PublicMethed.triggerContract(contractAddress,
        "getContractName()", "#", false,
        0, maxFeeLimit, grammarAddress2, testKeyForGrammarAddress2, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    String returnString = ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray());
    Assert.assertEquals(returnString,
        "0000000000000000000000000000000000000000000000000000000000000020000000000000000000"
            + "000000000000000000000000000000000000000000000646656c696e650000000000000000000000000"
            + "000000000000000000000000000");
    String txid1 = PublicMethed.triggerContract(contractAddress,
        "utterance()", "#", false,
        0, maxFeeLimit, grammarAddress2, testKeyForGrammarAddress2, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    String returnString1 = ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray());
    Assert.assertEquals(returnString1,
        "6d69616f77000000000000000000000000000000000000000000000000000000");
  }

  @Test(enabled = true, description = "Gas, value test")
  public void test3Grammar010() {
    String filePath = "src/test/resources/soliditycode/contractGrammar002test3Grammar010.sol";
    String contractName = "Consumer";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        2000L, 100, null, testKeyForGrammarAddress2,
        grammarAddress2, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String contractName1 = "InfoFeed";
    HashMap retMap1 = PublicMethed.getBycodeAbi(filePath, contractName1);
    String code1 = retMap1.get("byteCode").toString();
    String abi1 = retMap1.get("abI").toString();

    byte[] contractAddress1 = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            0, 100, null, testKeyForGrammarAddress2,
            grammarAddress2, blockingStubFull);
    String txid = "";
    String initParmes = "\"" + Base58.encode58Check(contractAddress1) + "\"";
    txid = PublicMethed.triggerContract(contractAddress,
        "setFeed(address)", initParmes, false,
        0, maxFeeLimit, grammarAddress2, testKeyForGrammarAddress2, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String txid1 = PublicMethed.triggerContract(contractAddress,
        "callFeed()", "#", false,
        0, maxFeeLimit, grammarAddress2, testKeyForGrammarAddress2, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull1);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid1, blockingStubFull1);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
  }


  @Test(enabled = true, description = "Call a named function")
  public void test4Grammar011() {
    String filePath = "src/test/resources/soliditycode/contractGrammar002test4Grammar011.sol";
    String contractName = "C";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    byte[] contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress2,
        grammarAddress2, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    String number = "1" + "," + "2";
    String txid = PublicMethed.triggerContract(contractAddress,
        "f(uint256,uint256)", number, false,
        0, maxFeeLimit, grammarAddress2, testKeyForGrammarAddress2, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(returnnumber == 1);

    Optional<TransactionInfo> infoById1 = null;
    String txid1 = PublicMethed.triggerContract(contractAddress,
        "g()", "#", false,
        0, maxFeeLimit, grammarAddress2, testKeyForGrammarAddress2, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 0);
  }


  @Test(enabled = true, description = "Call a native function")
  public void test5Grammar012() {
    String filePath = "src/test/resources/soliditycode/contractGrammar002test4Grammar012.sol";
    String contractName = "rTest";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    byte[] contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress2,
        grammarAddress2, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    String txid = PublicMethed.triggerContract(contractAddress,
        "info()", "#", false,
        0, maxFeeLimit, grammarAddress2, testKeyForGrammarAddress2, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);

    Assert.assertTrue(infoById.get().getResultValue() == 0);

  }

  @Test(enabled = true, description = "Call a Destructor function")
  public void test6Grammar013() {
    String filePath = "src/test/resources/soliditycode/contractGrammar002test6Grammar013.sol";
    String contractName = "Counter";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    byte[] contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress2,
        grammarAddress2, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    String txid = PublicMethed.triggerContract(contractAddress,
        "getCount()", "#", false,
        0, maxFeeLimit, grammarAddress2, testKeyForGrammarAddress2, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(returnnumber == 0);

    Optional<TransactionInfo> infoById1 = null;
    String txid1 = PublicMethed.triggerContract(contractAddress,
        "increment()", "#", false,
        0, maxFeeLimit, grammarAddress2, testKeyForGrammarAddress2, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);

    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    Optional<TransactionInfo> infoById2 = null;
    String txid2 = PublicMethed.triggerContract(contractAddress,
        "getCount()", "#", false,
        0, maxFeeLimit, grammarAddress2, testKeyForGrammarAddress2, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById2 = PublicMethed.getTransactionInfoById(txid2, blockingStubFull);

    Assert.assertTrue(infoById2.get().getResultValue() == 0);

    Long returnnumber1 = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById2.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(returnnumber1 == 10);

    Optional<TransactionInfo> infoById3 = null;
    String txid3 = PublicMethed.triggerContract(contractAddress,
        "kill()", "#", false,
        0, maxFeeLimit, grammarAddress2, testKeyForGrammarAddress2, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById3 = PublicMethed.getTransactionInfoById(txid3, blockingStubFull);
    Assert.assertTrue(infoById3.get().getResultValue() == 0);

    Optional<TransactionInfo> infoById4 = null;
    String txid4 = PublicMethed.triggerContract(contractAddress,
        "getCount()", "#", false,
        0, maxFeeLimit, grammarAddress2, testKeyForGrammarAddress2, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(txid4 == null);


  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed.freedResource(grammarAddress2, testKeyForGrammarAddress2, testNetAccountAddress,
        blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

}
