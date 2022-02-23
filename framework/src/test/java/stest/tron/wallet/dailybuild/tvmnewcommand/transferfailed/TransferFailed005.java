package stest.tron.wallet.dailybuild.tvmnewcommand.transferfailed;

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
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.GrpcAPI.Return.response_code;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class TransferFailed005 {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  private final Long maxFeeLimit = Configuration.getByPath("testng.cong")
      .getLong("defaultParameter.maxFeeLimit");
  byte[] contractAddress = null;
  byte[] contractAddress1 = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] accountExcAddress = ecKey1.getAddress();
  String accountExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ManagedChannel channelSolidity = null;
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);

  

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(accountExcKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1).usePlaintext(true).build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

    {
      Assert.assertTrue(PublicMethed
          .sendcoin(accountExcAddress, 10000_000_000L, testNetAccountAddress, testNetAccountKey,
              blockingStubFull));
      PublicMethed.waitProduceNextBlock(blockingStubFull);

      String filePath = "src/test/resources/soliditycode/TransferFailed005.sol";
      String contractName = "EnergyOfTransferFailedTest";
      HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
      String code = retMap.get("byteCode").toString();
      String abi = retMap.get("abI").toString();

      contractAddress = PublicMethed
          .deployContract(contractName, abi, code, "", maxFeeLimit, 100L, 100L, null, accountExcKey,
              accountExcAddress, blockingStubFull);

      filePath = "src/test/resources/soliditycode/TransferFailed005.sol";
      contractName = "Caller";
      retMap = PublicMethed.getBycodeAbi(filePath, contractName);
      code = retMap.get("byteCode").toString();
      abi = retMap.get("abI").toString();

      contractAddress1 = PublicMethed
          .deployContract(contractName, abi, code, "", maxFeeLimit, 0L, 100L, null, accountExcKey,
              accountExcAddress, blockingStubFull);
    }
  }

  @Test(enabled = false, description = "Deploy contract for trigger")
  public void deployContract() {
    Assert.assertTrue(PublicMethed
        .sendcoin(accountExcAddress, 10000_000_000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String filePath = "src/test/resources/soliditycode/TransferFailed005.sol";
    String contractName = "EnergyOfTransferFailedTest";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    contractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0L, 100L, null, accountExcKey,
            accountExcAddress, blockingStubFull);
    String txid1 = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit, 0L, 100L,
            null, accountExcKey, accountExcAddress, blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid1, blockingStubFull);
    contractAddress = infoById.get().getContractAddress().toByteArray();
    Assert.assertEquals(0, infoById.get().getResultValue());

    filePath = "src/test/resources/soliditycode/TransferFailed005.sol";
    contractName = "Caller";
    retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();

    contractAddress1 = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0L, 100L, null, accountExcKey,
            accountExcAddress, blockingStubFull);
    txid1 = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit, 0L, 100L,
            null, accountExcKey, accountExcAddress, blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    contractAddress1 = infoById.get().getContractAddress().toByteArray();
    logger.info("caller address : " + Base58.encode58Check(contractAddress1));
    Assert.assertEquals(0, infoById.get().getResultValue());
  }

  @Test(enabled = true, description = "TransferFailed for function call_value ")
  public void triggerContract01() {
    Account info = null;

    AccountResourceMessage resourceInfo = PublicMethed
        .getAccountResource(accountExcAddress, blockingStubFull);
    info = PublicMethed.queryAccount(accountExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    //Assert.assertTrue(PublicMethed
    //    .sendcoin(contractAddress, 1000100L, accountExcAddress, accountExcKey, blockingStubFull));
    //Assert.assertTrue(PublicMethed
    //    .sendcoin(contractAddress1, 1, accountExcAddress, accountExcKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    logger.info("contractAddress balance before: " + PublicMethed
        .queryAccount(contractAddress, blockingStubFull).getBalance());
    logger.info("callerAddress balance before: " + PublicMethed
        .queryAccount(contractAddress1, blockingStubFull).getBalance());
    long paramValue = 1;

    // transfer trx to self`s account
    String param = "\"" + paramValue + "\",\"" + Base58.encode58Check(contractAddress) + "\"";
    String triggerTxid = PublicMethed
        .triggerContract(contractAddress, "testCallTrxInsufficientBalance(uint256,address)", param,
            false, 0L, maxFeeLimit, accountExcAddress, accountExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);

    Assert.assertEquals(infoById.get().getResultValue(), 1);
    Assert.assertEquals("FAILED", infoById.get().getResult().toString());
    Assert.assertEquals("TRANSFER_FAILED", infoById.get().getReceipt().getResult().toString());
    Assert.assertEquals("transfer trx failed: Cannot transfer TRX to yourself.",
        infoById.get().getResMessage().toStringUtf8());
    Assert.assertEquals(100L,
        PublicMethed.queryAccount(contractAddress, blockingStubFull).getBalance());
    Assert.assertEquals(0L,
        PublicMethed.queryAccount(contractAddress1, blockingStubFull).getBalance());
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() < 10000000);

    // transfer trx to unactivate account
    ECKey ecKey2 = new ECKey(Utils.getRandom());
    byte[] accountExcAddress2 = ecKey2.getAddress();
    param = "\"" + paramValue + "\",\"" + Base58.encode58Check(accountExcAddress2) + "\"";
    triggerTxid = PublicMethed
        .triggerContract(contractAddress, "testCallTrxInsufficientBalance(uint256,address)", param,
            false, 0L, maxFeeLimit, accountExcAddress, accountExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(triggerTxid, blockingStubFull);

    Assert.assertEquals(infoById.get().getResultValue(), 0);
    Assert.assertEquals("SUCESS", infoById.get().getResult().toString());

    Assert.assertEquals(99L,
        PublicMethed.queryAccount(contractAddress, blockingStubFull).getBalance());
    Assert.assertEquals(0L,
        PublicMethed.queryAccount(contractAddress1, blockingStubFull).getBalance());
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() < 10000000);

    // transfer trx to caller, value enough , function success contractResult(call_value) successed
    param = "\"" + paramValue + "\",\"" + Base58.encode58Check(contractAddress1) + "\"";
    triggerTxid = PublicMethed
        .triggerContract(contractAddress, "testCallTrxInsufficientBalance(uint256,address)", param,
            false, 0L, maxFeeLimit, accountExcAddress, accountExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethed.getTransactionInfoById(triggerTxid, blockingStubFull);
    logger.info(infoById.get().getReceipt().getResult() + "");

    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);

    int contractResult = ByteArray.toInt(infoById.get().getContractResult(0).toByteArray());
    Assert.assertEquals(1, contractResult);

    Assert.assertEquals(infoById.get().getResultValue(), 0);
    Assert.assertEquals(infoById.get().getResult().toString(), "SUCESS");
    Assert.assertEquals(98L,
        PublicMethed.queryAccount(contractAddress, blockingStubFull).getBalance());
    Assert.assertEquals(1L,
        PublicMethed.queryAccount(contractAddress1, blockingStubFull).getBalance());
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() < 10000000);

    // transfer trx to caller, value not enough, function success
    // but contractResult(call_value) failed
    param = "\"" + 100 + "\",\"" + Base58.encode58Check(contractAddress1) + "\"";
    triggerTxid = PublicMethed
        .triggerContract(contractAddress, "testCallTrxInsufficientBalance(uint256,address)", param,
            false, 0L, maxFeeLimit, accountExcAddress, accountExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethed.getTransactionInfoById(triggerTxid, blockingStubFull);
    fee = infoById.get().getFee();
    netUsed = infoById.get().getReceipt().getNetUsage();
    energyUsed = infoById.get().getReceipt().getEnergyUsage();
    netFee = infoById.get().getReceipt().getNetFee();
    energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);

    //contractResult`s first boolean value
    contractResult = ByteArray.toInt(infoById.get().getContractResult(0).toByteArray());
    Assert.assertEquals(0, contractResult);
    Assert.assertEquals(infoById.get().getResultValue(), 0);
    Assert.assertEquals(infoById.get().getResult().toString(), "SUCESS");
    Assert.assertEquals(98L,
        PublicMethed.queryAccount(contractAddress, blockingStubFull).getBalance());
    Assert.assertEquals(1L,
        PublicMethed.queryAccount(contractAddress1, blockingStubFull).getBalance());
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() < 10000000);


  }

  @Test(enabled = true, description = "TransferFailed for create")
  public void triggerContract02() {
    final Long contractBalance = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getBalance();
    Account info = null;

    AccountResourceMessage resourceInfo = PublicMethed
        .getAccountResource(accountExcAddress, blockingStubFull);
    info = PublicMethed.queryAccount(accountExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    //Assert.assertTrue(PublicMethed
    //    .sendcoin(contractAddress, 1000100L, accountExcAddress, accountExcKey, blockingStubFull));
    //Assert.assertTrue(PublicMethed
    //    .sendcoin(contractAddress1, 1, accountExcAddress, accountExcKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    logger.info("contractAddress balance before: " + PublicMethed
        .queryAccount(contractAddress, blockingStubFull).getBalance());
    logger.info("callerAddress balance before: " + PublicMethed
        .queryAccount(contractAddress1, blockingStubFull).getBalance());
    long paramValue = 1;
    String param = "\"" + paramValue + "\"";

    String triggerTxid = PublicMethed
        .triggerContract(contractAddress, "testCreateTrxInsufficientBalance(uint256)", param, false,
            0L, maxFeeLimit, accountExcAddress, accountExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    logger.info(infoById.get().getReceipt().getResult() + "");

    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);

    logger.info("contractAddress balance before: " + PublicMethed
        .queryAccount(contractAddress, blockingStubFull).getBalance());
    logger.info("callerAddress balance before: " + PublicMethed
        .queryAccount(contractAddress1, blockingStubFull).getBalance());
    Assert.assertEquals(infoById.get().getResultValue(), 0);
    Assert.assertFalse(infoById.get().getInternalTransactions(0).getRejected());
    Assert.assertEquals(contractBalance - 1,
        PublicMethed.queryAccount(contractAddress, blockingStubFull).getBalance());
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() < 10000000);

    param = "\"" + (contractBalance + 1) + "\"";
    triggerTxid = PublicMethed
        .triggerContract(contractAddress, "testCreateTrxInsufficientBalance(uint256)", param, false,
            0L, maxFeeLimit, accountExcAddress, accountExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethed.getTransactionInfoById(triggerTxid, blockingStubFull);
    fee = infoById.get().getFee();
    netUsed = infoById.get().getReceipt().getNetUsage();
    energyUsed = infoById.get().getReceipt().getEnergyUsage();
    netFee = infoById.get().getReceipt().getNetFee();
    energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);

    logger.info("contractAddress balance before: " + PublicMethed
        .queryAccount(contractAddress, blockingStubFull).getBalance());
    logger.info("callerAddress balance before: " + PublicMethed
        .queryAccount(contractAddress1, blockingStubFull).getBalance());

    Assert.assertEquals(infoById.get().getResultValue(), 1);
    Assert.assertEquals(infoById.get().getResMessage().toStringUtf8(), "REVERT opcode executed");
    Assert.assertEquals(contractBalance - 1,
        PublicMethed.queryAccount(contractAddress, blockingStubFull).getBalance());
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() < 10000000);


  }

  @Test(enabled = true, description = "TransferFailed for create2")
  public void triggerContract03() {
    final Long contractBalance = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getBalance();

    Account info;

    AccountResourceMessage resourceInfo = PublicMethed
        .getAccountResource(accountExcAddress, blockingStubFull);
    info = PublicMethed.queryAccount(accountExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    //Assert.assertTrue(PublicMethed
    //    .sendcoin(contractAddress, 15L, accountExcAddress, accountExcKey, blockingStubFull));
    logger.info("contractAddress balance before: " + PublicMethed
        .queryAccount(contractAddress, blockingStubFull).getBalance());

    String filePath = "./src/test/resources/soliditycode/TransferFailed007.sol";
    String contractName = "Caller";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String testContractCode = retMap.get("byteCode").toString();
    Long salt = 1L;

    String param = "\"" + testContractCode + "\"," + salt;

    String triggerTxid = PublicMethed
        .triggerContract(contractAddress, "deploy(bytes,uint256)", param, false, 0L, maxFeeLimit,
            accountExcAddress, accountExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);

    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);

    Long afterBalance = 0L;
    afterBalance = PublicMethed.queryAccount(contractAddress, blockingStubFull).getBalance();
    logger.info("contractAddress balance after : " + PublicMethed
        .queryAccount(contractAddress, blockingStubFull).getBalance());
    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals("SUCESS", infoById.get().getResult().toString());
    Assert.assertEquals(contractBalance - 10L, afterBalance.longValue());
    Assert.assertFalse(infoById.get().getInternalTransactions(0).getRejected());
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() < 10000000);

    triggerTxid = PublicMethed
        .triggerContract(contractAddress, "deploy2(bytes,uint256)", param, false, 0L, maxFeeLimit,
            accountExcAddress, accountExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(triggerTxid, blockingStubFull);

    fee = infoById.get().getFee();
    netUsed = infoById.get().getReceipt().getNetUsage();
    energyUsed = infoById.get().getReceipt().getEnergyUsage();
    netFee = infoById.get().getReceipt().getNetFee();
    energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);

    afterBalance = PublicMethed.queryAccount(contractAddress, blockingStubFull).getBalance();
    logger.info("contractAddress balance after : " + PublicMethed
        .queryAccount(contractAddress, blockingStubFull).getBalance());
    Assert.assertEquals(1, infoById.get().getResultValue());
    Assert.assertEquals("FAILED", infoById.get().getResult().toString());
    Assert.assertEquals(contractBalance - 10L, afterBalance.longValue());
    Assert.assertEquals(0, ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() < 10000000);

  }

  @Test(enabled = true, description = "Triggerconstant a transfer function")
  public void triggerContract04() {
    Account account = PublicMethed.queryAccount(accountExcAddress, blockingStubFull);
    Account contractAccount = PublicMethed.queryAccount(contractAddress, blockingStubFull);

    final Long AccountBeforeBalance = account.getBalance();
    final Long contractAccountBalance = contractAccount.getBalance();

    TransactionExtention return1 = PublicMethed.triggerConstantContractForExtention(contractAddress,
        "testTransferTrxInsufficientBalance(uint256)", "1", false, 0L, 1000000000, "0", 0L,
        accountExcAddress, accountExcKey, blockingStubFull);
    Assert.assertEquals(response_code.SUCCESS, return1.getResult().getCode());
    /*Assert.assertEquals(
        "class org.tron.core.vm.program.Program$StaticCallModificationException "
            + ": Attempt to call a state modifying opcode inside STATICCALL",
        return1.getResult().getMessage().toStringUtf8());*/

    logger.info("return1: " + return1);

    account = PublicMethed.queryAccount(accountExcAddress, blockingStubFull);
    contractAccount = PublicMethed.queryAccount(contractAddress, blockingStubFull);

    Assert.assertEquals(AccountBeforeBalance.longValue(), account.getBalance());
    Assert.assertEquals(contractAccountBalance.longValue(), contractAccount.getBalance());
  }

  /**
   * constructor.
   */
  @AfterClass

  public void shutdown() throws InterruptedException {
    PublicMethed
        .freedResource(accountExcAddress, accountExcKey, testNetAccountAddress, blockingStubFull);
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
}
