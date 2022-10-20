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
import stest.tron.wallet.common.client.utils.PublicMethed;




@Slf4j
public class NewFeatureForSolc086 {

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

    String filePath = "src/test/resources/soliditycode/NewFeature086.sol";
    String contractName = "C";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    mapKeyContract = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        500000000L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethed.getContract(mapKeyContract,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
  }


  @Test(enabled = true, description = "catch assert fail")
  public void test01TrtCatchAssertFail() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "catchAssertFail()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(1, trueRes);

  }

  @Test(enabled = true, description = "catch under flow")
  public void test02CatchUnderFlow() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "catchUnderFlow()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(17, trueRes);

  }

  @Test(enabled = true, description = "catch divide zero")
  public void test03CatchDivideZero() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "catchDivideZero()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(18, trueRes);
  }

  @Test(enabled = true, description = "get address code length")
  public void test04GetAddressCodeLength() {
    String triggerTxid = PublicMethed.triggerContract(mapKeyContract, "getAddressCodeLength()",
        "#", false, 0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<Protocol.TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        transactionInfo.get().getReceipt().getResult());
    Assert.assertTrue(transactionInfo.get().getFee() < 40000);
  }

  @Test(enabled = true, description = "fix kecca256 bug: differt length return same code")
  public void test05Kecca256BugFix() {
    String args = "\"abcd123\"";
    String triggerTxid = PublicMethed.triggerContract(mapKeyContract, "keccak256Bug(string)",
        args, false, 0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<Protocol.TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        transactionInfo.get().getReceipt().getResult());
    Assert.assertEquals(0,
        ByteArray.toInt(transactionInfo.get().getContractResult(0).toByteArray()));
    logger.info(transactionInfo.toString());
  }

  @Test(enabled = true, description = "revert error type with params")
  public void test06RevertErrorType() {
    String args = "\"T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb\",1000000000";
    String triggerTxid = PublicMethed.triggerContract(mapKeyContract, "transfer(address,uint256)",
        args, false, 0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<Protocol.TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    logger.info(transactionInfo.toString());
    Assert.assertEquals(1, transactionInfo.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT,
        transactionInfo.get().getReceipt().getResult());
    Assert.assertEquals("cf479181",
        ByteArray.toHexString(transactionInfo.get()
            .getContractResult(0).substring(0, 4).toByteArray()));
    Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000000",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0)
            .substring(4, 36).toByteArray()));
    Assert.assertEquals("000000000000000000000000000000000000000000000000000000003b9aca00",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0)
            .substring(36, 68).toByteArray()));

  }

  @Test(enabled = true, description = "revert error type no params")
  public void test07RevertErrorType() {
    String triggerTxid = PublicMethed.triggerContract(mapKeyContract, "withdraw()", "#", false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<Protocol.TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertEquals(1, transactionInfo.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT,
        transactionInfo.get().getReceipt().getResult());
    Assert.assertEquals("82b42900",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0)
            .substring(0, 4).toByteArray()));
  }

  @Test(enabled = true, description = "test bytes concat")
  public void test08bytesConcat() {
    String args = "\"0x1234\",\"p2\",\"0x48e2f56f2c57e3532146eef2587a2a72\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "bytesConcat(bytes,string,bytes16)", args, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(36, trueRes);
  }

  @Test(enabled = true, description = "test emit event")
  public void test09EmitEvent() {
    String triggerTxid = PublicMethed.triggerContract(mapKeyContract, "testEmitEvent()", "#", false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<Protocol.TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    logger.info(transactionInfo.toString());
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        transactionInfo.get().getReceipt().getResult());
    Assert.assertEquals(6,
        ByteArray.toInt(transactionInfo.get().getLog(0).getData().toByteArray()));
  }


  @Test(enabled = true, description = "test bytes convert to byteN overflow")
  public void test10Bytes2ByteN() {
    String args = "\"0x12345678\"";
    String triggerTxid = PublicMethed.triggerContract(mapKeyContract, "bytes2BytesN(bytes)",
        args, false, 0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<Protocol.TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    logger.info(transactionInfo.toString());
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        transactionInfo.get().getReceipt().getResult());
    Assert.assertEquals("1234560000000000000000000000000000000000000000000000000000000000",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "test bytes convert to byteN underflow")
  public void test11Bytes2ByteN() {
    String args = "\"0x1234\"";
    String triggerTxid = PublicMethed.triggerContract(mapKeyContract, "bytes2BytesN(bytes)",
        args, false, 0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<Protocol.TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    logger.info(transactionInfo.toString());
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        transactionInfo.get().getReceipt().getResult());
    Assert.assertEquals("1234000000000000000000000000000000000000000000000000000000000000",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "get contract address by different function")
  public void test12GetConcatAddress() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "getContractAddress()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    String res1 = ByteArray.toHexString(transactionExtention.getConstantResult(0)
        .substring(0, 32).toByteArray());
    String res2 = ByteArray.toHexString(transactionExtention.getConstantResult(0)
        .substring(32, 64).toByteArray());
    Assert.assertEquals(res1, res2);
  }

  @Test(enabled = true, description = "test bytes concat with empty string")
  public void test13bytesConcatWithEmptyStr() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "bytesConcatWithEmptyStr()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
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

