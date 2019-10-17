package stest.tron.wallet.dailybuild.internaltransaction;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.spongycastle.util.encoders.Hex;
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

public class ContractInternalTransaction001 {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  byte[] contractAddress = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] internalTxsAddress = ecKey1.getAddress();
  String testKeyForinternalTxsAddress = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
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
    PublicMethed.printAddress(testKeyForinternalTxsAddress);
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

  @Test(enabled = true, description = "Create->call.Two-level nesting")
  public void testInternalTransaction001() {
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/"
        + "contractInternalTransaction001testInternalTransaction001.sol";
    String contractName = "A";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String contractName1 = "C";
    HashMap retMap1 = PublicMethed.getBycodeAbi(filePath, contractName1);
    String code1 = retMap1.get("byteCode").toString();
    String abi1 = retMap1.get("abI").toString();

    byte[] contractAddress1 = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String txid = "";
    String initParmes = "\"" + Base58.encode58Check(contractAddress1) + "\"";
    txid = PublicMethed.triggerContract(contractAddress,
        "test1(address)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    int transactionsCount = infoById.get().getInternalTransactionsCount();
    Assert.assertEquals(7, transactionsCount);
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());
    for (int i = 0; i < transactionsCount; i++) {
      Assert.assertFalse(infoById.get().getInternalTransactions(i).getRejected());
    }
    String initParmes2 = "\"" + Base58.encode58Check(contractAddress1) + "\",\"1\"";
    String txid1 = PublicMethed.triggerContract(contractAddress,
        "test2(address,uint256)", initParmes2, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    int transactionsCount1 = infoById1.get().getInternalTransactionsCount();
    Assert.assertEquals(10, transactionsCount1);
    dupInternalTrsansactionHash(infoById1.get().getInternalTransactionsList());
    for (int i = 0; i < transactionsCount1; i++) {
      Assert.assertFalse(infoById1.get().getInternalTransactions(i).getRejected());
    }
  }

  @Test(enabled = true, description = "There is one internalTransaction.Only call")
  public void testInternalTransaction002() {
    Assert.assertTrue(PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String filePath = "src/test/resources/soliditycode/"
        + "contractInternalTransaction001testInternalTransaction002.sol";
    String contractName = "A";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String contractName1 = "C";
    HashMap retMap1 = PublicMethed.getBycodeAbi(filePath, contractName1);
    String code1 = retMap1.get("byteCode").toString();
    String abi1 = retMap1.get("abI").toString();
    byte[] contractAddress1 = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String initParmes2 = "\"" + Base58.encode58Check(contractAddress1) + "\",\"1\"";
    String txid1 = PublicMethed.triggerContract(contractAddress,
        "test2(address,uint256)", initParmes2, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    int transactionsCount1 = infoById1.get().getInternalTransactionsCount();
    Assert.assertEquals(1, transactionsCount1);
    dupInternalTrsansactionHash(infoById1.get().getInternalTransactionsList());
    Assert.assertFalse(infoById1.get().getInternalTransactions(0).getRejected());
    String note = ByteArray
        .toStr(infoById1.get().getInternalTransactions(0).getNote().toByteArray());
    Assert.assertEquals("call", note);
    Assert.assertEquals(1,
        infoById1.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue());

  }

  @Test(enabled = true, description = "There is one internalTransaction.Only create")
  public void testInternalTransaction003() {
    Assert.assertTrue(PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/"
        + "contractInternalTransaction001testInternalTransaction003.sol";
    String contractName = "A";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String txid1 = PublicMethed.triggerContract(contractAddress,
        "transfer()", "#", false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    int transactionsCount1 = infoById1.get().getInternalTransactionsCount();
    Assert.assertEquals(1, transactionsCount1);
    dupInternalTrsansactionHash(infoById1.get().getInternalTransactionsList());

    Assert.assertFalse(infoById1.get().getInternalTransactions(0).getRejected());
    String note = ByteArray
        .toStr(infoById1.get().getInternalTransactions(0).getNote().toByteArray());
    Assert.assertEquals("create", note);
    Assert.assertEquals(10,
        infoById1.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue());

  }

  @Test(enabled = true, description = "Test suicide type in internalTransaction")
  public void testInternalTransaction004() {
    Assert.assertTrue(PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/"
        + "contractInternalTransaction001testInternalTransaction004.sol";
    String contractName = "A";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String contractName1 = "B";
    HashMap retMap1 = PublicMethed.getBycodeAbi(filePath, contractName1);
    String code1 = retMap1.get("byteCode").toString();
    String abi1 = retMap1.get("abI").toString();
    byte[] contractAddress1 = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            0, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String txid = "";
    String initParmes = "\"" + Base58.encode58Check(contractAddress)
        + "\",\"" + Base58.encode58Check(contractAddress1) + "\"";
    txid = PublicMethed.triggerContract(contractAddress1,
        "kill(address,address)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    int transactionsCount = infoById.get().getInternalTransactionsCount();
    Assert.assertEquals(2, transactionsCount);
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());
    for (int i = 0; i < transactionsCount; i++) {
      Assert.assertFalse(infoById.get().getInternalTransactions(i).getRejected());
    }
    String note = ByteArray
        .toStr(infoById.get().getInternalTransactions(0).getNote().toByteArray());
    String note1 = ByteArray
        .toStr(infoById.get().getInternalTransactions(1).getNote().toByteArray());
    Long vaule1 = infoById.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule2 = infoById.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Assert.assertEquals("call", note);
    Assert.assertEquals("suicide", note1);
    Assert.assertTrue(0 == vaule1);
    Assert.assertTrue(1000000L == vaule2);

    String txid1 = PublicMethed.triggerContract(contractAddress1,
        "kill2()", "#", false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    int transactionsCount1 = infoById1.get().getInternalTransactionsCount();
    Assert.assertEquals(3, transactionsCount1);
    dupInternalTrsansactionHash(infoById1.get().getInternalTransactionsList());
    String note3 = ByteArray
        .toStr(infoById1.get().getInternalTransactions(0).getNote().toByteArray());
    String note4 = ByteArray
        .toStr(infoById1.get().getInternalTransactions(1).getNote().toByteArray());
    String note5 = ByteArray
        .toStr(infoById1.get().getInternalTransactions(2).getNote().toByteArray());
    Assert.assertEquals("create", note3);
    Assert.assertEquals("call", note4);
    Assert.assertEquals("suicide", note5);
    for (int i = 0; i < transactionsCount1; i++) {
      Assert.assertFalse(infoById1.get().getInternalTransactions(i).getRejected());
      Assert.assertEquals(0,
          infoById1.get().getInternalTransactions(i).getCallValueInfo(0).getCallValue());

    }
  }

  @Test(enabled = true, description = "Type is create call")
  public void testInternalTransaction005() {
    Assert.assertTrue(PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/"
        + "contractInternalTransaction001testInternalTransaction005.sol";
    String contractName = "A";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String txid = "";

    txid = PublicMethed.triggerContract(contractAddress,
        "test1()", "#", false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 1);
    int transactionsCount = infoById.get().getInternalTransactionsCount();
    Assert.assertEquals(2, transactionsCount);
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());
    for (int i = 0; i < transactionsCount; i++) {
      Assert.assertTrue(infoById.get().getInternalTransactions(i).getRejected());
    }
    String note = ByteArray
        .toStr(infoById.get().getInternalTransactions(0).getNote().toByteArray());
    String note1 = ByteArray
        .toStr(infoById.get().getInternalTransactions(1).getNote().toByteArray());
    Long vaule1 = infoById.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule2 = infoById.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Assert.assertEquals("create", note);
    Assert.assertEquals("call", note1);
    Assert.assertTrue(10 == vaule1);
    Assert.assertTrue(0 == vaule2);

    String txid1 = PublicMethed.triggerContract(contractAddress,
        "test2()", "#", false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 1);
    int transactionsCount1 = infoById1.get().getInternalTransactionsCount();
    Assert.assertEquals(2, transactionsCount1);
    dupInternalTrsansactionHash(infoById1.get().getInternalTransactionsList());

    String note3 = ByteArray
        .toStr(infoById1.get().getInternalTransactions(0).getNote().toByteArray());
    String note4 = ByteArray
        .toStr(infoById1.get().getInternalTransactions(1).getNote().toByteArray());

    Long vaule3 = infoById1.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule4 = infoById1.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Assert.assertTrue(10 == vaule3);
    Assert.assertTrue(0 == vaule4);
    Assert.assertEquals("create", note3);
    Assert.assertEquals("call", note4);
    for (int i = 0; i < transactionsCount1; i++) {
      Assert.assertTrue(infoById1.get().getInternalTransactions(i).getRejected());


    }
  }

  @Test(enabled = true, description = "Type is create call call")
  public void testInternalTransaction006() {
    Assert.assertTrue(PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/"
        + "contractInternalTransaction001testInternalTransaction006.sol";
    String contractName = "A";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String txid = "";

    txid = PublicMethed.triggerContract(contractAddress,
        "test1()", "#", false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 1);
    int transactionsCount = infoById.get().getInternalTransactionsCount();
    Assert.assertEquals(3, transactionsCount);
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());

    for (int i = 0; i < transactionsCount; i++) {
      Assert.assertTrue(infoById.get().getInternalTransactions(i).getRejected());
    }
    String note = ByteArray
        .toStr(infoById.get().getInternalTransactions(0).getNote().toByteArray());
    String note1 = ByteArray
        .toStr(infoById.get().getInternalTransactions(1).getNote().toByteArray());
    String note2 = ByteArray
        .toStr(infoById.get().getInternalTransactions(1).getNote().toByteArray());
    Long vaule1 = infoById.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule2 = infoById.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Long vaule3 = infoById.get().getInternalTransactions(2).getCallValueInfo(0).getCallValue();

    Assert.assertEquals("create", note);
    Assert.assertEquals("call", note1);
    Assert.assertEquals("call", note2);
    Assert.assertTrue(10 == vaule1);
    Assert.assertTrue(0 == vaule2);
    Assert.assertTrue(0 == vaule3);

    String txid1 = PublicMethed.triggerContract(contractAddress,
        "test2()", "#", false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 1);
    int transactionsCount1 = infoById1.get().getInternalTransactionsCount();
    Assert.assertEquals(3, transactionsCount1);
    dupInternalTrsansactionHash(infoById1.get().getInternalTransactionsList());

    String note4 = ByteArray
        .toStr(infoById1.get().getInternalTransactions(0).getNote().toByteArray());
    String note5 = ByteArray
        .toStr(infoById1.get().getInternalTransactions(1).getNote().toByteArray());
    String note6 = ByteArray
        .toStr(infoById1.get().getInternalTransactions(2).getNote().toByteArray());
    Long vaule4 = infoById1.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule5 = infoById1.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Long vaule6 = infoById1.get().getInternalTransactions(2).getCallValueInfo(0).getCallValue();

    Assert.assertTrue(10 == vaule4);
    Assert.assertTrue(0 == vaule5);
    Assert.assertTrue(0 == vaule6);
    Assert.assertEquals("create", note4);
    Assert.assertEquals("call", note5);
    Assert.assertEquals("call", note6);

    for (int i = 0; i < transactionsCount1; i++) {
      Assert.assertTrue(infoById1.get().getInternalTransactions(i).getRejected());
    }
  }


  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed
        .freedResource(internalTxsAddress, testKeyForinternalTxsAddress, testNetAccountAddress,
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


  /**
   * constructor.
   */

  public void dupInternalTrsansactionHash(
      List<org.tron.protos.Protocol.InternalTransaction> internalTransactionList) {
    List<String> hashList = new ArrayList<>();
    internalTransactionList.forEach(
        internalTransaction -> hashList
            .add(Hex.toHexString(internalTransaction.getHash().toByteArray())));
    List<String> dupHash = hashList.stream()
        .collect(Collectors.toMap(e -> e, e -> 1, (a, b) -> a + b))
        .entrySet().stream().filter(entry -> entry.getValue() > 1).map(entry -> entry.getKey())
        .collect(Collectors.toList());
    Assert.assertEquals(dupHash.size(), 0);
  }
}
