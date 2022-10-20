package stest.tron.wallet.dailybuild.tvmnewcommand.triggerconstant;

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
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class TriggerConstant012 {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  byte[] contractAddress = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
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
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);



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
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
  }

  @Test(enabled = false, description = "TriggerContract a pure function without ABI")
  public void testTriggerContract() {
    Assert.assertTrue(PublicMethed
        .sendcoin(contractExcAddress, 1000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/TriggerConstant004.sol";
    String contractName = "testConstantContract";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    contractAddress = PublicMethed.deployContract(contractName, "[]", code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getAbi().toString().isEmpty());
    Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
    Account info;

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    String txid = "";

    txid = PublicMethed
        .triggerContract(contractAddress,
            "testPure()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
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

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnnumber);


  }


  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed
        .freedResource(contractExcAddress, contractExcKey, testNetAccountAddress, blockingStubFull);

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
