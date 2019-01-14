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
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractScenario014 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  byte[] contractAddress1 = null;
  byte[] contractAddress2 = null;
  byte[] contractAddress3 = null;
  String txid = "";
  Optional<TransactionInfo> infoById = null;
  String contractName = "";

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contract014Address = ecKey1.getAddress();
  String contract014Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

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
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = true)
  public void testTripleTrigger() {
    ecKey1 = new ECKey(Utils.getRandom());
    contract014Address = ecKey1.getAddress();
    contract014Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    ecKey2 = new ECKey(Utils.getRandom());
    receiverAddress = ecKey2.getAddress();
    receiverKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
    PublicMethed.printAddress(contract014Key);
    PublicMethed.printAddress(receiverKey);


    Assert.assertTrue(PublicMethed.sendcoin(contract014Address,5000000000L,fromAddress,
        testKey002,blockingStubFull));
    //Deploy contract1, contract1 has a function to transaction 5 sun to target account
    String contractName = "Contract1";
    String code = "608060405260d2806100126000396000f300608060405260043610603e5763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416633d96d24c81146043575b600080fd5b606273ffffffffffffffffffffffffffffffffffffffff600435166064565b005b60405173ffffffffffffffffffffffffffffffffffffffff82169060009060059082818181858883f1935050505015801560a2573d6000803e3d6000fd5b50505600a165627a7a72305820e2d0e2bbf60a802771a52693e71a934ef01e5c5f6a584b5a3f24f5088866de4d0029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"_receiver\",\"type\":\"address\"}],\"name\":\"send5SunToReceiver\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
    txid = PublicMethed.deployContractAndGetTransactionInfoById(contractName,abi,code,"",
        maxFeeLimit, 0L, 100,null,contract014Key,contract014Address,blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    contractAddress1 = infoById.get().getContractAddress().toByteArray();

    //Deploy contract2, contract2 has a function to call contract1 transaction sun function.
    // and has a revert function.
    code = "6080604052604051602080610263833981016040525160008054600160a060020a03909216600160a060020a031990921691909117905561021e806100456000396000f30060806040526004361061003d5763ffffffff60e060020a600035041663b3b638ab8114610042578063df5dd9c814610065578063ecb0b86214610086575b600080fd5b61006373ffffffffffffffffffffffffffffffffffffffff600435166100c4565b005b61006373ffffffffffffffffffffffffffffffffffffffff6004351661014e565b34801561009257600080fd5b5061009b6101d6565b6040805173ffffffffffffffffffffffffffffffffffffffff9092168252519081900360200190f35b60008054604080517f73656e643553756e546f526563656976657228616464726573732900000000008152815190819003601b01812063ffffffff60e060020a91829004908116909102825273ffffffffffffffffffffffffffffffffffffffff8681166004840152925192909316936024808301939192829003018183875af150505050600080fd5b60008054604080517f73656e643553756e546f526563656976657228616464726573732900000000008152815190819003601b01812063ffffffff60e060020a91829004908116909102825273ffffffffffffffffffffffffffffffffffffffff8681166004840152925192909316936024808301939192829003018183875af15050505050565b60005473ffffffffffffffffffffffffffffffffffffffff16815600a165627a7a7230582065632ad682ad1abe06031e0f1471af18b8caeaddc98c67de6765b9f01ce8aa320029";
    abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"_receiver\",\"type\":\"address\"}],\"name\":\"triggerContract1ButRevert\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_receiver\",\"type\":\"address\"}],\"name\":\"triggerContract1\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"payContract\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"name\":\"_add\",\"type\":\"address\"}],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
    String parame = "\"" +  Base58.encode58Check(contractAddress1) + "\"";
    contractName = "Contract2";

    txid = PublicMethed.deployContractWithConstantParame(contractName,abi,code,
        "constructor(address)", parame,"", maxFeeLimit,0L,100,null,
        contract014Key, contract014Address,blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    contractAddress2 = infoById.get().getContractAddress().toByteArray();

    //Deploy contract3, trigger contrct2 function.
    code = "60806040526040516020806101df833981016040525160008054600160a060020a03909216600160a060020a031990921691909117905561019a806100456000396000f30060806040526004361061004b5763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035041663025750698114610050578063ecb0b86214610073575b600080fd5b61007173ffffffffffffffffffffffffffffffffffffffff600435166100b1565b005b34801561007f57600080fd5b50610088610152565b6040805173ffffffffffffffffffffffffffffffffffffffff9092168252519081900360200190f35b60008054604080517f74726967676572436f6e747261637431286164647265737329000000000000008152815190819003601901812063ffffffff7c010000000000000000000000000000000000000000000000000000000091829004908116909102825273ffffffffffffffffffffffffffffffffffffffff8681166004840152925192909316936024808301939192829003018183875af15050505050565b60005473ffffffffffffffffffffffffffffffffffffffff16815600a165627a7a723058205a66bc83322abbfb01da52698e6f5a6b2ca2ff7c17793c1ff9db3a6c7e7f6cb10029";
    abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"_receiver\",\"type\":\"address\"}],\"name\":\"triggerContract2\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"payContract\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"name\":\"_add\",\"type\":\"address\"}],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
    parame = "\"" +  Base58.encode58Check(contractAddress2) + "\"";
    contractName = "Contract3";

    txid = PublicMethed.deployContractWithConstantParame(contractName,abi,code,
        "constructor(address)",parame,"", maxFeeLimit,0L,100,null,
        contract014Key,contract014Address,blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    contractAddress3 = infoById.get().getContractAddress().toByteArray();

    Assert.assertTrue(PublicMethed.sendcoin(contractAddress1,1000000L,fromAddress,testKey002,
        blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(receiverAddress,1000000L,fromAddress,testKey002,
        blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(contractAddress2,1000000L,fromAddress,testKey002,
        blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(contractAddress3,1000000L,fromAddress,testKey002,
        blockingStubFull));

    //Test contract2 trigger contract1 to test call function
    Account contract2AccountInfo = PublicMethed.queryAccount(contractAddress2,blockingStubFull);
    Long contract2BeforeBalance = contract2AccountInfo.getBalance();
    Account receiverAccountInfo = PublicMethed.queryAccount(receiverAddress,blockingStubFull);
    Long receiverBeforeBalance = receiverAccountInfo.getBalance();
    Account contract1AccountInfo = PublicMethed.queryAccount(contractAddress1,blockingStubFull);
    Long contract1BeforeBalance = contract1AccountInfo.getBalance();
    logger.info("before contract1 balance is " + Long.toString(contract1BeforeBalance));
    logger.info("before receiver balance is " + Long.toString(receiverBeforeBalance));
    String receiveAddress = "\"" +  Base58.encode58Check(receiverAddress) + "\"";
    txid = PublicMethed.triggerContract(contractAddress2,
        "triggerContract1(address)", receiveAddress, false,
        0, 10000000L, contract014Address, contract014Key, blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    contract2AccountInfo = PublicMethed.queryAccount(contractAddress2,blockingStubFull);
    Long contract2AfterBalance = contract2AccountInfo.getBalance();
    receiverAccountInfo = PublicMethed.queryAccount(receiverAddress,blockingStubFull);
    Long receiverAfterBalance = receiverAccountInfo.getBalance();
    contract1AccountInfo = PublicMethed.queryAccount(contractAddress1,blockingStubFull);
    Long contract1AfterBalance = contract1AccountInfo.getBalance();
    logger.info("after contract1 balance is " + Long.toString(contract1AfterBalance));
    Assert.assertTrue(receiverAfterBalance - receiverBeforeBalance == 5);
    Assert.assertTrue(contract2BeforeBalance - contract2AfterBalance == 0);
    Assert.assertTrue(contract1BeforeBalance - contract1AfterBalance == 5);

    //Test contract2 trigger contract1 but revert
    contract1AccountInfo = PublicMethed.queryAccount(contractAddress1,blockingStubFull);
    contract1BeforeBalance = contract1AccountInfo.getBalance();
    receiverAccountInfo = PublicMethed.queryAccount(receiverAddress,blockingStubFull);
    receiverBeforeBalance = receiverAccountInfo.getBalance();
    receiveAddress = "\"" +  Base58.encode58Check(receiverAddress) + "\"";
    txid = PublicMethed.triggerContract(contractAddress2,
        "triggerContract1ButRevert(address)", receiveAddress, false,
        0, 10000000L, contract014Address, contract014Key, blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 1);
    contract1AccountInfo = PublicMethed.queryAccount(contractAddress1,blockingStubFull);
    contract1AfterBalance = contract1AccountInfo.getBalance();
    receiverAccountInfo = PublicMethed.queryAccount(receiverAddress,blockingStubFull);
    receiverAfterBalance = receiverAccountInfo.getBalance();
    logger.info("after receiver balance is " + Long.toString(receiverAfterBalance));
    Assert.assertTrue(receiverAfterBalance - receiverBeforeBalance == 0);
    Assert.assertTrue(contract1BeforeBalance - contract1AfterBalance == 0);

    //Test contract3 trigger contract2 to call contract1
    contract1AccountInfo = PublicMethed.queryAccount(contractAddress1,blockingStubFull);
    contract1BeforeBalance = contract1AccountInfo.getBalance();
    Account contract3AccountInfo = PublicMethed.queryAccount(contractAddress3,blockingStubFull);
    Long contract3BeforeBalance = contract3AccountInfo.getBalance();
    receiverAccountInfo = PublicMethed.queryAccount(receiverAddress,blockingStubFull);
    receiverBeforeBalance = receiverAccountInfo.getBalance();
    logger.info("before receiver balance is " + Long.toString(receiverBeforeBalance));
    logger.info("before contract3 balance is " + Long.toString(contract3BeforeBalance));
    receiveAddress = "\"" +  Base58.encode58Check(receiverAddress) + "\"";
    txid = PublicMethed.triggerContract(contractAddress3,
        "triggerContract2(address)", receiveAddress, false,
        0, 10000000L, contract014Address, contract014Key, blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    contract3AccountInfo = PublicMethed.queryAccount(contractAddress3,blockingStubFull);
    Long contract3AfterBalance = contract3AccountInfo.getBalance();
    receiverAccountInfo = PublicMethed.queryAccount(receiverAddress,blockingStubFull);
    receiverAfterBalance = receiverAccountInfo.getBalance();
    logger.info("after receiver balance is " + Long.toString(receiverAfterBalance));
    logger.info("after contract3 balance is " + Long.toString(contract3AfterBalance));
    contract1AccountInfo = PublicMethed.queryAccount(contractAddress1,blockingStubFull);
    contract1AfterBalance = contract1AccountInfo.getBalance();

    Assert.assertTrue(receiverAfterBalance - receiverBeforeBalance == 5);
    Assert.assertTrue(contract3BeforeBalance - contract3AfterBalance == 0);
    Assert.assertTrue(contract1BeforeBalance - contract1AfterBalance == 5);



  }



  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


