package stest.tron.wallet.contract.linkage;

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
public class ContractLinkage007 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  String contractName;
  String code;
  String abi;
  byte[] contractAddress;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] linkage007Address = ecKey1.getAddress();
  String linkage007Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(linkage007Key);
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
  public void testRangeOfFeeLimit() {

    //Now the feelimit range is 0-1000000000,including 0 and 1000000000
    Assert.assertTrue(PublicMethed.sendcoin(linkage007Address, 2000000000L, fromAddress,
        testKey002, blockingStubFull));
    contractName = "testRangeOfFeeLimit";
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
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(linkage007Address,
        blockingStubFull);
    Account info;
    info = PublicMethed.queryAccount(linkage007Address, blockingStubFull);
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
    //When the feelimit is large, the deploy will be failed,No used everything.
    String txid;
    txid = PublicMethed.deployContractAndGetTransactionInfoById(contractName, abi, code,
        "", maxFeeLimit + 1, 0L, 100, null, linkage007Key,
        linkage007Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account infoafter = PublicMethed.queryAccount(linkage007Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(linkage007Address,
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
    Assert.assertEquals(beforeBalance, afterBalance);
    Assert.assertTrue(afterEnergyUsed == 0);
    Assert.assertTrue(afterNetUsed == 0);
    Assert.assertTrue(afterFreeNetUsed == 0);

    Assert.assertTrue(txid == null);
    AccountResourceMessage resourceInfo1 = PublicMethed.getAccountResource(linkage007Address,
        blockingStubFull);
    Account info1 = PublicMethed.queryAccount(linkage007Address, blockingStubFull);
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
    //When the feelimit is 0, the deploy will be failed.Only use FreeNet,balance not change.
    txid = PublicMethed.deployContractAndGetTransactionInfoById(contractName, abi, code,
        "", 0L, 0L, 100, null, linkage007Key,
        linkage007Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account infoafter1 = PublicMethed.queryAccount(linkage007Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter1 = PublicMethed.getAccountResource(linkage007Address,
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
    Assert.assertEquals(beforeBalance1, afterBalance1);
    Assert.assertTrue(afterFreeNetUsed1 > 0);
    Assert.assertTrue(afterNetUsed1 == 0);
    Assert.assertTrue(afterEnergyUsed1 == 0);
    Optional<TransactionInfo> infoById;

    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 1);

    //Deploy the contract.success.use FreeNet,EnergyFee.balcne change
    AccountResourceMessage resourceInfo2 = PublicMethed.getAccountResource(linkage007Address,
        blockingStubFull);
    Account info2 = PublicMethed.queryAccount(linkage007Address, blockingStubFull);
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
    txid = PublicMethed.deployContractAndGetTransactionInfoById(contractName, abi, code,
        "", maxFeeLimit, 0L, 100, null, linkage007Key,
        linkage007Address, blockingStubFull);
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
    Account infoafter2 = PublicMethed.queryAccount(linkage007Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter2 = PublicMethed.getAccountResource(linkage007Address,
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
    logger.info("---------------:");
    Assert.assertTrue((beforeBalance2 - fee2) == afterBalance2);
    Assert.assertTrue(afterEnergyUsed2 == 0);
    Assert.assertTrue(afterFreeNetUsed2 > beforeFreeNetUsed2);
    Assert.assertTrue(infoById2.get().getResultValue() == 0);
    contractAddress = infoById2.get().getContractAddress().toByteArray();

    //When the feelimit is large, the trigger will be failed.Only use FreeNetUsed,Balance not change
    AccountResourceMessage resourceInfo3 = PublicMethed.getAccountResource(linkage007Address,
        blockingStubFull);
    Account info3 = PublicMethed.queryAccount(linkage007Address, blockingStubFull);
    Long beforeBalance3 = info3.getBalance();
    Long beforeEnergyLimit3 = resourceInfo3.getEnergyLimit();
    Long beforeEnergyUsed3 = resourceInfo3.getEnergyUsed();
    Long beforeFreeNetLimit3 = resourceInfo3.getFreeNetLimit();
    Long beforeNetLimit3 = resourceInfo3.getNetLimit();
    Long beforeNetUsed3 = resourceInfo3.getNetUsed();
    Long beforeFreeNetUsed3 = resourceInfo3.getFreeNetUsed();
    logger.info("beforeBalance3:" + beforeBalance3);
    logger.info("beforeEnergyLimit3:" + beforeEnergyLimit3);
    logger.info("beforeEnergyUsed3:" + beforeEnergyUsed3);
    logger.info("beforeFreeNetLimit3:" + beforeFreeNetLimit3);
    logger.info("beforeNetLimit3:" + beforeNetLimit3);
    logger.info("beforeNetUsed3:" + beforeNetUsed3);
    logger.info("beforeFreeNetUsed3:" + beforeFreeNetUsed3);
    String initParmes = "\"" + Base58.encode58Check(fromAddress) + "\",\"63\"";
    txid = PublicMethed.triggerContract(contractAddress,
        "init(address,uint256)", initParmes, false,
        1000, maxFeeLimit + 1, linkage007Address, linkage007Key, blockingStubFull);
    Account infoafter3 = PublicMethed.queryAccount(linkage007Address, blockingStubFull1);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    AccountResourceMessage resourceInfoafter3 = PublicMethed.getAccountResource(linkage007Address,
        blockingStubFull1);
    Long afterBalance3 = infoafter3.getBalance();
    Long afterEnergyLimit3 = resourceInfoafter3.getEnergyLimit();
    Long afterEnergyUsed3 = resourceInfoafter3.getEnergyUsed();
    Long afterFreeNetLimit3 = resourceInfoafter3.getFreeNetLimit();
    Long afterNetLimit3 = resourceInfoafter3.getNetLimit();
    Long afterNetUsed3 = resourceInfoafter3.getNetUsed();
    Long afterFreeNetUsed3 = resourceInfoafter3.getFreeNetUsed();
    logger.info("afterBalance3:" + afterBalance3);
    logger.info("afterEnergyLimit3:" + afterEnergyLimit3);
    logger.info("afterEnergyUsed3:" + afterEnergyUsed3);
    logger.info("afterFreeNetLimit3:" + afterFreeNetLimit3);
    logger.info("afterNetLimit3:" + afterNetLimit3);
    logger.info("afterNetUsed3:" + afterNetUsed3);
    logger.info("afterFreeNetUsed3:" + afterFreeNetUsed3);
    logger.info("---------------:");
    Assert.assertTrue(txid == null);
    Assert.assertEquals(beforeBalance3, afterBalance3);
    Assert.assertTrue(afterFreeNetUsed3 > beforeNetUsed3);
    Assert.assertTrue(afterNetUsed3 == 0);
    Assert.assertTrue(afterEnergyUsed3 == 0);
    //When the feelimit is 0, the trigger will be failed.Only use FreeNetUsed,Balance not change
    AccountResourceMessage resourceInfo4 = PublicMethed.getAccountResource(linkage007Address,
        blockingStubFull);
    Account info4 = PublicMethed.queryAccount(linkage007Address, blockingStubFull);
    Long beforeBalance4 = info4.getBalance();
    Long beforeEnergyLimit4 = resourceInfo4.getEnergyLimit();
    Long beforeEnergyUsed4 = resourceInfo4.getEnergyUsed();
    Long beforeFreeNetLimit4 = resourceInfo4.getFreeNetLimit();
    Long beforeNetLimit4 = resourceInfo4.getNetLimit();
    Long beforeNetUsed4 = resourceInfo4.getNetUsed();
    Long beforeFreeNetUsed4 = resourceInfo4.getFreeNetUsed();
    logger.info("beforeBalance4:" + beforeBalance4);
    logger.info("beforeEnergyLimit4:" + beforeEnergyLimit4);
    logger.info("beforeEnergyUsed4:" + beforeEnergyUsed4);
    logger.info("beforeFreeNetLimit4:" + beforeFreeNetLimit4);
    logger.info("beforeNetLimit4:" + beforeNetLimit4);
    logger.info("beforeNetUsed4:" + beforeNetUsed4);
    logger.info("beforeFreeNetUsed4:" + beforeFreeNetUsed4);
    PublicMethed.triggerContract(contractAddress,
        "init(address,uint256)", initParmes, false,
        1000, 0, linkage007Address, linkage007Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account infoafter4 = PublicMethed.queryAccount(linkage007Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter4 = PublicMethed.getAccountResource(linkage007Address,
        blockingStubFull1);
    Long afterBalance4 = infoafter4.getBalance();
    Long afterEnergyLimit4 = resourceInfoafter4.getEnergyLimit();
    Long afterEnergyUsed4 = resourceInfoafter4.getEnergyUsed();
    Long afterFreeNetLimit4 = resourceInfoafter4.getFreeNetLimit();
    Long afterNetLimit4 = resourceInfoafter4.getNetLimit();
    Long afterNetUsed4 = resourceInfoafter4.getNetUsed();
    Long afterFreeNetUsed4 = resourceInfoafter4.getFreeNetUsed();
    logger.info("afterBalance4:" + afterBalance4);
    logger.info("afterEnergyLimit4:" + afterEnergyLimit4);
    logger.info("afterEnergyUsed4:" + afterEnergyUsed4);
    logger.info("afterFreeNetLimit4:" + afterFreeNetLimit4);
    logger.info("afterNetLimit4:" + afterNetLimit4);
    logger.info("afterNetUsed4:" + afterNetUsed4);
    logger.info("afterFreeNetUsed4:" + afterFreeNetUsed4);
    logger.info("---------------:");
    Assert.assertEquals(beforeBalance4, afterBalance4);
    Assert.assertTrue(afterFreeNetUsed4 > beforeNetUsed4);
    Assert.assertTrue(afterNetUsed4 == 0);
    Assert.assertTrue(afterEnergyUsed4 == 0);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info(Integer.toString(infoById.get().getResultValue()));
    Assert.assertTrue(infoById.get().getFee() == 0);
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}


