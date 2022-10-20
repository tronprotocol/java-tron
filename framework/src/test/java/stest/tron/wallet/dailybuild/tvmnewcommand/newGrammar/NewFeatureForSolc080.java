package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

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
import org.tron.protos.contract.SmartContractOuterClass;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;


@Slf4j
public class NewFeatureForSolc080 {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  byte[] mapKeyContract = null;
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

    String filePath = "src/test/resources/soliditycode/NewFeature080.sol";
    String contractName = "C";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    mapKeyContract = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        5000000L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethed.getContract(mapKeyContract,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
  }


  @Test(enabled = true, description = "math sub without unchecked, transaction revert")
  public void test01MathSubNoUncheck() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "subNoUncheck()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals("REVERT opcode executed",
        transactionExtention.getResult().getMessage().toStringUtf8());
    Assert.assertEquals("FAILED",
        transactionExtention.getTransaction().getRet(0).getRet().toString());

  }

  @Test(enabled = true, description = "math sub with uncheck,transaction success")
  public void test02SubWithUncheck() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "subWithUncheck()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(255, trueRes);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertTrue(transactionExtention.getEnergyUsed() > 300);

  }

  @Test(enabled = true, description = "math add overflow without unchecked, transaction revert")
  public void test03MathAddNoUncheck() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "addNoUncheck()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals("REVERT opcode executed",
        transactionExtention.getResult().getMessage().toStringUtf8());
    Assert.assertEquals("FAILED",
        transactionExtention.getTransaction().getRet(0).getRet().toString());

  }

  @Test(enabled = true, description = "math divide zero without unchecked, transaction revert")
  public void test04DivideZeroNoUncheck() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "divideZeroNoUncheck()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals("REVERT opcode executed",
        transactionExtention.getResult().getMessage().toStringUtf8());
    Assert.assertEquals("FAILED",
        transactionExtention.getTransaction().getRet(0).getRet().toString());

  }

  @Test(enabled = true, description = "assert fail without unchecked, transaction revert")
  public void test05AssertFailNoUncheck() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "assertFailNoUncheck()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals("REVERT opcode executed",
        transactionExtention.getResult().getMessage().toStringUtf8());
    Assert.assertEquals("FAILED",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
  }

  @Test(enabled = true, description = "array out of index without unchecked, transaction revert")
  public void test06AssertFailNoUncheck() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "arrayOutofIndex()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals("REVERT opcode executed",
        transactionExtention.getResult().getMessage().toStringUtf8());
    Assert.assertEquals("FAILED",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
  }

  @Test(enabled = true, description = "type convert")
  public void test07TypeConvert() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "typeConvert()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(65535, trueRes);
    Assert.assertEquals(true,
        transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
  }

  @Test(enabled = true, description = "power multi by default turn: right to left")
  public void test08PowerMultiRightToLeft() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "powerMultiRightToLeft()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(2, trueRes);
    Assert.assertEquals(true,
        transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
  }

  @Test(enabled = true, description = "power multi: left to right ")
  public void test09PowerMultiLeftToRight() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "powerMultiLeftToRight()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(64, trueRes);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
  }

  @Test(enabled = true, description = "power multi with 2 params ")
  public void test10PowerMultiWith2Params() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "powerMultiWith2Params()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(8, trueRes);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
  }

  @Test(enabled = true, description = "get block chain id ")
  public void test11GetBlockChainId() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "getBlockChainId()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    String chainId = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("chainId:  "  + chainId);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
  }

  @Test(enabled = true, description = "get normal account address hashcode ")
  public void test12GetAccountHashCode() {
    String argStr = "\"" + Base58.encode58Check(contractExcAddress) + "\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "getAddressCodehash(address)", argStr, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    logger.info(transactionExtention.toString());
    String trueRes = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("0000000:  " + trueRes);
  }

  @Test(enabled = true, description = "get contract address hashcode ")
  public void test13GetContractAddressHashCode() {
    String argStr = "\"" + Base58.encode58Check(mapKeyContract) + "\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "getAddressCodehash(address)", argStr, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    logger.info(transactionExtention.toString());
    String trueRes = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("0000000:  " + trueRes);
  }

  @Test(enabled = true, description = "transfer trx to tx.origin address with payable")
  public void test14TransferToTxoriginAddress() {
    Protocol.Account info = PublicMethed.queryAccount(mapKeyContract, blockingStubFull);
    Long beforeBalance = info.getBalance();
    logger.info("beforeBalance: " + beforeBalance);

    String methodStr = "transferToTxorigin(uint64)";
    String triggerTxid = PublicMethed.triggerContract(mapKeyContract, methodStr, "1000000", false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<Protocol.TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        transactionInfo.get().getReceipt().getResult());

    info = PublicMethed.queryAccount(mapKeyContract, blockingStubFull);
    Long afterBalance = info.getBalance();
    logger.info("afterBalance: " + afterBalance);
    Assert.assertTrue(beforeBalance == afterBalance + 1000000);
  }

  @Test(enabled = true, description = "transfer trx to literal address with payable")
  public void test15TransferToLiteralAddress() {
    Protocol.Account info = PublicMethed.queryAccount(mapKeyContract, blockingStubFull);
    Long beforeBalance = info.getBalance();
    logger.info("beforeBalance: " + beforeBalance);

    String methodStr = "transferToLiteralAddress(uint64)";
    String triggerTxid = PublicMethed.triggerContract(mapKeyContract, methodStr, "1000000", false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);

    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        transactionInfo.get().getReceipt().getResult());

    info = PublicMethed.queryAccount(mapKeyContract, blockingStubFull);
    Long afterBalance = info.getBalance();
    logger.info("afterBalance: " + afterBalance);
    Assert.assertTrue(beforeBalance == afterBalance + 1000000);
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

