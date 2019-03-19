package stest.tron.wallet.dailybuild.manual;

import static org.hamcrest.core.StringContains.containsString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
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
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractUnknownException {


  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelSolidity = null;

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;

  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);


  byte[] contractAddress = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] grammarAddress = ecKey1.getAddress();
  String testKeyForGrammarAddress = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] grammarAddress2 = ecKey2.getAddress();
  String testKeyForGrammarAddress2 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());


  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] grammarAddress3 = ecKey3.getAddress();
  String testKeyForGrammarAddress3 = ByteArray.toHexString(ecKey3.getPrivKeyBytes());

  ECKey ecKey4 = new ECKey(Utils.getRandom());
  byte[] grammarAddress4 = ecKey4.getAddress();
  String testKeyForGrammarAddress4 = ByteArray.toHexString(ecKey4.getPrivKeyBytes());

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
    PublicMethed.printAddress(testKeyForGrammarAddress);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    logger.info(Long.toString(PublicMethed.queryAccount(testNetAccountKey, blockingStubFull)
        .getBalance()));
  }

  @Test(enabled = true, description = "trigger selfdestruct method")
  public void testGrammar001() {
    Assert.assertTrue(PublicMethed
        .sendcoin(grammarAddress, 1000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(grammarAddress, 204800000,
        0, 1, testKeyForGrammarAddress, blockingStubFull));
    Account info;
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(grammarAddress,
        blockingStubFull);
    info = PublicMethed.queryAccount(grammarAddress, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    long beforeenergyLimit = resourceInfo.getEnergyLimit();

    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("beforeenergyLimit:" + beforeenergyLimit);
    String contractName = "testA";
    String code = "60806040526000600a600e609f565b6040518091039082f0801580156028573d600080"
        + "3e3d6000fd5b509050905080600160a060020a031663946644cd6040518163ffffffff167c0100"
        + "000000000000000000000000000000000000000000000000000000028152600401600060405180"
        + "830381600087803b158015608357600080fd5b505af11580156096573d6000803e3d6000fd5b50"
        + "5050505060ad565b60405160088060ef83390190565b60358060ba6000396000f3006080604052"
        + "600080fd00a165627a7a723058205f699e7434a691ee9a433c497973f2eee624efde40e7b7dd86"
        + "512767fbe7752c0029608060405233ff00";
    String abi = "[{\"inputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\","
        + "\"type\":\"constructor\"}]";
    String txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            20L, 100, null, testKeyForGrammarAddress,
            grammarAddress, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    final String s = infoById.get().getResMessage().toStringUtf8();
    long fee = infoById.get().getFee();
    long energyUsage = infoById.get().getReceipt().getEnergyUsage();
    long energyFee = infoById.get().getReceipt().getEnergyFee();
    Account infoafter = PublicMethed.queryAccount(grammarAddress, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(grammarAddress,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfo.getNetUsed();
    Long afterFreeNetUsed = resourceInfo.getFreeNetUsed();
    long aftereenergyLimit = resourceInfo.getEnergyLimit();

    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("afterenergyLimit:" + aftereenergyLimit);
    Assert.assertThat(s, containsString("REVERT opcode executed"));

  }

  @Test(enabled = true, description = "trigger revert method")
  public void testGrammar002() {
    PublicMethed
        .sendcoin(grammarAddress2, 100000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(grammarAddress2, 10000000L,
        0, 1, testKeyForGrammarAddress2, blockingStubFull));
    Account info;
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(grammarAddress2,
        blockingStubFull);
    info = PublicMethed.queryAccount(grammarAddress2, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    long beforeenergyLimit = resourceInfo.getEnergyLimit();

    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("beforeenergyLimit:" + beforeenergyLimit);
    String contractName = "testB";
    String code = "60806040526000600a600e609f565b6040518091039082f080158015602"
        + "8573d6000803e3d6000fd5b509050905080600160a060020a031663946644cd6040"
        + "518163ffffffff167c0100000000000000000000000000000000000000000000000"
        + "000000000028152600401600060405180830381600087803b158015608357600080"
        + "fd5b505af11580156096573d6000803e3d6000fd5b505050505060ae565b6040516"
        + "00a806100f183390190565b6035806100bc6000396000f3006080604052600080fd"
        + "00a165627a7a7230582036a40a807cbf71508011574ef42c706ad7b40d844807909"
        + "c3b8630f9fb9ae6f700296080604052600080fd00";
    String abi = "[{\"inputs\":[],\"payable\":false,\"stateMutability\":\""
        + "nonpayable\",\"type\":\"constructor\"}]";
    String txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            20L, 100, null, testKeyForGrammarAddress2,
            grammarAddress2, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    final long fee = infoById.get().getFee();
    final long energyUsage = infoById.get().getReceipt().getEnergyUsage();
    final long energyFee = infoById.get().getReceipt().getEnergyFee();

    final String s = infoById.get().getResMessage().toStringUtf8();

    Account infoafter = PublicMethed.queryAccount(grammarAddress2, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(grammarAddress2,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfo.getNetUsed();
    Long afterFreeNetUsed = resourceInfo.getFreeNetUsed();
    long aftereenergyLimit = resourceInfo.getEnergyLimit();

    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("afterenergyLimit:" + aftereenergyLimit);
    Assert.assertThat(s, containsString("REVERT opcode executed"));
    Assert.assertFalse(energyFee == 1000000000);

    Assert.assertTrue(beforeBalance - fee == afterBalance);

  }

  @Test(enabled = true, description = "trigger assert method")
  public void testGrammar003() {
    PublicMethed
        .sendcoin(grammarAddress3, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(grammarAddress3, 1000000000L,
        0, 1, testKeyForGrammarAddress3, blockingStubFull));
    Account info;
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(grammarAddress3,
        blockingStubFull);
    info = PublicMethed.queryAccount(grammarAddress3, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    long beforeenergyLimit = resourceInfo.getEnergyLimit();

    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("beforeenergyLimit:" + beforeenergyLimit);
    String contractName = "testC";
    String code = "60806040526000600a600e609f565b6040518091039082f0801580156028573d600"
        + "0803e3d6000fd5b509050905080600160a060020a031663946644cd6040518163ffffffff16"
        + "7c0100000000000000000000000000000000000000000000000000000000028152600401600"
        + "060405180830381600087803b158015608357600080fd5b505af11580156096573d6000803e"
        + "3d6000fd5b505050505060ad565b60405160078060ef83390190565b60358060ba600039600"
        + "0f3006080604052600080fd00a165627a7a72305820970ee7543687d338b72131a122af927a"
        + "698a081c0118577f49fffd8831a1195800296080604052fe00";
    String abi = "[{\"inputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\","
        + "\"type\":\"constructor\"}]";
    String txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            20L, 100, null, testKeyForGrammarAddress3,
            grammarAddress3, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    final long fee = infoById.get().getFee();
    final long energyUsage = infoById.get().getReceipt().getEnergyUsage();
    final long energyFee = infoById.get().getReceipt().getEnergyFee();
    String s = infoById.get().getResMessage().toStringUtf8();
    Account infoafter = PublicMethed.queryAccount(grammarAddress3, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(grammarAddress3,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfo.getNetUsed();
    Long afterFreeNetUsed = resourceInfo.getFreeNetUsed();
    long aftereenergyLimit = resourceInfo.getEnergyLimit();

    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("afterenergyLimit:" + aftereenergyLimit);
    logger.info("s:" + s);
    Assert.assertThat(s, containsString("Not enough energy for"));

    Assert.assertTrue(beforeBalance - fee == afterBalance);

  }


  @Test(enabled = true, description = "trigger require method")
  public void testGrammar004() {
    PublicMethed
        .sendcoin(grammarAddress4, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(grammarAddress4, 100000000L,
        0, 1, testKeyForGrammarAddress4, blockingStubFull));
    Account info;
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(grammarAddress4,
        blockingStubFull);
    info = PublicMethed.queryAccount(grammarAddress4, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    long beforeenergyLimit = resourceInfo.getEnergyLimit();

    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("beforeenergyLimit:" + beforeenergyLimit);
    String contractName = "testD";
    String code = "60806040526000600a600e609f565b6040518091039082f0801580156028573d"
        + "6000803e3d6000fd5b509050905080600160a060020a031663946644cd6040518163fffff"
        + "fff167c010000000000000000000000000000000000000000000000000000000002815260"
        + "0401600060405180830381600087803b158015608357600080fd5b505af11580156096573"
        + "d6000803e3d6000fd5b505050505060ae565b604051600a806100f183390190565b603580"
        + "6100bc6000396000f3006080604052600080fd00a165627a7a72305820fd7ca23ea399b6d"
        + "513a8d4eb084f5eb748b94fab6437bfb5ea9f4a03d9715c3400296080604052600080fd00";
    String abi = "[{\"inputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\""
        + ",\"type\":\"constructor\"}]";
    String txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            20L, 100, null, testKeyForGrammarAddress4,
            grammarAddress4, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    final String s = infoById.get().getResMessage().toStringUtf8();
    final long fee = infoById.get().getFee();
    long energyUsage = infoById.get().getReceipt().getEnergyUsage();
    final long energyFee = infoById.get().getReceipt().getEnergyFee();

    Account infoafter = PublicMethed.queryAccount(grammarAddress4, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(grammarAddress4,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfo.getNetUsed();
    Long afterFreeNetUsed = resourceInfo.getFreeNetUsed();
    long aftereenergyLimit = resourceInfo.getEnergyLimit();

    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("afterenergyLimit:" + aftereenergyLimit);
    Assert.assertThat(s, containsString("REVERT opcode executed"));
    Assert.assertTrue(beforeBalance - fee == afterBalance);
    Assert.assertFalse(energyFee == 1000000000);


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
