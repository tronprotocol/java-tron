package stest.tron.wallet.contract.scenario;

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
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.SmartContract;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractScenario012 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  byte[] contractAddress = null;
  String txid = "";
  Optional<TransactionInfo> infoById = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contract012Address = ecKey1.getAddress();
  String contract012Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] receiverAddress = ecKey2.getAddress();
  String receiverKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

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

    ecKey2 = new ECKey(Utils.getRandom());
    receiverAddress = ecKey2.getAddress();
    receiverKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());


    Assert.assertTrue(PublicMethed.sendcoin(contract012Address,2000000000L,fromAddress,
        testKey002,blockingStubFull));
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(contract012Address,
        blockingStubFull);
    Long energyLimit = accountResource.getEnergyLimit();
    Long energyUsage = accountResource.getEnergyUsed();

    logger.info("before energy limit is " + Long.toString(energyLimit));
    logger.info("before energy usage is " + Long.toString(energyUsage));
    String contractName = "TransactionCoin";
    String code = Configuration.getByPath("testng.conf")
            .getString("code.code_ContractScenario012_deployTransactionCoin");
    String abi = Configuration.getByPath("testng.conf")
            .getString("abi.abi_ContractScenario012_deployTransactionCoin");
    contractAddress = PublicMethed.deployContract(contractName,abi,code,"",maxFeeLimit,
        0L, 100,null,contract012Key,contract012Address,blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress,blockingStubFull);
    Assert.assertTrue(smartContract.getAbi() != null);
  }

  @Test(enabled = true)
  public void test2TriggerTransactionCoin() {
    //When the contract has no money,transaction coin failed.
    String receiveAddress = "\"" + Base58.encode58Check(receiverAddress)
        + "\"";
    txid = PublicMethed.triggerContract(contractAddress,
        "sendToAddress2(address)", receiveAddress, false,
        0, 100000000L, contract012Address, contract012Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info(txid);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 1);
    logger.info("energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() > 0);
    Assert.assertTrue(infoById.get().getFee() == infoById.get().getReceipt().getEnergyFee());
    Assert.assertFalse(infoById.get().getContractAddress().isEmpty());

    //Send some trx to the contract account.
    Assert.assertTrue(PublicMethed.sendcoin(contractAddress,100000L,contract012Address,
        contract012Key,blockingStubFull));


    //In smart contract, you can't create account
    txid = PublicMethed.triggerContract(contractAddress,
        "sendToAddress2(address)", receiveAddress, false,
        0, 100000000L, contract012Address, contract012Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info(txid);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("result is " + infoById.get().getResultValue());
    Assert.assertTrue(infoById.get().getResultValue() == 1);
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() > 0);
    Assert.assertTrue(infoById.get().getFee() == infoById.get().getReceipt().getEnergyFee());
    Assert.assertFalse(infoById.get().getContractAddress().isEmpty());

    //This time, trigger the methed sendToAddress2 is OK.
    Assert.assertTrue(PublicMethed.sendcoin(receiverAddress,10000000L,fromAddress,
        testKey002,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    txid = PublicMethed.triggerContract(contractAddress,
        "sendToAddress2(address)", receiveAddress, false,
        0, 100000000L, contract012Address, contract012Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info(txid);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("result is " + infoById.get().getResultValue());
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


