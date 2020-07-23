package stest.tron.wallet.dailybuild.manual;

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
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractScenario002 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contract002Address = ecKey1.getAddress();
  String contract002Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private String txid;
  private ManagedChannel channelFull = null;
  private ManagedChannel channelSolidity = null;
  private ManagedChannel channelSoliInFull = null;
  private ManagedChannel channelPbft = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSoliInFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubPbft = null;
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
  private String soliInFullnode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(1);
  private String soliInPbft = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(2);

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
    PublicMethed.printAddress(contract002Key);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);

    channelSoliInFull = ManagedChannelBuilder.forTarget(soliInFullnode)
        .usePlaintext(true)
        .build();
    blockingStubSoliInFull = WalletSolidityGrpc.newBlockingStub(channelSoliInFull);

    channelPbft = ManagedChannelBuilder.forTarget(soliInPbft)
        .usePlaintext(true)
        .build();
    blockingStubPbft = WalletSolidityGrpc.newBlockingStub(channelPbft);

    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

  }

  @Test(enabled = true, description = "Deploy contract with java-tron support interface")
  public void test01DeployTronNative() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] contract002Address = ecKey1.getAddress();
    String contract002Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    Assert.assertTrue(PublicMethed.sendcoin(contract002Address, 500000000L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(contract002Address, 1000000L,
        0, 1, contract002Key, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(contract002Address,
        blockingStubFull);
    Long energyLimit = accountResource.getEnergyLimit();
    Long energyUsage = accountResource.getEnergyUsed();
    Long balanceBefore = PublicMethed.queryAccount(contract002Key, blockingStubFull).getBalance();

    logger.info("before energy limit is " + Long.toString(energyLimit));
    logger.info("before energy usage is " + Long.toString(energyUsage));
    logger.info("before balance is " + Long.toString(balanceBefore));

    String contractName = "TronNative";
    String filePath = "./src/test/resources/soliditycode/contractScenario002.sol";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    txid = PublicMethed.deployContractAndGetTransactionInfoById(contractName, abi, code, "",
        maxFeeLimit, 0L, 100, null, contract002Key, contract002Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull1);

    logger.info(txid);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    com.google.protobuf.ByteString contractAddress = infoById.get().getContractAddress();
    SmartContract smartContract = PublicMethed
        .getContract(contractAddress.toByteArray(), blockingStubFull);
    Assert.assertTrue(smartContract.getAbi() != null);
    PublicMethed.waitProduceNextBlock(blockingStubFull1);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    accountResource = PublicMethed.getAccountResource(contract002Address, blockingStubFull1);
    energyLimit = accountResource.getEnergyLimit();
    energyUsage = accountResource.getEnergyUsed();
    Long balanceAfter = PublicMethed.queryAccount(contract002Address, blockingStubFull1)
        .getBalance();

    logger.info("after energy limit is " + Long.toString(energyLimit));
    logger.info("after energy usage is " + Long.toString(energyUsage));
    logger.info("after balance is " + Long.toString(balanceAfter));
    logger.info("transaction fee is " + Long.toString(infoById.get().getFee()));

    Assert.assertTrue(energyUsage > 0);
    Assert.assertTrue(balanceBefore == balanceAfter + infoById.get().getFee());
    PublicMethed.unFreezeBalance(contract002Address, contract002Key, 1,
        contract002Address, blockingStubFull);

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get smart contract with invalid address")
  public void test02GetContractWithInvalidAddress() {
    byte[] contractAddress = contract002Address;
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    logger.info(smartContract.getAbi().toString());
    Assert.assertTrue(smartContract.getAbi().toString().isEmpty());
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get transaction by id from solidity")
  public void test03GetTransactionByIdFromSolidity() {
    Assert.assertFalse(PublicMethed.getTransactionById(txid, blockingStubSolidity)
        .get().getSignature(0).isEmpty());
    Assert.assertEquals(PublicMethed.getTransactionById(txid, blockingStubFull),
        PublicMethed.getTransactionById(txid, blockingStubSolidity));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get transaction by id from PBFT")
  public void test04GetTransactionByIdFromPbft() {
    Assert.assertFalse(PublicMethed.getTransactionById(txid, blockingStubPbft)
        .get().getSignature(0).isEmpty());
    Assert.assertEquals(PublicMethed.getTransactionById(txid, blockingStubSoliInFull),
        PublicMethed.getTransactionById(txid, blockingStubPbft));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get transaction by id from Solidity")
  public void test05GetTransactionInfoByIdFromSolidity() throws Exception {
    long netUsage = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get().getReceipt()
        .getNetUsage();

    Assert.assertEquals(PublicMethed.getTransactionInfoByIdFromSolidity(txid, blockingStubSolidity)
        .get().getReceipt().getNetUsage(), netUsage);

    Assert
        .assertEquals(PublicMethed.getTransactionInfoByIdFromSolidity(txid, blockingStubSoliInFull)
            .get().getReceipt().getNetUsage(), netUsage);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get transaction by id from PBFT")
  public void test06GetTransactionInfoByIdFromPbft() {
    long energyUsage = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get()
        .getReceipt()
        .getEnergyUsage();

    Assert.assertEquals(PublicMethed.getTransactionInfoByIdFromSolidity(txid, blockingStubPbft)
        .get().getReceipt().getEnergyUsage(), energyUsage);
  }


  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed.freedResource(contract002Address, contract002Key, fromAddress, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelPbft != null) {
      channelPbft.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSoliInFull != null) {
      channelSoliInFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
