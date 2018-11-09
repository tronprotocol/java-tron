package stest.tron.wallet.manual;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.SmartContract;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class WalletTestAccount012 {
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("mainWitness.key2");
  private final byte[] testAddress003 = PublicMethed.getFinalAddress(testKey003);

  private final String testKey004 = Configuration.getByPath("testng.conf")
      .getString("mainWitness.key3");
  private final byte[] testAddress004 = PublicMethed.getFinalAddress(testKey004);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  ArrayList<String> txidList = new ArrayList<String>();

  Optional<TransactionInfo> infoById = null;
  Long beforeTime;
  Long afterTime;
  Long beforeBlockNum;
  Long afterBlockNum;
  Block currentBlock;
  Long currentBlockNum;

  //get account
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] asset011Address = ecKey1.getAddress();
  String testKeyForAssetIssue011 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());



  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);

  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(testKeyForAssetIssue011);
    PublicMethed.printAddress(testKey002);
    PublicMethed.printAddress(testKey003);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
    currentBlock = blockingStubFull1.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    beforeBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    beforeTime = System.currentTimeMillis();
  }

  @Test(enabled = true,threadPoolSize = 1, invocationCount = 1)
  public void storageAndCpu() {
    PublicMethed.sendcoin(asset011Address,10000000000L,fromAddress,testKey002,blockingStubFull);
    Random rand = new Random();
    Integer randNum = rand.nextInt(30) + 1;
    randNum = rand.nextInt(4000);

    Long maxFeeLimit = 1000000000L;
    String contractName = "StorageAndCpu" + Integer.toString(randNum);
    String code = "6080604052600060045534801561001557600080fd5b506104c0806100256000396000f3006080604052600436106100a35763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416632656df4881146100a85780633755cd3c146100bf5780636f269385146100e95780637d965688146100fe578063a05b257714610116578063b0d6304d1461012e578063b648763914610162578063bbe1d75b146101ec578063f8a8fd6d14610201578063fe75faab14610216575b600080fd5b3480156100b457600080fd5b506100bd61022e565b005b3480156100cb57600080fd5b506100d7600435610230565b60408051918252519081900360200190f35b3480156100f557600080fd5b506100bd61024f565b34801561010a57600080fd5b506100d7600435610296565b34801561012257600080fd5b506100d76004356102b6565b34801561013a57600080fd5b506100d773ffffffffffffffffffffffffffffffffffffffff60043581169060243516610300565b34801561016e57600080fd5b5061017761031d565b6040805160208082528351818301528351919283929083019185019080838360005b838110156101b1578181015183820152602001610199565b50505050905090810190601f1680156101de5780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b3480156101f857600080fd5b506100d76103ab565b34801561020d57600080fd5b506100bd6103b1565b34801561022257600080fd5b506100d76004356103c2565b565b600180548290811061023e57fe5b600091825260209091200154905081565b6040805180820190915260088082527f31323334353637380000000000000000000000000000000000000000000000006020909201918252610293916000916103f9565b50565b600080805b838110156102af576001918201910161029b565b5092915050565b600080805b838110156102af5760018054808201825560008290527fb10e2d527612073b26eecdfd717e6a320cf44b4afac2b0732d9fcbe2b7fa0cf60182905591820191016102bb565b600360209081526000928352604080842090915290825290205481565b6000805460408051602060026001851615610100026000190190941693909304601f810184900484028201840190925281815292918301828280156103a35780601f10610378576101008083540402835291602001916103a3565b820191906000526020600020905b81548152906001019060200180831161038657829003601f168201915b505050505081565b60025481565b60048054600101905561022e6103b1565b60006103cd826103d8565b600281905592915050565b60006103e6600283036103d8565b6103f2600184036103d8565b0192915050565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f1061043a57805160ff1916838001178555610467565b82800160010185558215610467579182015b8281111561046757825182559160200191906001019061044c565b50610473929150610477565b5090565b61049191905b80821115610473576000815560010161047d565b905600a165627a7a72305820ca3a4850a926264dc27c0e3483830bac3385c61565c738281b05d747d20676670029";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"oneCpu\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"name\":\"iarray\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"storage8Char\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"a\",\"type\":\"uint256\"}],\"name\":\"testUseCpu\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"a\",\"type\":\"uint256\"}],\"name\":\"testUseStorage\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"\",\"type\":\"address\"},{\"name\":\"\",\"type\":\"address\"}],\"name\":\"m\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"iarray1\",\"outputs\":[{\"name\":\"\",\"type\":\"string\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"calculatedFibNumber\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"test\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"n\",\"type\":\"uint256\"}],\"name\":\"setFibonacci\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    byte[] contractAddress = PublicMethed.deployContract(contractName,abi,code,
        "",maxFeeLimit,
        0L, 100,null,testKeyForAssetIssue011,asset011Address,blockingStubFull);
    try {
      Thread.sleep(3000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    SmartContract smartContract = PublicMethed.getContract(contractAddress,blockingStubFull);
    String txid;

    Integer i = 1;
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(asset011Address,
        blockingStubFull);
    accountResource = PublicMethed.getAccountResource(asset011Address,
        blockingStubFull);
    Long beforeEnergyLimit = accountResource.getEnergyLimit();
    Long afterEnergyLimit;
    Long beforeTotalEnergyLimit = accountResource.getTotalEnergyLimit();
    Account account = PublicMethed.queryAccount(testKeyForAssetIssue011,blockingStubFull);
    Long afterTotalEnergyLimit;
    while (i++ < 5) {
      accountResource = PublicMethed.getAccountResource(asset011Address,
          blockingStubFull);
      beforeEnergyLimit = accountResource.getEnergyLimit();
      beforeTotalEnergyLimit = accountResource.getTotalEnergyLimit();
      String initParmes = "\"" + "930" + "\"";
      txid = PublicMethed.triggerContract(contractAddress,
          "storage8Char()", "", false,
          0, maxFeeLimit, asset011Address, testKeyForAssetIssue011, blockingStubFull);
      accountResource = PublicMethed.getAccountResource(asset011Address,
          blockingStubFull);
      logger.info("Current limit is " + accountResource.getTotalEnergyLimit());
      PublicMethed.freezeBalanceGetEnergy(asset011Address,1000000L,3,
          1,testKeyForAssetIssue011,blockingStubFull);

      accountResource = PublicMethed.getAccountResource(asset011Address,
          blockingStubFull);
      afterEnergyLimit = accountResource.getEnergyLimit();
      afterTotalEnergyLimit = accountResource.getTotalEnergyLimit();

      logger.info("Total energy limit min is " + (afterTotalEnergyLimit - beforeTotalEnergyLimit));
      logger.info("Energy limit min is " + (afterEnergyLimit - beforeEnergyLimit));
      Float rate = (float)(afterTotalEnergyLimit - beforeTotalEnergyLimit) / beforeTotalEnergyLimit;
      logger.info("rate is " + rate);
      Assert.assertTrue(rate >= 0.001001000 && rate <= 0.001001002);
      txidList.add(txid);
      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      account = PublicMethed.queryAccount(testKeyForAssetIssue011,blockingStubFull);
      Float energyrate = (float)(beforeEnergyLimit) / account.getAccountResource()
          .getFrozenBalanceForEnergy().getFrozenBalance() * 1000000;
      logger.info("energy rate is " + energyrate);
    }
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}