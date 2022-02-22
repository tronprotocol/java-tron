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
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractScenario012 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  byte[] contractAddress = null;
  String txid = "";
  Optional<TransactionInfo> infoById = null;
  String receiveAddressParam;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contract012Address = ecKey1.getAddress();
  String contract012Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] receiverAddress = ecKey2.getAddress();
  String receiverKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");



  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(contract012Key);
    PublicMethed.printAddress(receiverKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = true)
  public void test1DeployTransactionCoin() {
    ecKey1 = new ECKey(Utils.getRandom());
    contract012Address = ecKey1.getAddress();
    contract012Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    Assert.assertTrue(PublicMethed.sendcoin(contract012Address, 2000000000L, fromAddress,
        testKey002, blockingStubFull));
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(contract012Address,
        blockingStubFull);
    Long energyLimit = accountResource.getEnergyLimit();
    Long energyUsage = accountResource.getEnergyUsed();

    logger.info("before energy limit is " + Long.toString(energyLimit));
    logger.info("before energy usage is " + Long.toString(energyUsage));
    String filePath = "./src/test/resources/soliditycode/contractScenario012.sol";
    String contractName = "PayTest";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    String txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit, 0L, 100,
            null, contract012Key, contract012Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    logger.info("energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    contractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getAbi() != null);
  }


  @Test(enabled = true)
  public void test2TriggerTransactionCoin() {
    Account account = PublicMethed.queryAccount(contractAddress, blockingStubFull);
    logger.info("contract Balance : -- " + account.getBalance());
    receiveAddressParam = "\"" + Base58.encode58Check(fromAddress)
        + "\"";
    //When the contract has no money,transaction coin failed.
    txid = PublicMethed.triggerContract(contractAddress,
        "sendToAddress2(address)", receiveAddressParam, false,
        0, 100000000L, contract012Address, contract012Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info(txid);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);
    Assert.assertTrue(infoById.get().getResultValue() == 1);
    logger.info("energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() > 0);
    Assert.assertTrue(infoById.get().getFee() == infoById.get().getReceipt().getEnergyFee());
    Assert.assertFalse(infoById.get().getContractAddress().isEmpty());
  }


  @Test(enabled = true)
  public void test3TriggerTransactionCanNotCreateAccount() {
    ecKey2 = new ECKey(Utils.getRandom());
    receiverAddress = ecKey2.getAddress();
    receiverKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    //Send some trx to the contract account.
    Account account = PublicMethed.queryAccount(contractAddress, blockingStubFull);
    logger.info("contract Balance : -- " + account.getBalance());
    receiveAddressParam = "\"" + Base58.encode58Check(receiverAddress)
        + "\"";
    //In smart contract, you can create account
    txid = PublicMethed.triggerContract(contractAddress,
        "sendToAddress2(address)", receiveAddressParam, false,
        1000000000L, 100000000L, contract012Address, contract012Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info(txid);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);
    logger.info("result is " + infoById.get().getResultValue());
    logger.info("energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() > 0);
    Assert.assertTrue(infoById.get().getFee() == infoById.get().getReceipt().getEnergyFee());
    Assert.assertFalse(infoById.get().getContractAddress().isEmpty());

    Account account2 = PublicMethed.queryAccount(receiverAddress, blockingStubFull);
    Assert.assertEquals(5L, account2.getBalance());

  }


  @Test(enabled = true)
  public void test4TriggerTransactionCoin() {
    receiveAddressParam = "\"" + Base58.encode58Check(receiverAddress)
        + "\"";
    Account account = PublicMethed.queryAccount(contractAddress, blockingStubFull);
    logger.info("contract Balance : -- " + account.getBalance());
    //This time, trigger the methed sendToAddress2 is OK.
    Assert.assertTrue(PublicMethed.sendcoin(receiverAddress, 10000000L, toAddress,
        testKey003, blockingStubFull));
    txid = PublicMethed.triggerContract(contractAddress,
        "sendToAddress2(address)", receiveAddressParam, false,
        0, 100000000L, contract012Address, contract012Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info(txid);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);
    logger.info("result is " + infoById.get().getResultValue());
    logger.info("energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() > 0);
    Assert.assertTrue(infoById.get().getFee() == infoById.get().getReceipt().getEnergyFee());
    Assert.assertFalse(infoById.get().getContractAddress().isEmpty());

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


