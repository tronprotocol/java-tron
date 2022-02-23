package stest.tron.wallet.dailybuild.tvmnewcommand.tryCatch;

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
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class tryCatchTest002 {
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
  private byte[] errorContractAddress;

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * miraculous.wong.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(testKey001);
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    PublicMethed
        .sendcoin(testAddress001, 10000_000_000L, testFoundationAddress, testFoundationKey,
            blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/tryCatch001.sol";
    String contractName = "tryTest";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0, 100, null,
            testFoundationKey, testFoundationAddress, blockingStubFull);


  }


  @Test(enabled = true,  description = "try catch [new] revert no msg")
  public void tryCatchTest001() {
    String methodStr = "catchNewErrorSwitch(uint256)";
    String argStr = "0";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals(
        "0000000000000000000000000000000000000000000000000000000000000000",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "try catch [new] revert msg")
  public void tryCatchTest002() {
    String methodStr = "catchNewErrorSwitch(uint256)";
    String argStr = "1";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals(
        "0000000000000000000000000000000000000000000000000000000000000000",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "try catch [new] Require no msg")
  public void tryCatchTest003() {
    String methodStr = "catchNewErrorSwitch(uint256)";
    String argStr = "2";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals(
        "0000000000000000000000000000000000000000000000000000000000000000",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0).toByteArray()));

  }

  @Test(enabled = true, description = "try catch [new] Require msg")
  public void tryCatchTest004() {
    String methodStr = "catchNewErrorSwitch(uint256)";
    String argStr = "3";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals(
        "0000000000000000000000000000000000000000000000000000000000000000",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "try catch [new] assert")
  public void tryCatchTest005() {
    String methodStr = "catchNewErrorSwitch(uint256)";
    String argStr = "4";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertEquals(contractResult.SUCCESS,
        transactionInfo.get().getReceipt().getResult());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals(
        "0000000000000000000000000000000000000000000000000000000000000000",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "try catch [new] transfer fail")
  public void tryCatchTest006() {
    String methodStr = "catchNewErrorSwitch(uint256)";
    String argStr = "5";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals(
        "0000000000000000000000000000000000000000000000000000000000000000",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0).toByteArray()));

  }

  @Test(enabled = true, description = "try catch [new] Send_Error")
  public void tryCatchTest007() {
    String methodStr = "catchNewErrorSwitch(uint256)";
    String argStr = "6";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertNotEquals(
        "0000000000000000000000000000000000000000000000000000000000000000",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0).toByteArray()));

  }

  @Test(enabled = true, description = "try catch [new] Math_Error")
  public void tryCatchTest008() {
    String methodStr = "catchNewErrorSwitch(uint256)";
    String argStr = "7";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals(contractResult.SUCCESS,
        transactionInfo.get().getReceipt().getResult());

  }

  @Test(enabled = true, description = "try catch [new] ArrayOverFlow_Error")
  public void tryCatchTest009() {
    String methodStr = "catchNewErrorSwitch(uint256)";
    String argStr = "8";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals(contractResult.SUCCESS,
        transactionInfo.get().getReceipt().getResult());

  }

}
