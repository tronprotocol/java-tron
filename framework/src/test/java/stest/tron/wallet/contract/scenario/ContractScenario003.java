package stest.tron.wallet.contract.scenario;

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
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractScenario003 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contract003Address = ecKey1.getAddress();
  String contract003Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
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
    PublicMethed.printAddress(contract003Key);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    logger.info(Long.toString(PublicMethed.queryAccount(contract003Key, blockingStubFull)
        .getBalance()));
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
  }

  @Test(enabled = true)
  public void deployErc223() {
    ecKey1 = new ECKey(Utils.getRandom());
    contract003Address = ecKey1.getAddress();
    contract003Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    Assert.assertTrue(PublicMethed.sendcoin(contract003Address, 500000000L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(contract003Address,
        blockingStubFull);
    Long energyLimit = accountResource.getEnergyLimit();
    Long energyUsage = accountResource.getEnergyUsed();
    Long balanceBefore = PublicMethed.queryAccount(contract003Key, blockingStubFull).getBalance();

    logger.info("before energy limit is " + Long.toString(energyLimit));
    logger.info("before energy usage is " + Long.toString(energyUsage));
    logger.info("before balance is " + Long.toString(balanceBefore));

    String filePath = "./src/test/resources/soliditycode/contractScenario003.sol";
    String contractName = "divideIHaveArgsReturnStorage";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    String txid = PublicMethed.deployContractAndGetTransactionInfoById(contractName, abi, code, "",
        maxFeeLimit, 0L, 100, null, contract003Key, contract003Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull1);
    logger.info(txid);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("Deploy energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    com.google.protobuf.ByteString contractAddress = infoById.get().getContractAddress();
    SmartContract smartContract = PublicMethed
        .getContract(contractAddress.toByteArray(), blockingStubFull);
    Assert.assertTrue(smartContract.getAbi() != null);
    Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());

    PublicMethed.waitProduceNextBlock(blockingStubFull1);
    accountResource = PublicMethed.getAccountResource(contract003Address, blockingStubFull1);
    energyLimit = accountResource.getEnergyLimit();
    energyUsage = accountResource.getEnergyUsed();
    Long balanceAfter = PublicMethed.queryAccount(contract003Address, blockingStubFull1)
        .getBalance();

    logger.info("after energy limit is " + Long.toString(energyLimit));
    logger.info("after energy usage is " + Long.toString(energyUsage));
    logger.info("after balance is " + Long.toString(balanceAfter));
    logger.info("transaction fee is " + Long.toString(infoById.get().getFee()));

    Assert.assertTrue(energyLimit == 0);
    Assert.assertTrue(energyUsage == 0);
    Assert.assertTrue(balanceBefore == balanceAfter + infoById.get().getFee());
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


