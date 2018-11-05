package stest.tron.wallet.manual;

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
public class ContractLinkage006 {

  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey003);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  String contractName;
  String code;
  String abi;
  byte[] contractAddress;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] linkage006Address = ecKey1.getAddress();
  String linkage006Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] linkage006Address2 = ecKey2.getAddress();
  String linkage006Key2 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(linkage006Key);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
  }

  @Test(enabled = true)
  public void teststackOutByContract() {

    Assert.assertTrue(PublicMethed.sendcoin(linkage006Address, 20000000000L, fromAddress,
        testKey003, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalance(linkage006Address, 1000000L,
        3, linkage006Key, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(linkage006Address, 1000000L,
        3, 1, linkage006Key, blockingStubFull));
    contractName = "stackOutByContract";
    code = "60806040526000805561026c806100176000396000f3006080604052600436106100565763ffffffff7c01"
        + "0000000000000000000000000000000000000000000000000000000060003504166306661abd811461005b5"
        + "780631548567714610082578063399ae724146100a8575b600080fd5b34801561006757600080fd5b506100"
        + "706100cc565b60408051918252519081900360200190f35b6100a673fffffffffffffffffffffffffffffff"
        + "fffffffff600435166024356100d2565b005b6100a673ffffffffffffffffffffffffffffffffffffffff60"
        + "0435166024356101af565b60005481565b80600054101561017257600080546001018155604080517f15485"
        + "67700000000000000000000000000000000000000000000000000000000815273ffffffffffffffffffffff"
        + "ffffffffffffffffff851660048201526024810184905290513092631548567792604480820193918290030"
        + "1818387803b15801561015557600080fd5b505af1158015610169573d6000803e3d6000fd5b505050506100"
        + "d2565b8060005414156101ab5760405173ffffffffffffffffffffffffffffffffffffffff8316906000906"
        + "0149082818181858883f150505050505b5050565b6000808055604080517f15485677000000000000000000"
        + "00000000000000000000000000000000000000815273ffffffffffffffffffffffffffffffffffffffff851"
        + "6600482015260248101849052905130926315485677926044808201939182900301818387803b1580156102"
        + "2457600080fd5b505af1158015610238573d6000803e3d6000fd5b5050505050505600a165627a7a7230582"
        + "0ecdc49ccf0dea5969829debf8845e77be6334f348e9dcaeabf7e98f2d6c7f5270029";
    abi = "[{\"constant\":true,\"inputs\":[],\"name\":\"count\",\"outputs\":[{\"name\":\"\",\"type"
        + "\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},"
        + "{\"constant\":false,\"inputs\":[{\"name\":\"addr\",\"type\":\"address\"},{\"name\":\""
        + "max\",\"type\":\"uint256\"}],\"name\":\"hack\",\"outputs\":[],\"payable\":true,\""
        + "stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{"
        + "\"name\":\"addr\",\"type\":\"address\"},{\"name\":\"max\",\"type\":\"uint256\"}],\""
        + "name\":\"init\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type"
        + "\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type"
        + "\":\"constructor\"}]";
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(linkage006Address,
        blockingStubFull);
    Account info;
    info = PublicMethed.queryAccount(linkage006Address, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyLimit = resourceInfo.getEnergyLimit();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeFreeNetLimit = resourceInfo.getFreeNetLimit();
    Long beforeNetLimit = resourceInfo.getNetLimit();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyLimit:" + beforeEnergyLimit);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeFreeNetLimit:" + beforeFreeNetLimit);
    logger.info("beforeNetLimit:" + beforeNetLimit);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    //success ,balnace change.use EnergyUsed and NetUsed
    String txid = PublicMethed.deployContractAndGetTransactionInfoById(contractName, abi, code,
        "", maxFeeLimit, 1000L, 100, null, linkage006Key,
        linkage006Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    Long fee = infoById.get().getFee();
    Long energyFee = infoById.get().getReceipt().getEnergyFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    logger.info("energyUsageTotal:" + energyUsageTotal);
    logger.info("fee:" + fee);
    logger.info("energyFee:" + energyFee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    Account infoafter = PublicMethed.queryAccount(linkage006Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(linkage006Address,
        blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyLimit = resourceInfoafter.getEnergyLimit();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterFreeNetLimit = resourceInfoafter.getFreeNetLimit();
    Long afterNetLimit = resourceInfoafter.getNetLimit();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyLimit:" + afterEnergyLimit);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterFreeNetLimit:" + afterFreeNetLimit);
    logger.info("afterNetLimit:" + afterNetLimit);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue((beforeBalance - fee - 1000L) == afterBalance);
    Assert.assertTrue((beforeNetUsed + netUsed) >= afterNetUsed);
    Assert.assertTrue((beforeEnergyUsed + energyUsed) >= afterEnergyUsed);
    Assert.assertTrue(PublicMethed.sendcoin(linkage006Address2, 20000000000L, fromAddress,
        testKey003, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalance(linkage006Address2, 1000000L,
        3, linkage006Key2, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(linkage006Address2, 1000000L,
        3, 1, linkage006Key2, blockingStubFull));
    contractAddress = infoById.get().getContractAddress().toByteArray();
    AccountResourceMessage resourceInfo1 = PublicMethed.getAccountResource(linkage006Address2,
        blockingStubFull);
    Account info1 = PublicMethed.queryAccount(linkage006Address2, blockingStubFull);
    Long beforeBalance1 = info1.getBalance();
    Long beforeEnergyLimit1 = resourceInfo1.getEnergyLimit();
    Long beforeEnergyUsed1 = resourceInfo1.getEnergyUsed();
    Long beforeFreeNetLimit1 = resourceInfo1.getFreeNetLimit();
    Long beforeNetLimit1 = resourceInfo1.getNetLimit();
    Long beforeNetUsed1 = resourceInfo1.getNetUsed();
    Long beforeFreeNetUsed1 = resourceInfo1.getFreeNetUsed();
    logger.info("beforeBalance1:" + beforeBalance1);
    logger.info("beforeEnergyLimit1:" + beforeEnergyLimit1);
    logger.info("beforeEnergyUsed1:" + beforeEnergyUsed1);
    logger.info("beforeFreeNetLimit1:" + beforeFreeNetLimit1);
    logger.info("beforeNetLimit1:" + beforeNetLimit1);
    logger.info("beforeNetUsed1:" + beforeNetUsed1);
    logger.info("beforeFreeNetUsed1:" + beforeFreeNetUsed1);

    //success ,balance change.use EnergyUsed and NetUsed
    String initParmes = "\"" + Base58.encode58Check(fromAddress) + "\",\"63\"";
    txid = PublicMethed.triggerContract(contractAddress,
        "init(address,uint256)", initParmes, false,
        0, 100000000L, linkage006Address2, linkage006Key2, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById1 = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Long energyUsageTotal1 = infoById1.get().getReceipt().getEnergyUsageTotal();
    Long fee1 = infoById1.get().getFee();
    Long energyFee1 = infoById1.get().getReceipt().getEnergyFee();
    Long netUsed1 = infoById1.get().getReceipt().getNetUsage();
    Long energyUsed1 = infoById1.get().getReceipt().getEnergyUsage();
    Long netFee1 = infoById1.get().getReceipt().getNetFee();
    logger.info("energyUsageTotal1:" + energyUsageTotal1);
    logger.info("fee1:" + fee1);
    logger.info("energyFee1:" + energyFee1);
    logger.info("netUsed1:" + netUsed1);
    logger.info("energyUsed1:" + energyUsed1);
    logger.info("netFee1:" + netFee1);
    Account infoafter1 = PublicMethed.queryAccount(linkage006Address2, blockingStubFull1);
    AccountResourceMessage resourceInfoafter1 = PublicMethed.getAccountResource(linkage006Address2,
        blockingStubFull1);
    Long afterBalance1 = infoafter1.getBalance();
    Long afterEnergyLimit1 = resourceInfoafter1.getEnergyLimit();
    Long afterEnergyUsed1 = resourceInfoafter1.getEnergyUsed();
    Long afterFreeNetLimit1 = resourceInfoafter1.getFreeNetLimit();
    Long afterNetLimit1 = resourceInfoafter1.getNetLimit();
    Long afterNetUsed1 = resourceInfoafter1.getNetUsed();
    Long afterFreeNetUsed1 = resourceInfoafter1.getFreeNetUsed();
    logger.info("afterBalance1:" + afterBalance1);
    logger.info("afterEnergyLimit1:" + afterEnergyLimit1);
    logger.info("afterEnergyUsed1:" + afterEnergyUsed1);
    logger.info("afterFreeNetLimit1:" + afterFreeNetLimit1);
    logger.info("afterNetLimit1:" + afterNetLimit1);
    logger.info("afterNetUsed1:" + afterNetUsed1);
    logger.info("afterFreeNetUsed1:" + afterFreeNetUsed1);
    logger.info("---------------:");
    Assert.assertTrue((beforeBalance1 - fee1) == afterBalance1);
    Assert.assertTrue(afterNetUsed1 > beforeNetUsed1);
    Assert.assertTrue((beforeEnergyUsed1 + energyUsed1) >= afterEnergyUsed1);

    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    initParmes = "\"" + Base58.encode58Check(fromAddress) + "\",\"64\"";
    AccountResourceMessage resourceInfo2 = PublicMethed.getAccountResource(linkage006Address2,
        blockingStubFull);
    Account info2 = PublicMethed.queryAccount(linkage006Address2, blockingStubFull);
    Long beforeBalance2 = info2.getBalance();
    Long beforeEnergyLimit2 = resourceInfo2.getEnergyLimit();
    Long beforeEnergyUsed2 = resourceInfo2.getEnergyUsed();
    Long beforeFreeNetLimit2 = resourceInfo2.getFreeNetLimit();
    Long beforeNetLimit2 = resourceInfo2.getNetLimit();
    Long beforeNetUsed2 = resourceInfo2.getNetUsed();
    Long beforeFreeNetUsed2 = resourceInfo2.getFreeNetUsed();
    logger.info("beforeBalance2:" + beforeBalance2);
    logger.info("beforeEnergyLimit2:" + beforeEnergyLimit2);
    logger.info("beforeEnergyUsed2:" + beforeEnergyUsed2);
    logger.info("beforeFreeNetLimit2:" + beforeFreeNetLimit2);
    logger.info("beforeNetLimit2:" + beforeNetLimit2);
    logger.info("beforeNetUsed2:" + beforeNetUsed2);
    logger.info("beforeFreeNetUsed2:" + beforeFreeNetUsed2);
    //failed ,use EnergyUsed and NetUsed
    txid = PublicMethed.triggerContract(contractAddress,
        "init(address,uint256)", initParmes, false,
        1000, 100000000L, linkage006Address2, linkage006Key2, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById2 = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Long energyUsageTotal2 = infoById2.get().getReceipt().getEnergyUsageTotal();
    Long fee2 = infoById2.get().getFee();
    Long energyFee2 = infoById2.get().getReceipt().getEnergyFee();
    Long netUsed2 = infoById2.get().getReceipt().getNetUsage();
    Long energyUsed2 = infoById2.get().getReceipt().getEnergyUsage();
    Long netFee2 = infoById2.get().getReceipt().getNetFee();
    logger.info("energyUsageTotal2:" + energyUsageTotal2);
    logger.info("fee2:" + fee2);
    logger.info("energyFee2:" + energyFee2);
    logger.info("netUsed2:" + netUsed2);
    logger.info("energyUsed2:" + energyUsed2);
    logger.info("netFee2:" + netFee2);

    Account infoafter2 = PublicMethed.queryAccount(linkage006Address2, blockingStubFull1);
    AccountResourceMessage resourceInfoafter2 = PublicMethed.getAccountResource(linkage006Address2,
        blockingStubFull1);
    Long afterBalance2 = infoafter2.getBalance();
    Long afterEnergyLimit2 = resourceInfoafter2.getEnergyLimit();
    Long afterEnergyUsed2 = resourceInfoafter2.getEnergyUsed();
    Long afterFreeNetLimit2 = resourceInfoafter2.getFreeNetLimit();
    Long afterNetLimit2 = resourceInfoafter2.getNetLimit();
    Long afterNetUsed2 = resourceInfoafter2.getNetUsed();
    Long afterFreeNetUsed2 = resourceInfoafter2.getFreeNetUsed();
    logger.info("afterBalance2:" + afterBalance2);
    logger.info("afterEnergyLimit2:" + afterEnergyLimit2);
    logger.info("afterEnergyUsed2:" + afterEnergyUsed2);
    logger.info("afterFreeNetLimit2:" + afterFreeNetLimit2);
    logger.info("afterNetLimit2:" + afterNetLimit2);
    logger.info("afterNetUsed2:" + afterNetUsed2);
    logger.info("afterFreeNetUsed2:" + afterFreeNetUsed2);

    Assert.assertTrue((beforeBalance2 - fee2) == afterBalance2);
    Assert.assertTrue(afterNetUsed2 > beforeNetUsed2);
    Assert.assertTrue((beforeEnergyUsed2 + energyUsed2) >= afterEnergyUsed2);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 1);
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}


