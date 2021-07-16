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
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class tryCatchTest001 {
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
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String filePath = "src/test/resources/soliditycode/tryCatch001.sol";
    String contractName = "tryTest";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0, 100, null,
            testFoundationKey, testFoundationAddress, blockingStubFull);

    contractName = "errorContract";
    retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    errorContractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0, 100, null,
            testFoundationKey, testFoundationAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

  }


  @Test(enabled = true,  description = "try catch  revert no msg")
  public void tryCatchTest001() {
    String methodStr = "getErrorSwitch(address,uint256)";
    String argStr = "\"" + Base58.encode58Check(errorContractAddress) + "\",0";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals("NoErrorMsg", PublicMethed
        .getContractStringMsg(transactionInfo.get().getContractResult(0).toByteArray()));


  }

  @Test(enabled = true, description = "try catch  revert msg")
  public void tryCatchTest002() {
    String methodStr = "getErrorSwitch(address,uint256)";
    String argStr = "\"" + Base58.encode58Check(errorContractAddress) + "\",1";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals("Revert Msg.", PublicMethed
        .getContractStringMsg(transactionInfo.get().getContractResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "try catch  Require no msg")
  public void tryCatchTest003() {
    String methodStr = "getErrorSwitch(address,uint256)";
    String argStr = "\"" + Base58.encode58Check(errorContractAddress) + "\",2";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals("NoErrorMsg", PublicMethed
        .getContractStringMsg(transactionInfo.get().getContractResult(0).toByteArray()));

  }

  @Test(enabled = true, description = "try catch  Require msg")
  public void tryCatchTest004() {
    String methodStr = "getErrorSwitch(address,uint256)";
    String argStr = "\"" + Base58.encode58Check(errorContractAddress) + "\",3";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals("Require Msg.", PublicMethed
        .getContractStringMsg(transactionInfo.get().getContractResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "try catch  assert")
  public void tryCatchTest005() {
    String methodStr = "getErrorSwitch(address,uint256)";
    String argStr = "\"" + Base58.encode58Check(errorContractAddress) + "\",4";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertEquals(contractResult.SUCCESS,
        transactionInfo.get().getReceipt().getResult());

  }

  @Test(enabled = true, description = "try catch  transfer fail")
  public void tryCatchTest006() {
    String methodStr = "getErrorSwitch(address,uint256)";
    String argStr = "\"" + Base58.encode58Check(errorContractAddress) + "\",5";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals("NoErrorMsg", PublicMethed
        .getContractStringMsg(transactionInfo.get().getContractResult(0).toByteArray()));

  }

  @Test(enabled = true, description = "try catch  Send_Error")
  public void tryCatchTest007() {
    String methodStr = "getErrorSwitch(address,uint256)";
    String argStr = "\"" + Base58.encode58Check(errorContractAddress) + "\",6";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals("success", PublicMethed
        .getContractStringMsg(transactionInfo.get().getContractResult(0).toByteArray()));

  }

  @Test(enabled = true, description = "try catch  Math_Error")
  public void tryCatchTest008() {
    String methodStr = "getErrorSwitch(address,uint256)";
    String argStr = "\"" + Base58.encode58Check(errorContractAddress) + "\",7";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertEquals(contractResult.SUCCESS,
        transactionInfo.get().getReceipt().getResult());

  }

  @Test(enabled = true, description = "try catch  ArrayOverFlow_Error")
  public void tryCatchTest009() {
    String methodStr = "getErrorSwitch(address,uint256)";
    String argStr = "\"" + Base58.encode58Check(errorContractAddress) + "\",8";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertEquals(contractResult.SUCCESS,
        transactionInfo.get().getReceipt().getResult());

  }

}

