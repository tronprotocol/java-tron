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

public class ContractInternalTransaction003 {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
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


  @Test(enabled = true, description = "Three-level nesting.Type is Create call->call->create")
  public void testInternalTransaction013() {
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/"
        + "contractInternalTransaction003testInternalTransaction013.sol";
    String contractName = "A";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String contractName1 = "D";
    HashMap retMap1 = PublicMethed.getBycodeAbi(filePath, contractName1);
    String code1 = retMap1.get("byteCode").toString();
    String abi1 = retMap1.get("abI").toString();
    byte[] contractAddress1 = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String initParmes = "\"" + Base58.encode58Check(contractAddress1) + "\"";
    String txid = "";
    txid = PublicMethed.triggerContract(contractAddress,
        "test1(address)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    int transactionsCount = infoById.get().getInternalTransactionsCount();
    Assert.assertEquals(4, transactionsCount);
    for (int i = 0; i < transactionsCount; i++) {
      Assert.assertFalse(infoById.get().getInternalTransactions(i).getRejected());
    }
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());
    String note = ByteArray
        .toStr(infoById.get().getInternalTransactions(0).getNote().toByteArray());
    String note1 = ByteArray
        .toStr(infoById.get().getInternalTransactions(1).getNote().toByteArray());
    String note2 = ByteArray
        .toStr(infoById.get().getInternalTransactions(2).getNote().toByteArray());
    String note3 = ByteArray
        .toStr(infoById.get().getInternalTransactions(3).getNote().toByteArray());
    Assert.assertEquals("create", note);
    Assert.assertEquals("call", note1);
    Assert.assertEquals("call", note2);
    Assert.assertEquals("create", note3);
    Long vaule1 = infoById.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule2 = infoById.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Long vaule3 = infoById.get().getInternalTransactions(2).getCallValueInfo(0).getCallValue();
    Long vaule4 = infoById.get().getInternalTransactions(3).getCallValueInfo(0).getCallValue();
    Assert.assertTrue(10 == vaule1);
    Assert.assertTrue(0 == vaule2);
    Assert.assertTrue(2 == vaule3);
    Assert.assertTrue(5 == vaule4);


  }


