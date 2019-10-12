package stest.tron.wallet.contract.scenario;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
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
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractScenario010 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contract009Address = ecKey1.getAddress();
  String contract009Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

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
    PublicMethed.printAddress(contract009Key);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = true)
  public void deployContainLibraryContract() {
    ecKey1 = new ECKey(Utils.getRandom());
    contract009Address = ecKey1.getAddress();
    contract009Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    Assert.assertTrue(PublicMethed.sendcoin(contract009Address, 600000000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(contract009Address, 10000000L,
        3, 1, contract009Key, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(contract009Address,
        blockingStubFull);
    Long energyLimit = accountResource.getEnergyLimit();
    Long energyUsage = accountResource.getEnergyUsed();
    Long netUsage = accountResource.getNetUsed();

    logger.info("before energy limit is " + Long.toString(energyLimit));
    logger.info("before energy usage is " + Long.toString(energyUsage));
    logger.info("before Net usage is " + Long.toString(netUsage));
    String filePath = "./src/test/resources/soliditycode/contractScenario010.sol";
    String contractName = "TRON_ERC721";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    byte[] libraryAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contract009Key, contract009Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(libraryAddress, blockingStubFull);

    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
    logger.info(ByteArray.toHexString(smartContract.getContractAddress().toByteArray()));
    accountResource = PublicMethed.getAccountResource(contract009Address, blockingStubFull);
    energyLimit = accountResource.getEnergyLimit();
    energyUsage = accountResource.getEnergyUsed();
    netUsage = accountResource.getNetUsed();
    Assert.assertTrue(energyLimit > 0);
    Assert.assertTrue(energyUsage > 0);

    logger.info("after energy limit is " + Long.toString(energyLimit));
    logger.info("after energy usage is " + Long.toString(energyUsage));
    logger.info("after Net usage is " + Long.toString(netUsage));
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


