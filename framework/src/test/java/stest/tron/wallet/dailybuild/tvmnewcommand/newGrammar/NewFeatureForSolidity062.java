package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import static org.tron.protos.Protocol.TransactionInfo.code.FAILED;
import static org.tron.protos.Protocol.TransactionInfo.code.SUCESS;

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
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
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
public class NewFeatureForSolidity062 {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  byte[] getSelectorContract = null;
  byte[] gasValueContract = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);

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

    Assert.assertTrue(PublicMethed
        .sendcoin(contractExcAddress, 300100_000_000L,
            testNetAccountAddress, testNetAccountKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String filePath = "src/test/resources/soliditycode/ExternalSelector.sol";
    String contractName = "TestGasValue";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    gasValueContract = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        10000000L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }


  @Test(enabled = true, description = "get selector from contract or interface's external function")
  public void test01GetFunctionSelector() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(gasValueContract,
        "getContractSelectorNoParam()", "#", true,
        0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Protocol.Transaction transaction = transactionExtention.getTransaction();
    String truerRes = ByteArray.toHexString(transactionExtention
        .getConstantResult(0).toByteArray());
    logger.info("truerRes: " + truerRes + "   message:" + transaction.getRet(0).getRet());
    logger.info("transactionExtention: " + transactionExtention);
    Assert.assertTrue(truerRes.startsWith("6c4959fa"));

    transactionExtention = PublicMethed.triggerConstantContractForExtention(gasValueContract,
        "getContractSelectorWithParam()", "#", true,
        0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    transaction = transactionExtention.getTransaction();
    truerRes = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("truerRes: " + truerRes + "   message:" + transaction.getRet(0).getRet());
    logger.info("transactionExtention: " + transactionExtention);
    Assert.assertTrue(truerRes.startsWith("fbb94ff8"));

    transactionExtention = PublicMethed.triggerConstantContractForExtention(gasValueContract,
        "getInterfaceSelectorNoParam()", "#", true,
        0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    transaction = transactionExtention.getTransaction();
    truerRes = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("truerRes: " + truerRes + "   message:" + transaction.getRet(0).getRet());
    logger.info("transactionExtention: " + transactionExtention);
    Assert.assertTrue(truerRes.startsWith("034899bc"));
  }

  @Test(enabled = true, description = "call external function like "
      + "c.f{gas: 0, value: 1}()")
  public void test02Call0GasAnd1Value() {

    String txid = PublicMethed.triggerContract(gasValueContract,
        "callWithGasAndValue(uint256,uint256)", "0,1", false,
        0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("txid: " + txid + "\n" + infoById.toString());
    Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000159",
        ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray()));
    byte[] internalReceiver = infoById.get().getInternalTransactions(0)
        .getTransferToAddress().toByteArray();

    long balanceReceiver = PublicMethed.queryAccount(internalReceiver, blockingStubFull)
        .getBalance();
    logger.info("transfer to address: " + Base58.encode58Check(internalReceiver)
        + "\n balance:" + balanceReceiver);
    Assert.assertEquals(1, balanceReceiver);
  }

  @Test(enabled = true, description = "call external function like "
      + "c.f{gas: 0, value: 0}()")
  public void test03Call0GasAnd0Value() {
    String txid = PublicMethed.triggerContract(gasValueContract,
        "callWithGasAndValue(uint256,uint256)", "0,0", false,
        0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("txid: " + txid + "\n" + infoById.toString());
    Assert.assertEquals(FAILED, infoById.get().getResult());
    Assert.assertEquals("REVERT opcode executed",
        infoById.get().getResMessage().toStringUtf8());

  }

  @Test(enabled = true, description = "inline assembly allow true and false")
  public void test04AssembleTrueFalse() {

    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(gasValueContract,
        "testAssemblyTrue()", "#", true,
        0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Protocol.Transaction transaction = transactionExtention.getTransaction();
    int truerRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("truerRes: " + truerRes + "   message:" + transaction.getRet(0).getRet());
    Assert.assertEquals(1, truerRes);

    transactionExtention = PublicMethed.triggerConstantContractForExtention(gasValueContract,
        "testAssemblyFalse()", "#", true,
        0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    transaction = transactionExtention.getTransaction();
    int falseRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("res: " + falseRes + "   message:" + transaction.getRet(0).getRet());
    Assert.assertEquals(0, falseRes);

  }

  @Test(enabled = true, description = "test new create2")
  public void test05NewCreate2() {

    String txid = PublicMethed.triggerContract(gasValueContract,
        "testCreate2()", "#", true,
        0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("txid: " + txid + "\n" + infoById.toString());
    byte[] internalReceiver = infoById.get()
        .getInternalTransactions(0).getTransferToAddress().toByteArray();

    long balanceReceiver = PublicMethed.queryAccount(internalReceiver, blockingStubFull)
        .getBalance();
    logger.info("transfer to address: " + Base58.encode58Check(internalReceiver)
        + "\n balance:" + balanceReceiver);
    Assert.assertEquals(1000000, balanceReceiver);

  }

  @Test(enabled = true, description = "test Interface Succeed")
  public void test06InterfaceSucceed() {

    String filePath = "src/test/resources/soliditycode/ExternalSelector.sol";
    String contractName = "implementContract";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    byte[] implementContract = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        10000000L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(implementContract,
        "getSelector()", "#", true,
        0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Protocol.Transaction transaction = transactionExtention.getTransaction();
    int truerRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("truerRes: " + truerRes + "   message:" + transaction.getRet(0).getRet());
    Assert.assertEquals(66, truerRes);
  }

  @Test(enabled = true, description = "call in contract external function like "
      + "c.f{gas: 0, value: 1}()")
  public void test07CallThis0GasAnd1Value() {

    String txid = PublicMethed.triggerContract(gasValueContract,
        "callThisNoGasAnd1Value()", "#", true,
        0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("txid: " + txid + "\n" + infoById.toString());
    Assert.assertEquals(FAILED, infoById.get().getResult());

  }

  @Test(enabled = true, description = "call external function like "
      + "c.f{gas: 440000, value: 0}()")
  public void test08CallWithGasAnd0Value() {
    String txid = PublicMethed.triggerContract(gasValueContract,
        "callWithGasAndValue(uint256,uint256)", "440000,0", false,
        0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("txid: " + txid + "\n" + infoById.toString());
    Assert.assertEquals(SUCESS, infoById.get().getResult());

  }

  @Test(enabled = true, description = "call external function like "
      + "c.f{gas: 1, value: 0}()")
  public void test09CallWith1GasAnd0Value() {
    String txid = PublicMethed.triggerContract(gasValueContract,
        "callWithGasAndValue(uint256,uint256)", "1,0", false,
        0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("txid: " + txid + "\n" + infoById.toString());
    Assert.assertEquals(FAILED, infoById.get().getResult());

  }

  @Test(enabled = true, description = "call external function like "
      + "c.f{gas: 0, value: > balance}()")
  public void test10CallWith0GasAndBigValue() {
    String txid = PublicMethed.triggerContract(gasValueContract,
        "callWithGasAndValue(uint256,uint256)", "0,9223372036854775800", false,
        0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("txid: " + txid + "\n" + infoById.toString());
    Assert.assertEquals(FAILED, infoById.get().getResult());
  }

  @Test(enabled = true, description = "call external function like "
      + "c.f{gas: 9223372036854775800, value: 0}()")
  public void test11CallWithBigGasAnd0Value() {
    String txid = PublicMethed.triggerContract(gasValueContract,
        "callWithGasAndValue(uint256,uint256)", "9223372036854775800,0", false,
        0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("txid: " + txid + "\n" + infoById.toString());
    Assert.assertEquals(SUCESS, infoById.get().getResult());

  }


  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed.freedResource(contractExcAddress, contractExcKey,
        testNetAccountAddress, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}