  @Test(enabled = true, description = "Test delegatecall and callcode.")
  public void testInternalTransaction014() {
    Assert.assertTrue(PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/"
        + "contractInternalTransaction003testInternalTransaction014.sol";
    String contractName = "callerContract";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String contractName1 = "calledContract";
    HashMap retMap1 = PublicMethed.getBycodeAbi(filePath, contractName1);
    String code1 = retMap1.get("byteCode").toString();
    String abi1 = retMap1.get("abI").toString();
    byte[] contractAddress1 = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String contractName2 = "c";
    HashMap retMap2 = PublicMethed.getBycodeAbi(filePath, contractName2);
    String code2 = retMap2.get("byteCode").toString();
    String abi2 = retMap2.get("abI").toString();
    byte[] contractAddress2 = PublicMethed
        .deployContract(contractName2, abi2, code2, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String initParmes = "\"" + Base58.encode58Check(contractAddress1)
        + "\",\"" + Base58.encode58Check(contractAddress2) + "\"";
    String txid = "";
    txid = PublicMethed.triggerContract(contractAddress,
        "sendToB(address,address)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    int transactionsCount = infoById.get().getInternalTransactionsCount();
    Assert.assertEquals(2, transactionsCount);
    for (int i = 0; i < transactionsCount; i++) {
      Assert.assertFalse(infoById.get().getInternalTransactions(i).getRejected());
      Assert.assertEquals("call", ByteArray
          .toStr(infoById.get().getInternalTransactions(i).getNote().toByteArray()));
    }
    Assert.assertEquals(ByteArray
            .toHexString(infoById.get().getInternalTransactions(0).getCallerAddress()
                .toByteArray()),
        ByteArray.toHexString(
            infoById.get().getInternalTransactions(0).getTransferToAddress().toByteArray()));

    Assert.assertEquals(ByteArray
            .toHexString(contractAddress2),
        ByteArray.toHexString(
            infoById.get().getInternalTransactions(1).getTransferToAddress().toByteArray()));
    String txid2 = "";
    txid2 = PublicMethed.triggerContract(contractAddress,
        "sendToB2(address,address)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById2 = null;
    infoById2 = PublicMethed.getTransactionInfoById(txid2, blockingStubFull);
    Assert.assertTrue(infoById2.get().getResultValue() == 0);
    int transactionsCount2 = infoById2.get().getInternalTransactionsCount();
    Assert.assertEquals(2, transactionsCount2);
    for (int i = 0; i < transactionsCount2; i++) {
      Assert.assertFalse(infoById2.get().getInternalTransactions(i).getRejected());
      Assert.assertEquals("call", ByteArray
          .toStr(infoById2.get().getInternalTransactions(i).getNote().toByteArray()));
    }
    Assert.assertEquals(ByteArray
            .toHexString(contractAddress),
        ByteArray.toHexString(
            infoById2.get().getInternalTransactions(0).getCallerAddress().toByteArray()));
    Assert.assertEquals(ByteArray
            .toHexString(contractAddress1),
        ByteArray.toHexString(
            infoById2.get().getInternalTransactions(0).getTransferToAddress().toByteArray()));
    Assert.assertEquals(ByteArray
            .toHexString(contractAddress1),
        ByteArray.toHexString(
            infoById2.get().getInternalTransactions(1).getCallerAddress().toByteArray()));
    Assert.assertEquals(ByteArray
            .toHexString(contractAddress2),
        ByteArray.toHexString(
            infoById2.get().getInternalTransactions(1).getTransferToAddress().toByteArray()));

    String txid3 = "";
    txid3 = PublicMethed.triggerContract(contractAddress,
        "sendToB3(address,address)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById3 = null;
    infoById3 = PublicMethed.getTransactionInfoById(txid3, blockingStubFull);
    Assert.assertTrue(infoById3.get().getResultValue() == 0);
    int transactionsCount3 = infoById3.get().getInternalTransactionsCount();
    Assert.assertEquals(2, transactionsCount3);
    for (int i = 0; i < transactionsCount3; i++) {
      Assert.assertFalse(infoById3.get().getInternalTransactions(i).getRejected());
      Assert.assertEquals("call", ByteArray
          .toStr(infoById3.get().getInternalTransactions(i).getNote().toByteArray()));
    }
    Assert.assertEquals(ByteArray
            .toHexString(infoById3.get().getInternalTransactions(0).getCallerAddress()
                .toByteArray()),
        ByteArray.toHexString(
            infoById3.get().getInternalTransactions(0).getTransferToAddress().toByteArray()));
    Assert.assertEquals(ByteArray
            .toHexString(contractAddress2),
        ByteArray.toHexString(
            infoById3.get().getInternalTransactions(1).getTransferToAddress().toByteArray()));
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());
    dupInternalTrsansactionHash(infoById2.get().getInternalTransactionsList());
    dupInternalTrsansactionHash(infoById3.get().getInternalTransactionsList());

  }

  @Test(enabled = true, description = "Three-level nesting.Type "
      + "is create call->call->create call->suicide")
  public void testInternalTransaction015() {
    Assert.assertTrue(PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/"
        + "contractInternalTransaction003testInternalTransaction015.sol";
    String contractName = "A";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String contractName1 = "D";
    HashMap retMap1 = PublicMethed.getBycodeAbi(filePath, contractName1);
    String code1 = retMap1.get("byteCode").toString();
    String abi1 = retMap1.get("abI").toString();
    byte[] contractAddress1 = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String contractName2 = "E";
    HashMap retMap2 = PublicMethed.getBycodeAbi(filePath, contractName2);
    String code2 = retMap2.get("byteCode").toString();
    String abi2 = retMap2.get("abI").toString();
    byte[] contractAddress2 = PublicMethed
        .deployContract(contractName2, abi2, code2, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String initParmes = "\"" + Base58.encode58Check(contractAddress1)
        + "\",\"" + Base58.encode58Check(contractAddress2) + "\"";
    String txid = "";
    txid = PublicMethed.triggerContract(contractAddress,
        "test1(address,address)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    int transactionsCount = infoById.get().getInternalTransactionsCount();
    Assert.assertEquals(6, transactionsCount);
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());


  }


  @Test(enabled = false, description = "After create 80 times,then suicide")
  public void testInternalTransaction016() {
    Assert.assertTrue(PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/"
        + "contractInternalTransaction003testInternalTransaction016.sol";
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
        "transfer()", "#", false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    int transactionsCount = infoById.get().getInternalTransactionsCount();
    Assert.assertEquals(69, transactionsCount);
    for (int i = 0; i < transactionsCount; i++) {
      Assert.assertFalse(infoById.get().getInternalTransactions(i).getRejected());
    }
    Assert.assertEquals("suicide", ByteArray
        .toStr(infoById.get().getInternalTransactions(68).getNote().toByteArray()));
    Assert.assertEquals("call", ByteArray
        .toStr(infoById.get().getInternalTransactions(67).getNote().toByteArray()));
    Assert.assertEquals(0,
        infoById.get().getInternalTransactions(67).getCallValueInfo(0).getCallValue());
    Assert.assertEquals(1,
        infoById.get().getInternalTransactions(68).getCallValueInfo(0).getCallValue());
    for (int i = 0; i < transactionsCount - 2; i++) {
      Assert.assertEquals("create", ByteArray
          .toStr(infoById.get().getInternalTransactions(i).getNote().toByteArray()));
      Assert.assertEquals(1,
          infoById.get().getInternalTransactions(i).getCallValueInfo(0).getCallValue());
    }
    String txid1 = "";
    txid1 = PublicMethed.triggerContract(contractAddress,
        "transfer2()", "#", false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    int transactionsCount1 = infoById1.get().getInternalTransactionsCount();

    Assert.assertEquals(68, transactionsCount1);
    for (int i = 0; i < transactionsCount1; i++) {
      Assert.assertTrue(infoById1.get().getInternalTransactions(i).getRejected());
      Assert.assertEquals("create", ByteArray
          .toStr(infoById1.get().getInternalTransactions(i).getNote().toByteArray()));

    }
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());
    dupInternalTrsansactionHash(infoById1.get().getInternalTransactionsList());

  }

  @Test(enabled = false, description = "After create 88 times,then suicide")
  public void testInternalTransaction017() {
    Assert.assertTrue(PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/"
        + "contractInternalTransaction003testInternalTransaction017.sol";
    String contractName = "A";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String initParmes = "\"" + Base58.encode58Check(contractAddress) + "\"";

    String txid = "";
    txid = PublicMethed.triggerContract(contractAddress,
        "transfer(address)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    int transactionsCount = infoById.get().getInternalTransactionsCount();
    Assert.assertEquals(77, transactionsCount);
    for (int i = 0; i < transactionsCount; i++) {
      Assert.assertFalse(infoById.get().getInternalTransactions(i).getRejected());
    }
    Assert.assertEquals("suicide", ByteArray
        .toStr(infoById.get().getInternalTransactions(76).getNote().toByteArray()));
    Assert.assertEquals(1000000 - 76,
        infoById.get().getInternalTransactions(76).getCallValueInfo(0).getCallValue());
    for (int i = 0; i < transactionsCount - 1; i++) {
      Assert.assertEquals("create", ByteArray
          .toStr(infoById.get().getInternalTransactions(i).getNote().toByteArray()));
      Assert.assertEquals(1,
          infoById.get().getInternalTransactions(i).getCallValueInfo(0).getCallValue());
    }
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());
  }

  @Test(enabled = true, description = "Test maxfeelimit can trigger call create call max time")
  public void testInternalTransaction018() {
    Assert.assertTrue(PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String filePath = "src/test/resources/soliditycode/"
        + "contractInternalTransaction003testInternalTransaction018.sol";
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
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String contractName2 = "E";
    HashMap retMap2 = PublicMethed.getBycodeAbi(filePath, contractName2);

    String code2 = retMap2.get("byteCode").toString();
    String abi2 = retMap2.get("abI").toString();
    byte[] contractAddress2 = PublicMethed
        .deployContract(contractName2, abi2, code2, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed
        .sendcoin(internalTxsAddress, 1000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String initParmes = "\"" + Base58.encode58Check(contractAddress1)
        + "\",\"" + Base58.encode58Check(contractAddress2) + "\"";
    String txid = "";
    txid = PublicMethed.triggerContract(contractAddress,
        "test1(address,address)", initParmes, false,
        100000, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("InfoById:" + infoById);

    // retry 1 times
    txid = PublicMethed.triggerContract(contractAddress,
        "test1(address,address)", initParmes, false,
        100000, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("InfoById-1" + ": " + infoById);

    Assert.assertEquals(0, infoById.get().getResultValue());
    int transactionsCount = infoById.get().getInternalTransactionsCount();
    Assert.assertEquals(184, transactionsCount);
    for (int i = 0; i < transactionsCount; i++) {
      Assert.assertFalse(infoById.get().getInternalTransactions(i).getRejected());
    }
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());
    String note = ByteArray
        .toStr(infoById.get().getInternalTransactions(0).getNote().toByteArray());
    String note1 = ByteArray
        .toStr(infoById.get().getInternalTransactions(1).getNote().toByteArray());
    String note2 = ByteArray
        .toStr(infoById.get().getInternalTransactions(2).getNote().toByteArray());
    String note3 = ByteArray
        .toStr(infoById.get().getInternalTransactions(3).getNote().toByteArray());
    Long vaule1 = infoById.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule2 = infoById.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Long vaule3 = infoById.get().getInternalTransactions(2).getCallValueInfo(0).getCallValue();
    Long vaule4 = infoById.get().getInternalTransactions(3).getCallValueInfo(0).getCallValue();

    Assert.assertEquals("call", note);
    Assert.assertEquals("create", note1);
    Assert.assertEquals("call", note2);
    Assert.assertEquals("call", note3);
    Assert.assertTrue(1 == vaule1);
    Assert.assertTrue(100 == vaule2);
    Assert.assertTrue(0 == vaule3);
    Assert.assertTrue(1 == vaule4);
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
