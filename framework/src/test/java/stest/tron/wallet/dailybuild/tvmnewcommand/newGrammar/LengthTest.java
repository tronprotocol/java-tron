package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class LengthTest {
  private String testFoundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private byte[] testFoundationAddress = PublicMethed.getFinalAddress(testFoundationKey);

  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] testAddress001 = ecKey1.getAddress();
  String testKey001 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private byte[] contractAddress;

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
    PublicMethed.printAddress(testKey001);
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    PublicMethed
        .sendcoin(testAddress001, 10000_000_000L, testFoundationAddress, testFoundationKey,
            blockingStubFull);

    String filePath = "src/test/resources/soliditycode/arrayLength001.sol";
    String contractName = "arrayLength";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0, 100, null,
            testFoundationKey, testFoundationAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "push() increase Array length")
  public void arrayLengthTest001() {

    String methodStr = "arrayPush()";
    String argStr = "";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals(""
        + "0000000000000000000000000000000000000000000000000000000000000020"
        + "0000000000000000000000000000000000000000000000000000000000000002"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "push(value) increase Array length")
  public void arrayLengthTest002() {

    String methodStr = "arrayPushValue()";
    String argStr = "";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals(""
            + "0000000000000000000000000000000000000000000000000000000000000020"
            + "0000000000000000000000000000000000000000000000000000000000000002"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0100000000000000000000000000000000000000000000000000000000000000",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "pop() decrease Array length")
  public void arrayLengthTest003() {

    String methodStr = "arrayPop()";
    String argStr = "";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals(""
            + "0000000000000000000000000000000000000000000000000000000000000020"
            + "0000000000000000000000000000000000000000000000000000000000000000",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "push() return no value")
  public void arrayLengthTest004() {

    String methodStr = "arrayPushReturn()";
    String argStr = "";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals(""
            + "0000000000000000000000000000000000000000000000000000000000000000",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "push(value) return value")
  public void arrayLengthTest005() {

    String methodStr = "arrayPushValueReturn()";
    String argStr = "";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals("",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "pop() return no value")
  public void arrayLengthTest006() {

    String methodStr = "arrayPopReturn()";
    String argStr = "";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals("",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "bytes push() return value")
  public void arrayLengthTest007() {

    String methodStr = "bytesPush()";
    String argStr = "";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals(""
            + "0000000000000000000000000000000000000000000000000000000000000000",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "bytes push(value) return no value")
  public void arrayLengthTest008() {

    String methodStr = "bytesPushValue()";
    String argStr = "";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals("",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "bytes pop() return no value")
  public void arrayLengthTest009() {

    String methodStr = "bytesPop()";
    String argStr = "";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals("",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0).toByteArray()));
  }


  @Test(enabled = true, description = "array length change before v0.5.15")
  public void arrayLengthV0515() {
    String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_arrayLenth_0.5.15");
    String code = Configuration.getByPath("testng.conf")
        .getString("code.code_arrayLength_0.5.15");
    String contractName = "arrayLength";
    byte[] v0515Address = PublicMethed.deployContract(contractName,abi,code,"",maxFeeLimit,0,100,
        null, testKey001, testAddress001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String Txid = PublicMethed.triggerContract(v0515Address,"ChangeSize()","",false,0,maxFeeLimit,
        testAddress001,testKey001,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(Txid, blockingStubFull);

    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertEquals(""
        + "0000000000000000000000000000000000000000000000000000000000000020"
        + "0000000000000000000000000000000000000000000000000000000000000001"
        + "0100000000000000000000000000000000000000000000000000000000000000",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0).toByteArray()));

  }


}
