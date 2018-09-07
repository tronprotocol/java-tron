package stest.tron.wallet.contract.scenario;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
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
import org.tron.protos.Protocol.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractScenario001 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contract001Address = ecKey1.getAddress();
  String contract001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(contract001Key);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    Assert.assertTrue(PublicMethed.sendcoin(contract001Address,20000000L,fromAddress,
        testKey002,blockingStubFull));
    logger.info(Long.toString(PublicMethed.queryAccount(contract001Key,blockingStubFull)
        .getBalance()));
  }

  @Test(enabled = true)
  public void deployAddressDemo() {
    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(contract001Address, 1000000L,
        3,1,contract001Key,blockingStubFull));
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(contract001Address,
        blockingStubFull);
    Long energyLimit = accountResource.getEnergyLimit();
    Long energyUsage = accountResource.getEnergyUsed();

    logger.info("before energy limit is " + Long.toString(energyLimit));
    logger.info("before energy usage is " + Long.toString(energyUsage));
    String contractName = "addressDemo";
    String code = "608060405234801561001057600080fd5b5060bf8061001f6000396000f3006080604052600436"
        + "1060485763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035041"
        + "66313d1aa2e8114604d5780637995b15b146067575b600080fd5b348015605857600080fd5b506065600435"
        + "602435608b565b005b348015607257600080fd5b506079608f565b60408051918252519081900360200190f"
        + "35b5050565b42905600a165627a7a72305820086db30620ef850edcb987d91625ecf5a1c342dc87dbabb4fe"
        + "4b29ec8c1623c10029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"start\",\"type\":\"uint256\"},{\"na"
        + "me\":\"daysAfter\",\"type\":\"uint256\"}],\"name\":\"f\",\"outputs\":[],\"payable\":fal"
        + "se,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inpu"
        + "ts\":[],\"name\":\"nowInSeconds\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\""
        + "payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    byte[] contractAddress = PublicMethed.deployContract(contractName,abi,code,"",maxFeeLimit,
        0L, 100,null,contract001Key,contract001Address,blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress,blockingStubFull);
    Assert.assertTrue(smartContract.getAbi() != null);
    accountResource = PublicMethed.getAccountResource(contract001Address,blockingStubFull);
    energyLimit = accountResource.getEnergyLimit();
    energyUsage = accountResource.getEnergyUsed();
    Assert.assertTrue(energyLimit > 0);
    Assert.assertTrue(energyUsage > 0);

    logger.info("after energy limit is " + Long.toString(energyLimit));
    logger.info("after energy usage is " + Long.toString(energyUsage));
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


