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
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractLinkage005 {

  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey003);

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
  Long zeroForCycleCost;
  Long firstForCycleCost;
  Long secondForCycleCost;
  Long thirdForCycleCost;
  Long forthForCycleCost;
  Long fifthForCycleCost;
  Long zeroForCycleTimes = 498L;
  Long firstForCycleTimes = 500L;
  Long secondForCycleTimes = 502L;
  Long thirdForCycleTimes = 504L;
  Long forthForCycleTimes = 506L;
  Long fifthForCycleTimes = 508L;
  byte[] contractAddress;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] linkage005Address = ecKey1.getAddress();
  String linkage005Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(linkage005Key);
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
  public void testEnergyCostDetail() {
    Assert.assertTrue(PublicMethed.sendcoin(linkage005Address, 5000000000000L, fromAddress,
        testKey003, blockingStubFull));
    Integer times = 0;
    while (!PublicMethed.freezeBalance(linkage005Address, 200000000000L,
        3, linkage005Key, blockingStubFull)) {
      times++;
      if (times == 3) {
        break;
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    times = 0;
    while (!PublicMethed.freezeBalanceGetEnergy(linkage005Address, 200000000000L,
        3, 1, linkage005Key, blockingStubFull)) {
      times++;
      if (times == 3) {
        break;
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    contractName = "EnergyCost";
    code = "6080604052600060035534801561001557600080fd5b5061027b806100256000396000f300608060405260"
        + "0436106100825763ffffffff7c0100000000000000000000000000000000000000000000000000000000600"
        + "0350416633755cd3c81146100875780637d965688146100b1578063a05b2577146100c9578063b0d6304d14"
        + "6100e1578063bbe1d75b14610115578063f8a8fd6d1461012a578063fe75faab14610141575b600080fd5b3"
        + "4801561009357600080fd5b5061009f600435610159565b60408051918252519081900360200190f35b3480"
        + "156100bd57600080fd5b5061009f600435610178565b3480156100d557600080fd5b5061009f60043561019"
        + "8565b3480156100ed57600080fd5b5061009f73ffffffffffffffffffffffffffffffffffffffff60043581"
        + "1690602435166101e2565b34801561012157600080fd5b5061009f6101ff565b34801561013657600080fd5"
        + "b5061013f610205565b005b34801561014d57600080fd5b5061009f600435610218565b6000805482908110"
        + "61016757fe5b600091825260209091200154905081565b600080805b8381101561019157600191820191016"
        + "1017d565b5092915050565b600080805b838110156101915760008054600181810183559180527f290decd9"
        + "548b62a8d60345a988386fc84ba6bc95484008f6362f93160ef3e56301829055918201910161019d565b600"
        + "260209081526000928352604080842090915290825290205481565b60015481565b60038054600101905561"
        + "0216610205565b565b60006102238261022e565b600181905592915050565b600061023c6002830361022e5"
        + "65b6102486001840361022e565b01929150505600a165627a7a72305820bc44fd5f3a0e48cc057752b52e3a"
        + "bf50cd7dc75b3874ea7d049893cf1a2e345f0029";
    abi = "[{\"constant\":true,\"inputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"name\":\""
        + "iarray\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\""
        + "stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{"
        + "\"name\":\"a\",\"type\":\"uint256\"}],\"name\":\"testUseCpu\",\"outputs\":[{\"name\":"
        + "\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\""
        + "type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"a\",\"type\":\""
        + "uint256\"}],\"name\":\"testUseStorage\",\"outputs\":[{\"name\":\"\",\"type\":\""
        + "uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function"
        + "\"},{\"constant\":true,\"inputs\":[{\"name\":\"\",\"type\":\"address\"},{\"name\":\"\","
        + "\"type\":\"address\"}],\"name\":\"m\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\""
        + "}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\""
        + ":true,\"inputs\":[],\"name\":\"calculatedFibNumber\",\"outputs\":[{\"name\":\"\",\"type"
        + "\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},"
        + "{\"constant\":false,\"inputs\":[],\"name\":\"test\",\"outputs\":[],\"payable\":false,"
        + "\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs"
        + "\":[{\"name\":\"n\",\"type\":\"uint256\"}],\"name\":\"setFibonacci\",\"outputs\":"
        + "[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\""
        + "nonpayable\",\"type\":\"function\"}]";
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(linkage005Address,
        blockingStubFull);
    Account info;
    info = PublicMethed.queryAccount(linkage005Address, blockingStubFull);
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
    String txid = PublicMethed.deployContractAndGetTransactionInfoById(contractName, abi, code,
        "", maxFeeLimit, 0L, 100, null, linkage005Key,
        linkage005Address, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;

    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);

    Account infoafter = PublicMethed.queryAccount(linkage005Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(linkage005Address,
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
    logger.info("---------------:");
    Assert.assertEquals(beforeBalance, afterBalance);
    Assert.assertTrue(afterEnergyUsed > 0);
    Assert.assertTrue(afterFreeNetUsed > 0);
    firstForCycleTimes = 1000L;
    secondForCycleTimes = 1002L;
    thirdForCycleTimes = 1004L;

    AccountResourceMessage resourceInfo1 = PublicMethed.getAccountResource(linkage005Address,
        blockingStubFull);
    Account info1 = PublicMethed.queryAccount(linkage005Address, blockingStubFull);
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
    byte[] contractAddress = infoById.get().getContractAddress().toByteArray();
    txid = PublicMethed.triggerContract(contractAddress,
        "testUseCpu(uint256)", firstForCycleTimes.toString(), false,
        0, 100000000L, linkage005Address, linkage005Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account infoafter1 = PublicMethed.queryAccount(linkage005Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter1 = PublicMethed.getAccountResource(linkage005Address,
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
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    firstForCycleCost = infoById.get().getReceipt().getEnergyUsageTotal();
    Assert.assertEquals(beforeBalance1, afterBalance1);
    Assert.assertTrue(afterEnergyUsed1 > beforeEnergyUsed1);
    Assert.assertTrue(afterNetUsed1 > beforeNetUsed1);
    //use EnergyUsed and NetUsed.balance not change
    AccountResourceMessage resourceInfo2 = PublicMethed.getAccountResource(linkage005Address,
        blockingStubFull);
    Account info2 = PublicMethed.queryAccount(linkage005Address, blockingStubFull);
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
    txid = PublicMethed.triggerContract(contractAddress,
        "testUseCpu(uint256)", secondForCycleTimes.toString(), false,
        0, 100000000L, linkage005Address, linkage005Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    secondForCycleCost = infoById.get().getReceipt().getEnergyUsageTotal();
    Account infoafter2 = PublicMethed.queryAccount(linkage005Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter2 = PublicMethed.getAccountResource(linkage005Address,
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
    Assert.assertEquals(beforeBalance2, afterBalance2);
    Assert.assertTrue(afterEnergyUsed2 > beforeEnergyUsed2);
    Assert.assertTrue(afterNetUsed2 > beforeNetUsed2);

    AccountResourceMessage resourceInfo3 = PublicMethed.getAccountResource(linkage005Address,
        blockingStubFull);
    Account info3 = PublicMethed.queryAccount(linkage005Address, blockingStubFull);
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

    txid = PublicMethed.triggerContract(contractAddress,
        "testUseCpu(uint256)", thirdForCycleTimes.toString(), false,
        0, 100000000L, linkage005Address, linkage005Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account infoafter3 = PublicMethed.queryAccount(linkage005Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter3 = PublicMethed.getAccountResource(linkage005Address,
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
    Assert.assertEquals(beforeBalance3, afterBalance3);
    Assert.assertTrue(afterEnergyUsed3 > beforeEnergyUsed3);
    Assert.assertTrue(afterNetUsed3 > beforeNetUsed3);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    thirdForCycleCost = infoById.get().getReceipt().getEnergyUsageTotal();

    logger.info(firstForCycleCost.toString());
    logger.info(secondForCycleCost.toString());
    logger.info(thirdForCycleCost.toString());

    Assert.assertTrue(thirdForCycleCost - secondForCycleCost
        == secondForCycleCost - firstForCycleCost);

    zeroForCycleTimes = 498L;
    firstForCycleTimes = 500L;
    secondForCycleTimes = 502L;
    thirdForCycleTimes = 504L;
    forthForCycleTimes = 506L;
    fifthForCycleTimes = 508L;
    AccountResourceMessage resourceInfo4 = PublicMethed.getAccountResource(linkage005Address,
        blockingStubFull);
    Account info4 = PublicMethed.queryAccount(linkage005Address, blockingStubFull);
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
    txid = PublicMethed.triggerContract(contractAddress,
        "testUseStorage(uint256)", zeroForCycleTimes.toString(), false,
        0, 100000000L, linkage005Address, linkage005Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account infoafter4 = PublicMethed.queryAccount(linkage005Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter4 = PublicMethed.getAccountResource(linkage005Address,
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
    Assert.assertTrue(afterEnergyUsed4 > beforeEnergyUsed4);
    Assert.assertTrue(afterNetUsed4 > beforeNetUsed4);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    zeroForCycleCost = infoById.get().getReceipt().getEnergyUsageTotal();

    AccountResourceMessage resourceInfo5 = PublicMethed.getAccountResource(linkage005Address,
        blockingStubFull);
    Account info5 = PublicMethed.queryAccount(linkage005Address, blockingStubFull);
    Long beforeBalance5 = info5.getBalance();
    Long beforeEnergyLimit5 = resourceInfo5.getEnergyLimit();
    Long beforeEnergyUsed5 = resourceInfo5.getEnergyUsed();
    Long beforeFreeNetLimit5 = resourceInfo5.getFreeNetLimit();
    Long beforeNetLimit5 = resourceInfo5.getNetLimit();
    Long beforeNetUsed5 = resourceInfo5.getNetUsed();
    Long beforeFreeNetUsed5 = resourceInfo5.getFreeNetUsed();
    logger.info("beforeBalance5:" + beforeBalance5);
    logger.info("beforeEnergyLimit5:" + beforeEnergyLimit5);
    logger.info("beforeEnergyUsed5:" + beforeEnergyUsed5);
    logger.info("beforeFreeNetLimit5:" + beforeFreeNetLimit5);
    logger.info("beforeNetLimit5:" + beforeNetLimit5);
    logger.info("beforeNetUsed5:" + beforeNetUsed5);
    logger.info("beforeFreeNetUsed5:" + beforeFreeNetUsed5);
    txid = PublicMethed.triggerContract(contractAddress,
        "testUseStorage(uint256)", firstForCycleTimes.toString(), false,
        0, 100000000L, linkage005Address, linkage005Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account infoafter5 = PublicMethed.queryAccount(linkage005Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter5 = PublicMethed.getAccountResource(linkage005Address,
        blockingStubFull1);
    Long afterBalance5 = infoafter5.getBalance();
    Long afterEnergyLimit5 = resourceInfoafter5.getEnergyLimit();
    Long afterEnergyUsed5 = resourceInfoafter5.getEnergyUsed();
    Long afterFreeNetLimit5 = resourceInfoafter5.getFreeNetLimit();
    Long afterNetLimit5 = resourceInfoafter5.getNetLimit();
    Long afterNetUsed5 = resourceInfoafter5.getNetUsed();
    Long afterFreeNetUsed5 = resourceInfoafter5.getFreeNetUsed();
    logger.info("afterBalance5:" + afterBalance5);
    logger.info("afterEnergyLimit5:" + afterEnergyLimit5);
    logger.info("afterEnergyUsed5:" + afterEnergyUsed5);
    logger.info("afterFreeNetLimit5:" + afterFreeNetLimit5);
    logger.info("afterNetLimit5:" + afterNetLimit5);
    logger.info("afterNetUsed5:" + afterNetUsed5);
    logger.info("afterFreeNetUsed5:" + afterFreeNetUsed5);
    logger.info("---------------:");
    Assert.assertEquals(beforeBalance5, afterBalance5);
    Assert.assertTrue(afterEnergyUsed5 > beforeEnergyUsed5);
    Assert.assertTrue(afterNetUsed5 > beforeNetUsed5);

    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    firstForCycleCost = infoById.get().getReceipt().getEnergyUsageTotal();

    AccountResourceMessage resourceInfo6 = PublicMethed.getAccountResource(linkage005Address,
        blockingStubFull);
    Account info6 = PublicMethed.queryAccount(linkage005Address, blockingStubFull);
    Long beforeBalance6 = info6.getBalance();
    Long beforeEnergyLimit6 = resourceInfo6.getEnergyLimit();
    Long beforeEnergyUsed6 = resourceInfo6.getEnergyUsed();
    Long beforeFreeNetLimit6 = resourceInfo6.getFreeNetLimit();
    Long beforeNetLimit6 = resourceInfo6.getNetLimit();
    Long beforeNetUsed6 = resourceInfo6.getNetUsed();
    Long beforeFreeNetUsed6 = resourceInfo6.getFreeNetUsed();
    logger.info("beforeBalance6:" + beforeBalance6);
    logger.info("beforeEnergyLimit6:" + beforeEnergyLimit6);
    logger.info("beforeEnergyUsed6:" + beforeEnergyUsed6);
    logger.info("beforeFreeNetLimit6:" + beforeFreeNetLimit6);
    logger.info("beforeNetLimit6:" + beforeNetLimit6);
    logger.info("beforeNetUsed6:" + beforeNetUsed6);
    logger.info("beforeFreeNetUsed6:" + beforeFreeNetUsed6);

    txid = PublicMethed.triggerContract(contractAddress,
        "testUseStorage(uint256)", secondForCycleTimes.toString(), false,
        0, 100000000L, linkage005Address, linkage005Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account infoafter6 = PublicMethed.queryAccount(linkage005Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter6 = PublicMethed.getAccountResource(linkage005Address,
        blockingStubFull1);
    Long afterBalance6 = infoafter6.getBalance();
    Long afterEnergyLimit6 = resourceInfoafter6.getEnergyLimit();
    Long afterEnergyUsed6 = resourceInfoafter6.getEnergyUsed();
    Long afterFreeNetLimit6 = resourceInfoafter6.getFreeNetLimit();
    Long afterNetLimit6 = resourceInfoafter6.getNetLimit();
    Long afterNetUsed6 = resourceInfoafter6.getNetUsed();
    Long afterFreeNetUsed6 = resourceInfoafter6.getFreeNetUsed();
    logger.info("afterBalance6:" + afterBalance6);
    logger.info("afterEnergyLimit6:" + afterEnergyLimit6);
    logger.info("afterEnergyUsed6:" + afterEnergyUsed6);
    logger.info("afterFreeNetLimit6:" + afterFreeNetLimit6);
    logger.info("afterNetLimit6:" + afterNetLimit6);
    logger.info("afterNetUsed6:" + afterNetUsed6);
    logger.info("afterFreeNetUsed6:" + afterFreeNetUsed6);
    logger.info("---------------:");
    Assert.assertEquals(beforeBalance6, afterBalance6);
    Assert.assertTrue(afterEnergyUsed6 > beforeEnergyUsed6);
    Assert.assertTrue(afterNetUsed6 > beforeNetUsed6);

    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    secondForCycleCost = infoById.get().getReceipt().getEnergyUsageTotal();
    AccountResourceMessage resourceInfo7 = PublicMethed.getAccountResource(linkage005Address,
        blockingStubFull);
    Account info7 = PublicMethed.queryAccount(linkage005Address, blockingStubFull);
    Long beforeBalance7 = info7.getBalance();
    Long beforeEnergyLimit7 = resourceInfo7.getEnergyLimit();
    Long beforeEnergyUsed7 = resourceInfo7.getEnergyUsed();
    Long beforeFreeNetLimit7 = resourceInfo7.getFreeNetLimit();
    Long beforeNetLimit7 = resourceInfo7.getNetLimit();
    Long beforeNetUsed7 = resourceInfo7.getNetUsed();
    Long beforeFreeNetUsed7 = resourceInfo7.getFreeNetUsed();
    logger.info("beforeBalance7:" + beforeBalance7);
    logger.info("beforeEnergyLimit7:" + beforeEnergyLimit7);
    logger.info("beforeEnergyUsed7:" + beforeEnergyUsed7);
    logger.info("beforeFreeNetLimit7:" + beforeFreeNetLimit7);
    logger.info("beforeNetLimit7:" + beforeNetLimit7);
    logger.info("beforeNetUsed7:" + beforeNetUsed7);
    logger.info("beforeFreeNetUsed7:" + beforeFreeNetUsed7);

    txid = PublicMethed.triggerContract(contractAddress,
        "testUseStorage(uint256)", thirdForCycleTimes.toString(), false,
        0, 100000000L, linkage005Address, linkage005Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account infoafter7 = PublicMethed.queryAccount(linkage005Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter7 = PublicMethed.getAccountResource(linkage005Address,
        blockingStubFull1);
    Long afterBalance7 = infoafter7.getBalance();
    Long afterEnergyLimit7 = resourceInfoafter7.getEnergyLimit();
    Long afterEnergyUsed7 = resourceInfoafter7.getEnergyUsed();
    Long afterFreeNetLimit7 = resourceInfoafter7.getFreeNetLimit();
    Long afterNetLimit7 = resourceInfoafter7.getNetLimit();
    Long afterNetUsed7 = resourceInfoafter7.getNetUsed();
    Long afterFreeNetUsed7 = resourceInfoafter7.getFreeNetUsed();
    logger.info("afterBalance7:" + afterBalance7);
    logger.info("afterEnergyLimit7:" + afterEnergyLimit7);
    logger.info("afterEnergyUsed7:" + afterEnergyUsed7);
    logger.info("afterFreeNetLimit7:" + afterFreeNetLimit7);
    logger.info("afterNetLimit7:" + afterNetLimit7);
    logger.info("afterNetUsed7:" + afterNetUsed7);
    logger.info("afterFreeNetUsed7:" + afterFreeNetUsed7);
    logger.info("---------------:");
    Assert.assertEquals(beforeBalance7, afterBalance7);
    Assert.assertTrue(afterEnergyUsed7 > beforeEnergyUsed7);
    Assert.assertTrue(afterNetUsed7 > beforeNetUsed7);

    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    thirdForCycleCost = infoById.get().getReceipt().getEnergyUsageTotal();

    AccountResourceMessage resourceInfo8 = PublicMethed.getAccountResource(linkage005Address,
        blockingStubFull);
    Account info8 = PublicMethed.queryAccount(linkage005Address, blockingStubFull);
    Long beforeBalance8 = info8.getBalance();
    Long beforeEnergyLimit8 = resourceInfo8.getEnergyLimit();
    Long beforeEnergyUsed8 = resourceInfo8.getEnergyUsed();
    Long beforeFreeNetLimit8 = resourceInfo8.getFreeNetLimit();
    Long beforeNetLimit8 = resourceInfo8.getNetLimit();
    Long beforeNetUsed8 = resourceInfo8.getNetUsed();
    Long beforeFreeNetUsed8 = resourceInfo8.getFreeNetUsed();
    logger.info("beforeBalance8:" + beforeBalance8);
    logger.info("beforeEnergyLimit8:" + beforeEnergyLimit8);
    logger.info("beforeEnergyUsed8:" + beforeEnergyUsed8);
    logger.info("beforeFreeNetLimit8:" + beforeFreeNetLimit8);
    logger.info("beforeNetLimit8:" + beforeNetLimit8);
    logger.info("beforeNetUsed8:" + beforeNetUsed8);
    logger.info("beforeFreeNetUsed8:" + beforeFreeNetUsed8);

    txid = PublicMethed.triggerContract(contractAddress,
        "testUseStorage(uint256)", forthForCycleTimes.toString(), false,
        0, 100000000L, linkage005Address, linkage005Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account infoafter8 = PublicMethed.queryAccount(linkage005Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter8 = PublicMethed.getAccountResource(linkage005Address,
        blockingStubFull1);
    Long afterBalance8 = infoafter8.getBalance();
    Long afterEnergyLimit8 = resourceInfoafter8.getEnergyLimit();
    Long afterEnergyUsed8 = resourceInfoafter8.getEnergyUsed();
    Long afterFreeNetLimit8 = resourceInfoafter8.getFreeNetLimit();
    Long afterNetLimit8 = resourceInfoafter8.getNetLimit();
    Long afterNetUsed8 = resourceInfoafter8.getNetUsed();
    Long afterFreeNetUsed8 = resourceInfoafter8.getFreeNetUsed();
    logger.info("afterBalance8:" + afterBalance8);
    logger.info("afterEnergyLimit8:" + afterEnergyLimit8);
    logger.info("afterEnergyUsed8:" + afterEnergyUsed8);
    logger.info("afterFreeNetLimit8:" + afterFreeNetLimit8);
    logger.info("afterNetLimit8:" + afterNetLimit8);
    logger.info("afterNetUsed8:" + afterNetUsed8);
    logger.info("afterFreeNetUsed8:" + afterFreeNetUsed8);
    logger.info("---------------:");
    Assert.assertEquals(beforeBalance8, afterBalance8);
    Assert.assertTrue(afterEnergyUsed8 > beforeEnergyUsed8);
    Assert.assertTrue(afterNetUsed8 > beforeNetUsed8);

    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    forthForCycleCost = infoById.get().getReceipt().getEnergyUsageTotal();

    AccountResourceMessage resourceInfo9 = PublicMethed.getAccountResource(linkage005Address,
        blockingStubFull);
    Account info9 = PublicMethed.queryAccount(linkage005Address, blockingStubFull);
    Long beforeBalance9 = info9.getBalance();
    Long beforeEnergyLimit9 = resourceInfo9.getEnergyLimit();
    Long beforeEnergyUsed9 = resourceInfo9.getEnergyUsed();
    Long beforeFreeNetLimit9 = resourceInfo9.getFreeNetLimit();
    Long beforeNetLimit9 = resourceInfo9.getNetLimit();
    Long beforeNetUsed9 = resourceInfo9.getNetUsed();
    Long beforeFreeNetUsed9 = resourceInfo9.getFreeNetUsed();
    logger.info("beforeBalance9:" + beforeBalance9);
    logger.info("beforeEnergyLimit9:" + beforeEnergyLimit9);
    logger.info("beforeEnergyUsed9:" + beforeEnergyUsed9);
    logger.info("beforeFreeNetLimit9:" + beforeFreeNetLimit9);
    logger.info("beforeNetLimit9:" + beforeNetLimit9);
    logger.info("beforeNetUsed9:" + beforeNetUsed9);
    logger.info("beforeFreeNetUsed9:" + beforeFreeNetUsed9);

    txid = PublicMethed.triggerContract(contractAddress,
        "testUseStorage(uint256)", fifthForCycleTimes.toString(), false,
        0, 100000000L, linkage005Address, linkage005Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account infoafter9 = PublicMethed.queryAccount(linkage005Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter9 = PublicMethed.getAccountResource(linkage005Address,
        blockingStubFull1);
    Long afterBalance9 = infoafter9.getBalance();
    Long afterEnergyLimit9 = resourceInfoafter9.getEnergyLimit();
    Long afterEnergyUsed9 = resourceInfoafter9.getEnergyUsed();
    Long afterFreeNetLimit9 = resourceInfoafter9.getFreeNetLimit();
    Long afterNetLimit9 = resourceInfoafter9.getNetLimit();
    Long afterNetUsed9 = resourceInfoafter9.getNetUsed();
    Long afterFreeNetUsed9 = resourceInfoafter9.getFreeNetUsed();
    logger.info("afterBalance9:" + afterBalance9);
    logger.info("afterEnergyLimit9:" + afterEnergyLimit9);
    logger.info("afterEnergyUsed9:" + afterEnergyUsed9);
    logger.info("afterFreeNetLimit9:" + afterFreeNetLimit9);
    logger.info("afterNetLimit9:" + afterNetLimit9);
    logger.info("afterNetUsed9:" + afterNetUsed9);
    logger.info("afterFreeNetUsed9:" + afterFreeNetUsed9);
    logger.info("---------------:");
    Assert.assertEquals(beforeBalance9, afterBalance9);
    Assert.assertTrue(afterEnergyUsed9 > beforeEnergyUsed9);
    Assert.assertTrue(afterNetUsed9 > beforeNetUsed9);

    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    fifthForCycleCost = infoById.get().getReceipt().getEnergyUsageTotal();

    logger.info("Zero cost is " + zeroForCycleCost);
    logger.info("First cost is " + firstForCycleCost);
    logger.info("Second cost is " + secondForCycleCost);
    logger.info("Third cost is " + thirdForCycleCost);
    logger.info("Forth cost is " + forthForCycleCost);
    logger.info("Fifth cost is " + fifthForCycleCost);
    logger.info(Long.toString(firstForCycleCost - zeroForCycleCost));
    logger.info(Long.toString(secondForCycleCost - firstForCycleCost));
    logger.info(Long.toString(thirdForCycleCost - secondForCycleCost));
    logger.info(Long.toString(forthForCycleCost - thirdForCycleCost));
    logger.info(Long.toString(fifthForCycleCost - forthForCycleCost));

    Assert.assertTrue(thirdForCycleCost - secondForCycleCost
        == secondForCycleCost - firstForCycleCost);
    Assert.assertTrue(fifthForCycleCost - forthForCycleCost
        == forthForCycleCost - thirdForCycleCost);

  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}


