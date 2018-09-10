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

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(contract012Key);
    PublicMethed.printAddress(receiverKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    Assert.assertTrue(PublicMethed.sendcoin(contract012Address,2000000000L,fromAddress,
        testKey002,blockingStubFull));
  }

  @Test(enabled = true)
  public void deployTransactionCoin() {
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(contract012Address,
        blockingStubFull);
    Long energyLimit = accountResource.getEnergyLimit();
    Long energyUsage = accountResource.getEnergyUsed();

    logger.info("before energy limit is " + Long.toString(energyLimit));
    logger.info("before energy usage is " + Long.toString(energyUsage));
    String contractName = "TransactionCoin";
    String code = "60806040526000805561029f806100176000396000f3006080604052600436106100985763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166312065fe0811461009d5780632e52d606146100b7578063483f5a7f146100cc5780634f8632ba146100e25780635896476c146101135780638b47145f14610128578063b6b55f2514610144578063f46771d91461014f578063ff18253b14610163575b600080fd5b6100a561019b565b60408051918252519081900360200190f35b3480156100c357600080fd5b506100a56101a0565b6100e0600160a060020a03600435166101a6565b005b3480156100ee57600080fd5b506100f76101df565b60408051600160a060020a039092168252519081900360200190f35b34801561011f57600080fd5b506100e06101ee565b6101306101f9565b604080519115158252519081900360200190f35b610130600435610217565b6100e0600160a060020a036004351661023a565b34801561016f57600080fd5b5061017861026c565b60408051600160a060020a03909316835260208301919091528051918290030190f35b303190565b60005481565b604051600160a060020a038216903480156108fc02916000818181858888f193505050501580156101db573d6000803e3d6000fd5b5050565b600154600160a060020a031681565b600080546001019055565b6040516000903390829060019082818181858883f194505050505090565b604051600090339083156108fc0290849084818181858888f19695505050505050565b604051600160a060020a0382169060009060059082818181858883f193505050501580156101db573d6000803e3d6000fd5b33803190915600a165627a7a72305820fd081d59bd77b97252e4a657177023ae7352e1fe802dd638ec6b9fa5df59d6110029";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"getBalance\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"n\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_receiver\",\"type\":\"address\"}],\"name\":\"sendToAddress\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"user\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"nPlusOne\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"depositOneCoin\",\"outputs\":[{\"name\":\"success\",\"type\":\"bool\"}],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"money\",\"type\":\"uint256\"}],\"name\":\"deposit\",\"outputs\":[{\"name\":\"success\",\"type\":\"bool\"}],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_receiver\",\"type\":\"address\"}],\"name\":\"sendToAddress2\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"getSenderBalance\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"},{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
    contractAddress = PublicMethed.deployContract(contractName,abi,code,"",maxFeeLimit,
        0L, 100,null,contract012Key,contract012Address,blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress,blockingStubFull);
    Assert.assertTrue(smartContract.getAbi() != null);
  }

  @Test(enabled = true)
  public void triggerTransactionCoin() {
    //When the contract has no money,transaction coin failed.
    String receiveAddress = "\"" + Base58.encode58Check(receiverAddress)
        + "\"";
    txid = PublicMethed.triggerContract(contractAddress,
        "sendToAddress2(address)", receiveAddress, false,
        0, 100000000L, contract012Address, contract012Key, blockingStubFull);
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
    txid = PublicMethed.triggerContract(contractAddress,
        "sendToAddress2(address)", receiveAddress, false,
        0, 100000000L, contract012Address, contract012Key, blockingStubFull);
    logger.info(txid);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("result is " + infoById.get().getResultValue());
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() > 0);
    Assert.assertTrue(infoById.get().getFee() == infoById.get().getReceipt().getEnergyFee());
    Assert.assertFalse(infoById.get().getContractAddress().isEmpty());

  }



  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


