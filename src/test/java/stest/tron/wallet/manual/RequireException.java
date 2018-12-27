package stest.tron.wallet.manual;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
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
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class RequireException {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelSolidity = null;

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;

  private ManagedChannel channelFull2 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull2 = null;


  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;

  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);

  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

  byte[] contractAddress = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] asset016Address = ecKey1.getAddress();
  String testKeyForAssetIssue016 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(testKeyForAssetIssue016);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
    logger.info(Long.toString(PublicMethed.queryAccount(testNetAccountKey, blockingStubFull)
        .getBalance()));
    PublicMethed
        .sendcoin(asset016Address, 1000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);

  }

  @Test(enabled = true)
  public void testTestRequireContract() {
    String contractName = "TestRequireContract";
    String code = "608060405234801561001057600080fd5b5060b58061001f6000396000f30060806040526004361"
        + "0605c5763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416"
        + "632b813bc081146061578063357815c414607557806350bff6bf146075578063a26388bb146075575b60008"
        + "0fd5b348015606c57600080fd5b5060736087565b005b348015608057600080fd5b506073605c565bfe00a1"
        + "65627a7a723058209284d2c51e121903dde36db88dae131b1b20dc83b987a6f491dcac2d9b2d30db0029";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"testAssert\",\"outputs\":[],\""
        + "payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant"
        + "\":false,\"inputs\":[],\"name\":\"testRequire\",\"outputs\":[],\"payable\":false,\""
        + "stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs"
        + "\":[],\"name\":\"testThrow\",\"outputs\":[],\"payable\":false,\"stateMutability\":\""
        + "nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\""
        + "testRevert\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\""
        + "type\":\"function\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForAssetIssue016,
        asset016Address, blockingStubFull);
    Account info;
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull);
    info = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();

    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    String txid = "";

    txid = PublicMethed.triggerContract(contractAddress,
        "testRequire()", "#", false,
        0, maxFeeLimit, asset016Address, testKeyForAssetIssue016, blockingStubFull);
    Optional<TransactionInfo> infoById = null;

    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);

    Account infoafter = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();

    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);

  }

  @Test(enabled = true)
  public void testTestThrowsContract() {
    String contractName = "TestThrowsContract";
    String code = "608060405234801561001057600080fd5b5060b58061001f6000396000f30060806040526004361"
        + "0605c5763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416"
        + "632b813bc081146061578063357815c414607557806350bff6bf146075578063a26388bb146075575b60008"
        + "0fd5b348015606c57600080fd5b5060736087565b005b348015608057600080fd5b506073605c565bfe00a1"
        + "65627a7a723058209284d2c51e121903dde36db88dae131b1b20dc83b987a6f491dcac2d9b2d30db0029";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"testAssert\",\"outputs\":[],\""
        + "payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant"
        + "\":false,\"inputs\":[],\"name\":\"testRequire\",\"outputs\":[],\"payable\":false,\""
        + "stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs"
        + "\":[],\"name\":\"testThrow\",\"outputs\":[],\"payable\":false,\"stateMutability\":\""
        + "nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\""
        + "testRevert\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\","
        + "\"type\":\"function\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForAssetIssue016,
        asset016Address, blockingStubFull);
    Account info;
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull);
    info = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();

    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    String txid = "";
    txid = PublicMethed.triggerContract(contractAddress,
        "testThrow()", "#", false,
        0, maxFeeLimit, asset016Address, testKeyForAssetIssue016, blockingStubFull);
    Optional<TransactionInfo> infoById;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);

    Account infoafter = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();

    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
  }


  @Test(enabled = true)
  public void testTestRevertContract() {
    String contractName = "TestThrowsContract";
    String code = "608060405234801561001057600080fd5b5060b58061001f6000396000f3006080604052600436"
        + "10605c5763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504"
        + "16632b813bc081146061578063357815c414607557806350bff6bf146075578063a26388bb146075575b60"
        + "0080fd5b348015606c57600080fd5b5060736087565b005b348015608057600080fd5b506073605c565bfe"
        + "00a165627a7a723058209284d2c51e121903dde36db88dae131b1b20dc83b987a6f491dcac2d9b2d30db0"
        + "029";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"testAssert\",\"outputs\":[],\""
        + "payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant"
        + "\":false,\"inputs\":[],\"name\":\"testRequire\",\"outputs\":[],\"payable\":false,\""
        + "stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\""
        + ":[],\"name\":\"testThrow\",\"outputs\":[],\"payable\":false,\"stateMutability\":\""
        + "nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\""
        + "testRevert\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\","
        + "\"type\":\"function\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForAssetIssue016,
        asset016Address, blockingStubFull);
    Account info;
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull);
    info = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();

    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    String txid = "";
    txid = PublicMethed.triggerContract(contractAddress,
        "testRevert()", "#", false,
        0, maxFeeLimit, asset016Address, testKeyForAssetIssue016, blockingStubFull);
    Optional<TransactionInfo> infoById = null;

    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);

    Account infoafter = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();

    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);

  }

  @Test(enabled = true)
  public void testnoPayableContract() {
    String contractName = "noPayableContract";
    String code = "6080604052348015600f57600080fd5b5060978061001e6000396000f3006080604052600436106"
        + "03e5763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035041663"
        + "2380bf0581146043575b600080fd5b348015604e57600080fd5b5060556067565b604080519182525190819"
        + "00360200190f35b34905600a165627a7a72305820c15441923f769bff9193db8304db516b768651d3eb0861"
        + "a38163b3e7e6174ee50029";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"noPayable\",\"outputs\":[{\"name"
        + "\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\","
        + "\"type\":\"function\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForAssetIssue016,
        asset016Address, blockingStubFull);
    Account info;

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull);
    info = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();

    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    String txid = "";
    txid = PublicMethed.triggerContract(contractAddress,
        "noPayable()", "#", false,
        22, maxFeeLimit, asset016Address, testKeyForAssetIssue016, blockingStubFull);
    Optional<TransactionInfo> infoById = null;

    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    Account infoafter = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();

    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);

  }

  @Test(enabled = true)
  public void testnoPayableConstructor() {

    Account info;
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull);
    info = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();

    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    String txid = "";
    String contractName = "noPayableConstructor";
    String code = "6080604052348015600f57600080fd5b506040516020806071833981016040525134811115602c5"
        + "7600080fd5b600055603580603c6000396000f3006080604052600080fd00a165627a7a72305820cb20f649"
        + "31c41844749c1571bfc4dfdd268a58ed29b7446dd817ce3c54b014150029";
    String abi = "[{\"inputs\":[{\"name\":\"_money\",\"type\":\"uint256\"}],\"payable\":false,\""
        + "stateMutability\":\"nonpayable\",\"type\":\"constructor\"}]";
    txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            22L, 100, null,
            testKeyForAssetIssue016, asset016Address, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);

    Account infoafter = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();

    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);


  }

  @Test(enabled = true)
  public void transferTestContract() {
    String contractName = "transferTestContract";
    String code = "608060405234801561001057600080fd5b5060d28061001f6000396000f30060806040526004361"
        + "0603e5763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416"
        + "6319b357c581146043575b600080fd5b606273ffffffffffffffffffffffffffffffffffffffff600435166"
        + "064565b005b60405173ffffffffffffffffffffffffffffffffffffffff821690600090600a908281818185"
        + "8883f1935050505015801560a2573d6000803e3d6000fd5b50505600a165627a7a7230582078c54bf20a44a"
        + "5fecf3e03a7e6daf7b712dc71db7ec24840549f1655c55123760029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"addr\",\"type\":\"address\"}],\""
        + "name\":\"tranferTest\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable"
        + "\",\"type\":\"function\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForAssetIssue016,
        asset016Address, blockingStubFull);
    Account info;
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull);
    info = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();

    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    String newCxoAddress = "\"" + Base58.encode58Check(testNetAccountAddress)
        + "\"";
    String txid = "";
    txid = PublicMethed.triggerContract(contractAddress,
        "tranferTest(address) ", newCxoAddress, false,
        5, maxFeeLimit, asset016Address, testKeyForAssetIssue016, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;

    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);

    Account infoafter = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();

    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);

  }

  @Test(enabled = true)
  public void testpayableFallbakContract() {
    String contractName = "payableFallbak";
    String code = "6080604052348015600f57600080fd5b5060fb8061001e6000396000f3006080604052600436106"
        + "03e5763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035041663"
        + "32eb12f181146043575b600080fd5b348015604e57600080fd5b50606e73fffffffffffffffffffffffffff"
        + "fffffffffffff600435166070565b005b8073ffffffffffffffffffffffffffffffffffffffff1663abcdef"
        + "016040518163ffffffff167c010000000000000000000000000000000000000000000000000000000002815"
        + "26004016000604051808303816000875af150505050505600a165627a7a723058202930f27ada1f076de1a8"
        + "57ded5f8b46ef335b465a8edae1b487947d3d7dedcc30029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"test\",\"type\":\"address\"}],\""
        + "name\":\"callTest\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable"
        + "\",\"type\":\"function\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForAssetIssue016,
        asset016Address, blockingStubFull);
    Integer times = 0;
    String contractName1 = "TestContract";
    String code1 = "6080604052348015600f57600080fd5b50604380601d6000396000f3006080604052348015600f"
        + "57600080fd5b5060016000550000a165627a7a723058205718ecb0cace0afa330fc9447eff8556c5829aeb8"
        + "256c62364aaf58efa5bd96c0029";
    String abi1 = "[{\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"fallback\"}]";
    byte[] contractAddress1;
    contractAddress1 = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit, 0L,
            100, null, testKeyForAssetIssue016,
            asset016Address, blockingStubFull);
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull);
    Account info;
    info = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();

    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    String saleContractString = "\"" + Base58.encode58Check(contractAddress) + "\"";
    String txid = "";
    txid = PublicMethed.triggerContract(contractAddress1,
        "callTest(address)", saleContractString, false,
        5, maxFeeLimit, asset016Address, testKeyForAssetIssue016, blockingStubFull);
    Optional<TransactionInfo> infoById;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);

    Account infoafter = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();

    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);

  }

  @Test(enabled = true)
  public void testnewContractGasNoenough() {
    String contractName = "ContractGasNoenough";
    String code = "60806040526040516020806100bf83398101604052516000556099806100266000396000f300608"
        + "060405260043610603e5763ffffffff7c010000000000000000000000000000000000000000000000000000"
        + "000060003504166396964a2281146043575b600080fd5b348015604e57600080fd5b5060556067565b60408"
        + "051918252519081900360200190f35b600054815600a165627a7a72305820cb66b6d0ad40d2f5906f6a159f"
        + "47bc1a0c7b36676df34078edad0671caabd2370029";
    String abi = "[{\"constant\":true,\"inputs\":[],\"name\":\"accId\",\"outputs\":[{\"name"
        + "\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type"
        + "\":\"function\"},{\"inputs\":[{\"name\":\"accountId\",\"type\":\"uint256\"}],\"payable"
        + "\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForAssetIssue016,
        asset016Address, blockingStubFull);

    String txid = "";
    String contractName1 = "ContractGasNoenough";
    String code1 = "608060405234801561001057600080fd5b50610182806100206000396000f30060806040526004"
        + "3610603e5763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350"
        + "41663bf335e6281146043575b600080fd5b348015604e57600080fd5b5060556057565b005b600060016061"
        + "6088565b90815260405190819003602001906000f0801580156083573d6000803e3d6000fd5b505050565b6"
        + "0405160bf8061009883390190560060806040526040516020806100bf833981016040525160005560998061"
        + "00266000396000f300608060405260043610603e5763ffffffff7c010000000000000000000000000000000"
        + "000000000000000000000000060003504166396964a2281146043575b600080fd5b348015604e57600080fd"
        + "5b5060556067565b60408051918252519081900360200190f35b600054815600a165627a7a72305820cb66b"
        + "6d0ad40d2f5906f6a159f47bc1a0c7b36676df34078edad0671caabd2370029a165627a7a72305820b664e1"
        + "3ade4d346e9d9b848e75b8ded8d382d2ecacd77561561c3d6b189ad13f0029";
    String abi1 = "[{\"constant\":false,\"inputs\":[],\"name\":\"newAccount\",\"outputs\":[],\""
        + "payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";

    byte[] contractAddress1 = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            0L, 100, null,
            testKeyForAssetIssue016, asset016Address, blockingStubFull);
    Account info;
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull);
    info = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();

    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    txid = PublicMethed.triggerContract(contractAddress1,
        "newAccount()", "#", false,
        0, 5226000, asset016Address, testKeyForAssetIssue016, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);

    Account infoafter = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();

    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);


  }

  @Test(enabled = true)
  public void testMessageUsedErrorFeed() {
    String contractName = "MessageFeed";
    String code = "6080604052348015600f57600080fd5b50609c8061001e6000396000f300608060405260043610"
        + "603e5763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416"
        + "639138fd4c81146043575b600080fd5b348015604e57600080fd5b5060556067565b604080519182525190"
        + "81900360200190f35b60006001818082fe00a165627a7a72305820ebb23e69381b99dba3118ec3b715d619"
        + "ca395ecfd88820f7e5338579530fb54f0029";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"divideMathed\",\"outputs\":"
        + "[{\"name\":\"ret\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\""
        + "nonpayable\",\"type\":\"function\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForAssetIssue016,
        asset016Address, blockingStubFull);

    String saleContractString = "\"" + Base58.encode58Check(contractAddress) + "\"";
    String txid = "";
    String contractName1 = "MessageUseContract";
    String code1 = "608060405234801561001057600080fd5b50610149806100206000396000f30060806040526004"
        + "36106100405763ffffffff7c010000000000000000000000000000000000000000000000000000000060003"
        + "5041663ff04eb898114610045575b600080fd5b34801561005157600080fd5b5061007373ffffffffffffff"
        + "ffffffffffffffffffffffffff60043516610085565b60408051918252519081900360200190f35b600081"
        + "73ffffffffffffffffffffffffffffffffffffffff16639138fd4c6040518163ffffffff167c0100000000"
        + "000000000000000000000000000000000000000000000000028152600401602060405180830381600087803"
        + "b1580156100eb57600080fd5b505af11580156100ff573d6000803e3d6000fd5b505050506040513d602081"
        + "101561011557600080fd5b5051929150505600a165627a7a72305820733f086bcd980c277618750fe75cdd8"
        + "c6faf8f0b01de9521ac9531e8ec3589030029";
    String abi1 = "[{\"constant\":false,\"inputs\":[{\"name\":\"addr\",\"type\":\"address\"}]"
        + ",\"name\":\"MathedUse\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable"
        + "\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";

    byte[] contractAddress1 = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            0L, 100, null,
            testKeyForAssetIssue016, asset016Address, blockingStubFull);
    Account info;
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull);
    info = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();

    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    txid = PublicMethed.triggerContract(contractAddress1,
        "messageUse(address)", saleContractString, false,
        0, maxFeeLimit, asset016Address, testKeyForAssetIssue016, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);

    Account infoafter = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();

    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);

  }

  @Test(enabled = true)
  public void testFunctionUsedErrorFeed() {
    String contractName = "FunctionFeed";
    String code = "6080604052348015600f57600080fd5b50608b8061001e6000396000f3006080604052600436106"
        + "03e5763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035041663"
        + "19e1aef981146043575b600080fd5b6049605b565b60408051918252519081900360200190f35b34905600a"
        + "165627a7a7230582058a5ea8675d6e5710e5b539601c5567f746c197c90a6d6c2fa4626bfd6c107b30029";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"mValue\",\"outputs\":[{\"name\":"
        + "\"ret\",\"type\":\"uint256\"}],\"payable\":true,\"stateMutability\":\"payable\",\""
        + "type\":\"function\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForAssetIssue016,
        asset016Address, blockingStubFull);

    String txid = "";
    String saleContractString = "\"" + Base58.encode58Check(contractAddress) + "\"";

    String contractName1 = "FunctionUseContract";
    String code1 = "608060405234801561001057600080fd5b5061013f806100206000396000f3006080604052600"
        + "436106100405763ffffffff7c0100000000000000000000000000000000000000000000000000000000600"
        + "0350416637b77267a8114610045575b600080fd5b61006673fffffffffffffffffffffffffffffffffffff"
        + "fff60043516610078565b60408051918252519081900360200190f35b60008173fffffffffffffffffffff"
        + "fffffffffffffffffff166319e1aef960016040518263ffffffff167c01000000000000000000000000000"
        + "00000000000000000000000000000028152600401602060405180830381600088803b1580156100e057600"
        + "080fd5b5087f11580156100f4573d6000803e3d6000fd5b50505050506040513d602081101561010b57600"
        + "080fd5b5051929150505600a165627a7a723058207dcdb1b3c42bfc00674f226c36b1bbf2ee54a6a6ae6f"
        + "2eeace876b0370d83f5b0029";
    String abi1 = "[{\"constant\":false,\"inputs\":[{\"name\":\"addr\",\"type\":\"address\"}],"
        + "\"name\":\"messageUse\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable"
        + "\":true,\"stateMutability\":\"payable\",\"type\":\"function\"}]";
    byte[] contractAddress1 = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit, 0L,
            100, null, testKeyForAssetIssue016,
            asset016Address, blockingStubFull);
    Account info;
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull);
    info = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();

    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    txid = PublicMethed.triggerContract(contractAddress1,
        "messageUse(address)", saleContractString, false,
        0, maxFeeLimit, asset016Address, testKeyForAssetIssue016, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;

    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);

    Account infoafter = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();

    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);


  }
}
