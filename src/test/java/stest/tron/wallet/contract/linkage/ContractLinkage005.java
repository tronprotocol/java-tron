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
  byte [] contractAddress;

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

  }

  @Test(enabled = true)
  public void testEnergyCostDetail() {
    Assert.assertTrue(PublicMethed.sendcoin(linkage005Address,5000000000000L,fromAddress,
        testKey003,blockingStubFull));
    Integer times = 0;
    while (!PublicMethed.freezeBalance(linkage005Address,200000000000L,
        3,linkage005Key,blockingStubFull)) {
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
    while (!PublicMethed.freezeBalanceGetEnergy(linkage005Address,200000000000L,
        3,1,linkage005Key,blockingStubFull)) {
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
    code = "6080604052600060035534801561001557600080fd5b5061027b806100256000396000f3006080604052600436106100825763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416633755cd3c81146100875780637d965688146100b1578063a05b2577146100c9578063b0d6304d146100e1578063bbe1d75b14610115578063f8a8fd6d1461012a578063fe75faab14610141575b600080fd5b34801561009357600080fd5b5061009f600435610159565b60408051918252519081900360200190f35b3480156100bd57600080fd5b5061009f600435610178565b3480156100d557600080fd5b5061009f600435610198565b3480156100ed57600080fd5b5061009f73ffffffffffffffffffffffffffffffffffffffff600435811690602435166101e2565b34801561012157600080fd5b5061009f6101ff565b34801561013657600080fd5b5061013f610205565b005b34801561014d57600080fd5b5061009f600435610218565b600080548290811061016757fe5b600091825260209091200154905081565b600080805b83811015610191576001918201910161017d565b5092915050565b600080805b838110156101915760008054600181810183559180527f290decd9548b62a8d60345a988386fc84ba6bc95484008f6362f93160ef3e56301829055918201910161019d565b600260209081526000928352604080842090915290825290205481565b60015481565b600380546001019055610216610205565b565b60006102238261022e565b600181905592915050565b600061023c6002830361022e565b6102486001840361022e565b01929150505600a165627a7a72305820bc44fd5f3a0e48cc057752b52e3abf50cd7dc75b3874ea7d049893cf1a2e345f0029";
    abi = "[{\"constant\":true,\"inputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"name\":\"iarray\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"a\",\"type\":\"uint256\"}],\"name\":\"testUseCpu\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"a\",\"type\":\"uint256\"}],\"name\":\"testUseStorage\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"\",\"type\":\"address\"},{\"name\":\"\",\"type\":\"address\"}],\"name\":\"m\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"calculatedFibNumber\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"test\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"n\",\"type\":\"uint256\"}],\"name\":\"setFibonacci\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";

    contractAddress = PublicMethed.deployContract(contractName,abi,code,
        "",maxFeeLimit, 0L, 100,null,linkage005Key,
        linkage005Address,blockingStubFull);

    firstForCycleTimes = 1000L;
    secondForCycleTimes = 1002L;
    thirdForCycleTimes = 1004L;

    String txid = PublicMethed.triggerContract(contractAddress,
        "testUseCpu(uint256)",firstForCycleTimes.toString(), false,
        0, 100000000L, linkage005Address, linkage005Key, blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txid,blockingStubFull);
    firstForCycleCost = infoById.get().getReceipt().getEnergyUsageTotal();

    txid = PublicMethed.triggerContract(contractAddress,
        "testUseCpu(uint256)",secondForCycleTimes.toString(), false,
        0, 100000000L, linkage005Address, linkage005Key, blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid,blockingStubFull);
    secondForCycleCost = infoById.get().getReceipt().getEnergyUsageTotal();

    txid = PublicMethed.triggerContract(contractAddress,
        "testUseCpu(uint256)",thirdForCycleTimes.toString(), false,
        0, 100000000L, linkage005Address, linkage005Key, blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid,blockingStubFull);
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

    txid = PublicMethed.triggerContract(contractAddress,
        "testUseStorage(uint256)",zeroForCycleTimes.toString(), false,
        0, 100000000L, linkage005Address, linkage005Key, blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid,blockingStubFull);
    zeroForCycleCost = infoById.get().getReceipt().getEnergyUsageTotal();

    txid = PublicMethed.triggerContract(contractAddress,
        "testUseStorage(uint256)",firstForCycleTimes.toString(), false,
        0, 100000000L, linkage005Address, linkage005Key, blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid,blockingStubFull);
    firstForCycleCost = infoById.get().getReceipt().getEnergyUsageTotal();

    txid = PublicMethed.triggerContract(contractAddress,
        "testUseStorage(uint256)",secondForCycleTimes.toString(), false,
        0, 100000000L, linkage005Address, linkage005Key, blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid,blockingStubFull);
    secondForCycleCost = infoById.get().getReceipt().getEnergyUsageTotal();

    txid = PublicMethed.triggerContract(contractAddress,
        "testUseStorage(uint256)",thirdForCycleTimes.toString(), false,
        0, 100000000L, linkage005Address, linkage005Key, blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid,blockingStubFull);
    thirdForCycleCost = infoById.get().getReceipt().getEnergyUsageTotal();

    txid = PublicMethed.triggerContract(contractAddress,
        "testUseStorage(uint256)",forthForCycleTimes.toString(), false,
        0, 100000000L, linkage005Address, linkage005Key, blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid,blockingStubFull);
    forthForCycleCost = infoById.get().getReceipt().getEnergyUsageTotal();

    txid = PublicMethed.triggerContract(contractAddress,
        "testUseStorage(uint256)",fifthForCycleTimes.toString(), false,
        0, 100000000L, linkage005Address, linkage005Key, blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid,blockingStubFull);
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


