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
public class ContractScenario009 {

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
  byte[] contract009Address = ecKey1.getAddress();
  String contract009Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

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
    Assert.assertTrue(PublicMethed.sendcoin(contract009Address,20000000L,fromAddress,
        testKey002,blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(contract009Address, 1000000L,
        3,1,contract009Key,blockingStubFull));
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(contract009Address,
        blockingStubFull);
    Long energyLimit = accountResource.getEnergyLimit();
    Long energyUsage = accountResource.getEnergyUsed();

    logger.info("before energy limit is " + Long.toString(energyLimit));
    logger.info("before energy usage is " + Long.toString(energyUsage));
    String contractName = "Library";
    String code = "608060405234801561001057600080fd5b50610139806100206000396000f3006080604052600436106100405763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035041663f207564e8114610045575b600080fd5b34801561005157600080fd5b5061005d60043561005f565b005b73610199610030600b82828239805160001a6073146000811461002057610022565bfe5b5030600052607381538281f300730000000000000000000000000000000000000000301460806040526004361061006d5763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035041663483b8a1481146100725780636ce8e081146100a1578063831cb739146100bc575b600080fd5b81801561007e57600080fd5b5061008d6004356024356100d7565b604080519115158252519081900360200190f35b8180156100ad57600080fd5b5061008d600435602435610117565b8180156100c857600080fd5b5061008d60043560243561012d565b60008181526020839052604081205460ff1615156100f757506000610111565b506000818152602083905260409020805460ff1916905560015b92915050565b6000908152602091909152604090205460ff1690565b60008181526020839052604081205460ff161561014c57506000610111565b50600090815260209190915260409020805460ff19166001908117909155905600a165627a7a723058200bc4068752b78840d32288f8eeffe2618c356d76fe09451d92f808cf28d4d22e0029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"self\",\"type\":\"Set.Data storage\"},{\"name\":\"value\",\"type\":\"uint256\"}],\"name\":\"remove\",\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"self\",\"type\":\"Set.Data storage\"},{\"name\":\"value\",\"type\":\"uint256\"}],\"name\":\"contains\",\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"self\",\"type\":\"Set.Data storage\"},{\"name\":\"value\",\"type\":\"uint256\"}],\"name\":\"insert\",\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    byte[] libraryAddress = PublicMethed.deployContract(contractName,abi,code,"",maxFeeLimit,
        0L, 100,null,contract009Key,contract009Address,blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(libraryAddress,blockingStubFull);

    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
    logger.info(ByteArray.toHexString(smartContract.getContractAddress().toByteArray()));
    accountResource = PublicMethed.getAccountResource(contract009Address,blockingStubFull);
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


