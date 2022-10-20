package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import com.google.protobuf.ByteString;
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
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class AbiEncodeTest {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  private byte[] contractAddress = null;

  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] dev001Address = ecKey1.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());



  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {

    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    PublicMethed.printAddress(dev001Key);
  }

  @Test(enabled = true, description = "Deploy contract")
  public void test01DeployContract() {
    Assert.assertTrue(PublicMethed.sendcoin(dev001Address, 1000_000_000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress, 100_000_000L,
        0, 0, ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //before deploy, check account resource
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    Protocol.Account info = PublicMethed.queryAccount(dev001Key, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = accountResource.getEnergyUsed();
    Long beforeNetUsed = accountResource.getNetUsed();
    Long beforeFreeNetUsed = accountResource.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    String filePath = "./src/test/resources/soliditycode/abiencode.sol";
    String contractName = "AbiEncode";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    logger.info("abi:" + abi);

    final String txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, dev001Key,
            dev001Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    if (infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage());
    }

    TransactionInfo transactionInfo = infoById.get();
    logger.info("EnergyUsageTotal: " + transactionInfo.getReceipt().getEnergyUsageTotal());
    logger.info("NetUsage: " + transactionInfo.getReceipt().getNetUsage());

    contractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethed.getContract(contractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

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

    Protocol.Account infoafter = PublicMethed.queryAccount(dev001Key, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed
        .getAccountResource(dev001Address,
            blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
  }

  @Test(enabled = true, description = "Trigger contract with ")
  public void test02TriggerContract() {
    String methodStr = "h(int256[2][])";
    String argStr = "00000000000000000000000000000000000000000000000000000000000000200000000000000"
        + "000000000000000000000000000000000000000000000000003000000000000000000000000000000000000"
        + "000000000000000000000000000300000000000000000000000000000000000000000000000000000000000"
        + "000040000000000000000000000000000000000000000000000000000000000000000000000000000000000"
        + "000000000000000000000000000000000000000000006300000000000000000000000000000000000000000"
        + "000000000000000000000060000000000000000000000000000000000000000000000000000000000000008";
    String txid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, true,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    if (infoById.get().getResultValue() != 0) {
      Assert.fail("trigger contract failed with message: " + infoById.get().getResMessage());
    }
    logger.info("infoById" + infoById);
    String contractResult =
        ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray());
    Assert.assertEquals(
        "000000000000000000000000000000000000000000000000000000000000002000000000000000000"
            + "00000000000000000000000000000000000000000000100000000000000000000000000000000000000"
            + "00000000000000000000000000200000000000000000000000000000000000000000000000000000000"
            + "00000000300000000000000000000000000000000000000000000000000000000000000030000000000"
            + "00000000000000000000000000000000000000000000000000000400000000000000000000000000000"
            + "00000000000000000000000000000000000000000000000000000000000000000000000000000000000"
            + "00000000000000630000000000000000000000000000000000000000000000000000000000000006000"
            + "0000000000000000000000000000000000000000000000000000000000008",
        contractResult);

    String methodStr1 = "i(int256[2][2])";
    String argStr1 = "0000000000000000000000000000000000000000000000000000000000000005000000000000"
        + "000000000000000000000000000000000000000000000000000700000000000000000000000000000000000"
        + "000000000000000000000000003e80000000000000000000000000000000000000000000000000000000000"
        + "000065";
    String txid1 = PublicMethed.triggerContract(contractAddress, methodStr1, argStr1, true,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById1 = PublicMethed
        .getTransactionInfoById(txid1, blockingStubFull);
    if (infoById1.get().getResultValue() != 0) {
      Assert.fail("trigger contract failed with message: " + infoById1.get().getResMessage());
    }
    logger.info("infoById1" + infoById1);
    String contractResult1 =
        ByteArray.toHexString(infoById1.get().getContractResult(0).toByteArray());
    Assert.assertEquals(
        "000000000000000000000000000000000000000000000000000000000000002000000000000000000"
            + "00000000000000000000000000000000000000000000080000000000000000000000000000000000000"
            + "00000000000000000000000000050000000000000000000000000000000000000000000000000000000"
            + "00000000700000000000000000000000000000000000000000000000000000000000003e80000000000"
            + "000000000000000000000000000000000000000000000000000065",
        contractResult1);
  }

  @Test(enabled = true, description = "Trigger contract with negative number")
  public void test03TriggerContract() {
    String methodStr = "h(int256[2][])";
    String argStr = "00000000000000000000000000000000000000000000000000000000000000200000000000000"
        + "000000000000000000000000000000000000000000000000003ffffffffffffffffffffffffffffffffffff"
        + "ffffffffffffffffffffffffffff00000000000000000000000000000000000000000000000000000000000"
        + "000090000000000000000000000000000000000000000000000000000000000000042ffffffffffffffffff"
        + "ffffffffffffffffffffffffffffffffffffffffffffbe00000000000000000000000000000000000000000"
        + "000000000000000000000b1ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffa8";
    String txid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, true,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    if (infoById.get().getResultValue() != 0) {
      Assert.fail("trigger contract failed with message: " + infoById.get().getResMessage());
    }
    logger.info("infoById" + infoById);
    String contractResult =
        ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray());
    Assert.assertEquals(
        "000000000000000000000000000000000000000000000000000000000000002000000000000000000"
            + "00000000000000000000000000000000000000000000100000000000000000000000000000000000000"
            + "00000000000000000000000000200000000000000000000000000000000000000000000000000000000"
            + "000000003ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff0000000000"
            + "00000000000000000000000000000000000000000000000000000900000000000000000000000000000"
            + "00000000000000000000000000000000042ffffffffffffffffffffffffffffffffffffffffffffffff"
            + "ffffffffffffffbe00000000000000000000000000000000000000000000000000000000000000b1fff"
            + "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffa8",
        contractResult);

    String methodStr1 = "i(int256[2][2])";
    String argStr1 = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff000000000000"
        + "000000000000000000000000000000000000000000000000000900000000000000000000000000000000000"
        + "00000000000000000000000000042ffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
        + "ffffbe";
    String txid1 = PublicMethed.triggerContract(contractAddress, methodStr1, argStr1, true,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById1 = PublicMethed
        .getTransactionInfoById(txid1, blockingStubFull);
    if (infoById1.get().getResultValue() != 0) {
      Assert.fail("trigger contract failed with message: " + infoById1.get().getResMessage());
    }
    logger.info("infoById1" + infoById1);
    String contractResult1 =
        ByteArray.toHexString(infoById1.get().getContractResult(0).toByteArray());
    Assert.assertEquals(
        "00000000000000000000000000000000000000000000000000000000000000200000000000000000"
            + "000000000000000000000000000000000000000000000080ffffffffffffffffffffffffffffffffff"
            + "ffffffffffffffffffffffffffffff0000000000000000000000000000000000000000000000000000"
            + "0000000000090000000000000000000000000000000000000000000000000000000000000042ffffff"
            + "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffbe",
        contractResult1);
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    long balance = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();
    PublicMethed.sendcoin(fromAddress, balance, dev001Address, dev001Key,
        blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}



