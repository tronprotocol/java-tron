
package stest.tron.wallet.onlineStress;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;
import java.util.Random;
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
import org.tron.protos.Protocol.SmartContract;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class TestExceptionCodeAndAbi {

  //testng001、testng002、testng003、testng004
  private final String testNetAccountKey =
      "6815B367FDDE637E53E9ADC8E69424E07724333C9A2B973CFA469975E20753FC";
  //"FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";
  //"BC70ADC5A0971BA3F7871FBB7249E345D84CE7E5458828BE1E28BF8F98F2795B";
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;

  private ManagedChannel channelFull2 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull2 = null;

  private ManagedChannel channelFull3 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull3 = null;

  private ManagedChannel channelFull4 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull4 = null;


  private ManagedChannel channelFull5 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull5 = null;

  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode2 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private String fullnode3 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(2);
  private String fullnode4 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(3);
  private String fullnode5 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(4);

  byte[] contractAddress = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] asset016Address = ecKey1.getAddress();
  String testKeyForAssetIssue016 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = false)
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

    channelFull2 = ManagedChannelBuilder.forTarget(fullnode2)
        .usePlaintext(true)
        .build();
    blockingStubFull2 = WalletGrpc.newBlockingStub(channelFull2);

    channelFull3 = ManagedChannelBuilder.forTarget(fullnode3)
        .usePlaintext(true)
        .build();
    blockingStubFull3 = WalletGrpc.newBlockingStub(channelFull3);

    channelFull4 = ManagedChannelBuilder.forTarget(fullnode4)
        .usePlaintext(true)
        .build();
    blockingStubFull4 = WalletGrpc.newBlockingStub(channelFull4);

    channelFull5 = ManagedChannelBuilder.forTarget(fullnode5)
        .usePlaintext(true)
        .build();
    blockingStubFull5 = WalletGrpc.newBlockingStub(channelFull5);

    logger.info(Long.toString(PublicMethed.queryAccount(testNetAccountKey,blockingStubFull)
        .getBalance()));
    PublicMethed.sendcoin(asset016Address,10000000000000L,testNetAccountAddress,
        testNetAccountKey,blockingStubFull);
    Assert.assertTrue(PublicMethed.freezeBalance(asset016Address,1000000L,3,
        testKeyForAssetIssue016,blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(asset016Address,3000000000L,
        3,1,testKeyForAssetIssue016,blockingStubFull));
  }

  @Test(enabled = false)
  public void testExceptionCodeAndAbi() {
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(testNetAccountAddress,
        blockingStubFull);
    Long cpuLimit = accountResource.getEnergyLimit();
    //Long storageLimit = accountResource.getStorageLimit();
    Long cpuUsage = accountResource.getEnergyUsed();
    //Long storageUsage = accountResource.getStorageUsed();
    Account account = PublicMethed.queryAccount(testNetAccountKey,blockingStubFull);
    logger.info("before balance is " + Long.toString(account.getBalance()));
    logger.info("before cpu limit is " + Long.toString(cpuLimit));
    logger.info("before cpu usage is " + Long.toString(cpuUsage));
    //logger.info("before storage limit is " + Long.toString(storageLimit));
    //logger.info("before storage usaged is " + Long.toString(storageUsage));
    Long maxFeeLimit = 100000000000L;
    Integer times = 0;
    String txid;
    byte[] contractAddress;
    Optional<TransactionInfo> infoById = null;
    Long energyTotal;

    while (times++ < 1) {
      String contractName = "Fomo3D";
      //String code = "608060405234801561001057600080fd5b50610731806100206000396000f3006080604052600436106100a35763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166306fdde0381146100a8578063095ea7b31461013257806318160ddd146101585780632f745c591461017f5780636352211e146101a35780636914db60146101d757806370a08231146101ef57806395d89b4114610210578063a9059cbb14610225578063b2e6ceeb14610249575b600080fd5b3480156100b457600080fd5b506100bd610261565b6040805160208082528351818301528351919283929083019185019080838360005b838110156100f75781810151838201526020016100df565b50505050905090810190601f1680156101245780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b34801561013e57600080fd5b50610156600160a060020a0360043516602435610298565b005b34801561016457600080fd5b5061016d61032d565b60408051918252519081900360200190f35b34801561018b57600080fd5b5061016d600160a060020a0360043516602435610336565b3480156101af57600080fd5b506101bb60043561035e565b60408051600160a060020a039092168252519081900360200190f35b3480156101e357600080fd5b506100bd600435610397565b3480156101fb57600080fd5b5061016d600160a060020a0360043516610438565b34801561021c57600080fd5b506100bd610453565b34801561023157600080fd5b50610156600160a060020a036004351660243561048a565b34801561025557600080fd5b5061015660043561059d565b60408051808201909152601181527f54726f6e2045524337323120546f6b656e000000000000000000000000000000602082015290565b6102a18161035e565b600160a060020a031633146102b557600080fd5b33600160a060020a03831614156102cb57600080fd5b336000818152600360209081526040808320600160a060020a03871680855290835292819020859055805185815290519293927f8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b925929181900390910190a35050565b64e8d4a5100090565b600160a060020a03919091166000908152600460209081526040808320938352929052205490565b60008181526002602052604081205460ff16151561037b57600080fd5b50600090815260016020526040902054600160a060020a031690565b60008181526005602090815260409182902080548351601f600260001961010060018616150201909316929092049182018490048402810184019094528084526060939283018282801561042c5780601f106104015761010080835404028352916020019161042c565b820191906000526020600020905b81548152906001019060200180831161040f57829003601f168201915b50505050509050919050565b600160a060020a031660009081526020819052604090205490565b60408051808201909152600581527f5437323154000000000000000000000000000000000000000000000000000000602082015290565b6000818152600260205260408120543391849160ff1615156104ab57600080fd5b6104b48461035e565b600160a060020a038481169116146104cb57600080fd5b600160a060020a0383811690831614156104e457600080fd5b600160a060020a03821615156104f957600080fd5b508161050581856106a7565b600160a060020a0381811660008181526020818152604080832080546000190190558883526001808352818420805473ffffffffffffffffffffffffffffffffffffffff19169689169687179055858452838352928190208054909301909255815188815291517fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef9281900390910190a35050505050565b600081815260026020526040812054819060ff1615156105bc57600080fd5b6105c58361035e565b9150339050600160a060020a0382168114156105e057600080fd5b600160a060020a03808316600090815260036020908152604080832093851683529290522054831461061157600080fd5b600160a060020a0382811660008181526020818152604080832080546000190190558783526001808352818420805473ffffffffffffffffffffffffffffffffffffffff19169688169687179055858452838352928190208054909301909255815187815291517fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef9281900390910190a3505050565b60005b600160a060020a0383166000908152600460209081526040808320848452909152902054821461070057600160a060020a03831660009081526004602090815260408083208484529091528120556001016106aa565b5050505600a165627a7a72305820d3ca2ca957b72f4c5028c633a6ad4bafe13572bf949793fabe72e34eb640d2c50029";
      String code = "60806040" + getRandomCode(2734) + getRandomCode(1000) + "0029";
      String abi = "[{\"constant\":true,\"inputs\":[],\"name\":\"name\",\"outputs\":[{\"name\":\"_name\",\"type\":\"string\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"totalSupply1\",\"outputs\":[{\"name\":\"_totalSupply1\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"decimals\",\"outputs\":[{\"name\":\"_decimals\",\"type\":\"uint8\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"MAX_UINT256\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"_owner\",\"type\":\"address\"}],\"name\":\"balanceOf\",\"outputs\":[{\"name\":\"balance\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"symbol\",\"outputs\":[{\"name\":\"_symbol\",\"type\":\"string\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_to\",\"type\":\"address\"},{\"name\":\"_value\",\"type\":\"uint256\"}],\"name\":\"transfer\",\"outputs\":[{\"name\":\"success\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_to\",\"type\":\"address\"},{\"name\":\"_value\",\"type\":\"uint256\"},{\"name\":\"_data\",\"type\":\"bytes\"}],\"name\":\"transfer\",\"outputs\":[{\"name\":\"success\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_to\",\"type\":\"address\"},{\"name\":\"_value\",\"type\":\"uint256\"},{\"name\":\"_data\",\"type\":\"bytes\"},{\"name\":\"_custom_fallback\",\"type\":\"string\"}],\"name\":\"transfer\",\"outputs\":[{\"name\":\"success\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"name\":\"_from\",\"type\":\"address\"},{\"indexed\":true,\"name\":\"_to\",\"type\":\"address\"},{\"indexed\":false,\"name\":\"_value\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"_data\",\"type\":\"bytes\"}],\"name\":\"Transfer\",\"type\":\"event\"}]";

      txid = PublicMethed.deployContractAndGetTransactionInfoById(contractName,abi,code,"",
          maxFeeLimit, 0L, 100,null,testNetAccountKey,testNetAccountAddress,blockingStubFull);
      logger.info("createGen0 " + txid);
      infoById = PublicMethed.getTransactionInfoById(txid,blockingStubFull);
      energyTotal = infoById.get().getReceipt().getEnergyUsageTotal();
      //writeCsv(code,abi,"", "","",txid,energyTotal.toString());

    }
    //final SmartContract smartContract = PublicMethed.getContract(contractAddress,
    // blockingStubFull);
    accountResource = PublicMethed.getAccountResource(testNetAccountAddress,blockingStubFull);
    cpuLimit = accountResource.getEnergyLimit();
    //storageLimit = accountResource.getStorageLimit();
    cpuUsage = accountResource.getEnergyUsed();
    //storageUsage = accountResource.getStorageUsed();
    account = PublicMethed.queryAccount(testNetAccountKey,blockingStubFull);
    logger.info("after balance is " + Long.toString(account.getBalance()));
    logger.info("after cpu limit is " + Long.toString(cpuLimit));
    logger.info("after cpu usage is " + Long.toString(cpuUsage));
    //logger.info("after storage limit is " + Long.toString(storageLimit));
    //logger.info("after storage usaged is " + Long.toString(storageUsage));
    //Assert.assertTrue(storageUsage > 0);
    //Assert.assertTrue(storageLimit > 0);
    Assert.assertTrue(cpuLimit > 0);
    Assert.assertTrue(cpuUsage > 0);
    //Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    //Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    //Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
    //logger.info(smartContract.getName());
    //logger.info(smartContract.getAbi().toString());
  }

  @Test(enabled = false)
  public void testtimeout() {
    String txid = "";
    Long energyTotal;
    String contractName = "timeout";
    String code = "6080604052600060035534801561001557600080fd5b5061027b806100256000396000f3006080604052600436106100825763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416633755cd3c81146100875780637d965688146100b1578063a05b2577146100c9578063b0d6304d146100e1578063bbe1d75b14610115578063f8a8fd6d1461012a578063fe75faab14610141575b600080fd5b34801561009357600080fd5b5061009f600435610159565b60408051918252519081900360200190f35b3480156100bd57600080fd5b5061009f600435610178565b3480156100d557600080fd5b5061009f600435610198565b3480156100ed57600080fd5b5061009f73ffffffffffffffffffffffffffffffffffffffff600435811690602435166101e2565b34801561012157600080fd5b5061009f6101ff565b34801561013657600080fd5b5061013f610205565b005b34801561014d57600080fd5b5061009f600435610218565b600080548290811061016757fe5b600091825260209091200154905081565b600080805b83811015610191576001918201910161017d565b5092915050565b600080805b838110156101915760008054600181810183559180527f290decd9548b62a8d60345a988386fc84ba6bc95484008f6362f93160ef3e56301829055918201910161019d565b600260209081526000928352604080842090915290825290205481565b60015481565b600380546001019055610216610205565b565b60006102238261022e565b600181905592915050565b600061023c6002830361022e565b6102486001840361022e565b01929150505600a165627a7a7230582077fd7ac1cd0908622d05db388922d485d6f8e3a546590b97ec8398f87f0c8a580029";
    String abi = "[{\"constant\":true,\"inputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"name\":\"iarray\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"a\",\"type\":\"uint256\"}],\"name\":\"testUseCpu\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"a\",\"type\":\"uint256\"}],\"name\":\"testUseStorage\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"\",\"type\":\"address\"},{\"name\":\"\",\"type\":\"address\"}],\"name\":\"m\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"calculatedFibNumber\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"test\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"n\",\"type\":\"uint256\"}],\"name\":\"setFibonacci\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    contractAddress = PublicMethed.deployContract(contractName,abi,code,"",maxFeeLimit, 0L, 
        100,null,testKeyForAssetIssue016,asset016Address,blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    Integer triggerNum = 10000000;
    Long energy;
    Integer times = 0;
    Account info = PublicMethed.queryAccount(testKeyForAssetIssue016,blockingStubFull);
    Long beforeBalance;
    Long beforeNetLimit;
    Long beforeFreeNetLimit;
    Long beforeFreeNetUsed;
    Long beforeNetUsed;
    Long beforeEnergyLimit;
    Long beforeEnergyUsed;

    Long afterBalance;
    Long afterNetLimit;
    Long afterFreeNetLimit;
    Long afterFreeNetUsed;
    Long afterNetUsed;
    Long afterEnergyLimit;
    Long afterEnergyUsed;

    Long energyUsed;
    Long netUsed;
    Long energyFee;
    Long fee;
    Long energyUsageTotal;
    Long netFee;
    Long minBalance;

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull);


    while (times++ < 10000) {
      info = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull);
      beforeBalance = info.getBalance();
      beforeEnergyLimit = resourceInfo.getEnergyLimit();
      beforeEnergyUsed = resourceInfo.getEnergyUsed();
      beforeFreeNetLimit = resourceInfo.getFreeNetLimit();
      beforeNetLimit = resourceInfo.getNetLimit();
      beforeNetUsed = resourceInfo.getNetUsed();
      beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
      logger.info("beofre free net used:" + Long.toString(beforeFreeNetUsed));

      logger.info("before in 1 the balance is " + PublicMethed.queryAccount(asset016Address,
          blockingStubFull1).getBalance());
      logger.info("before in 2 the balance is " + PublicMethed.queryAccount(asset016Address,
          blockingStubFull2).getBalance());
      logger.info("before in 3 the balance is " + PublicMethed.queryAccount(asset016Address,
          blockingStubFull3).getBalance());
      logger.info("before in 4 the balance is " + PublicMethed.queryAccount(asset016Address,
          blockingStubFull4).getBalance());
      logger.info("before in 5 the balance is " + PublicMethed.queryAccount(asset016Address,
          blockingStubFull5).getBalance());


      txid = PublicMethed.triggerContract(contractAddress,
          "testUseCpu(uint256)", triggerNum.toString(), false,
          0, maxFeeLimit, asset016Address, testKeyForAssetIssue016, blockingStubFull);
      try {
        Thread.sleep(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull2);
      energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
      fee = infoById.get().getFee();
      energyFee = infoById.get().getReceipt().getEnergyFee();
      netUsed = infoById.get().getReceipt().getNetUsage();
      energyUsed = infoById.get().getReceipt().getEnergyUsage();
      netFee = infoById.get().getReceipt().getNetFee();

      info = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull3);
      afterBalance = info.getBalance();
      afterEnergyLimit = resourceInfo.getEnergyLimit();
      afterEnergyUsed = resourceInfo.getEnergyUsed();
      afterFreeNetLimit = resourceInfo.getFreeNetLimit();
      afterNetLimit = resourceInfo.getNetLimit();
      afterNetUsed = resourceInfo.getNetUsed();
      minBalance = beforeBalance - afterBalance;
      resourceInfo = PublicMethed.getAccountResource(asset016Address,
          blockingStubFull2);
      afterFreeNetUsed = resourceInfo.getFreeNetUsed();


      serachInfo(txid,blockingStubFull1);
      serachInfo(txid,blockingStubFull2);
      serachInfo(txid,blockingStubFull3);
      serachInfo(txid,blockingStubFull4);
      serachInfo(txid,blockingStubFull5);
      logger.info("after free net used:" + Long.toString(afterFreeNetUsed));

      logger.info("after in 1 the balance is " + PublicMethed.queryAccount(asset016Address,
          blockingStubFull1).getBalance());
      logger.info("after in 2 the balance is " + PublicMethed.queryAccount(asset016Address,
          blockingStubFull2).getBalance());
      logger.info("after in 3 the balance is " + PublicMethed.queryAccount(asset016Address,
          blockingStubFull3).getBalance());
      logger.info("after in 4 the balance is " + PublicMethed.queryAccount(asset016Address,
          blockingStubFull4).getBalance());
      logger.info("after in 5 the balance is " + PublicMethed.queryAccount(asset016Address,
          blockingStubFull5).getBalance());




      writeCsv(minBalance.toString(),beforeBalance.toString(),beforeNetLimit.toString(),
          beforeFreeNetLimit.toString(),beforeNetUsed.toString(),
          beforeEnergyLimit.toString(),beforeEnergyUsed.toString(),beforeFreeNetUsed.toString(),
          energyUsageTotal.toString(),fee.toString(),energyFee.toString(),
          netUsed.toString(),energyUsed.toString(),netFee.toString(),afterBalance.toString(),
          afterEnergyLimit.toString(), afterEnergyUsed.toString(),afterFreeNetUsed.toString(),
          afterFreeNetLimit.toString(),afterNetLimit.toString(),afterNetUsed.toString(),txid,
          testKeyForAssetIssue016);
    }




  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  public static void serachInfo(String txid, WalletGrpc.WalletBlockingStub blockingStubFull) {
    Optional<TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txid, 
        blockingStubFull);
    logger.info("---------------------------------------------");

    logger.info("fee is " + infoById.get().getFee());
    logger.info("Energy fee is " + infoById.get().getReceipt().getEnergyFee());
    logger.info("Total energy is " + infoById.get().getReceipt().getEnergyUsageTotal());
    logger.info("Energy used is " + infoById.get().getReceipt().getEnergyUsage());
    logger.info("Net used is " + infoById.get().getReceipt().getNetUsage());
    logger.info("Net fee is " + infoById.get().getReceipt().getNetFee());

  }




  public static String getRandomCode(int length) {
    String str = "0123456789";
    Random random = new Random();
    StringBuffer sb = new StringBuffer();
    for (int i = 0;i < length; i++) {
      int number = random.nextInt(10);
      sb.append(str.charAt(number));
    }
    return sb.toString();
  }




  public static void writeCsv(String minBalance, String beforeBalance,String beforeNetLimit,
      String beforeFreeNet, String beforeNetUsed, String beforeEnergyLimit,String beforeEnergyUsed, 
      String beforeFreeNetUsed, String energyUsageTotal, String fee,String energyFee, 
      String netUsed, String energyUsed, 
      String netFee, String afterBalance, String afterEnergyLimit, String afterEnergyUsed, 
      String afterFreeNetUsed, String afterFreeNet, String afterNetLimit,
      String afterNetUsed, String txid, String testKeyForAssetIssue016) {
    try {
      File csv = new File("/Users/wangzihe/Documents/costFee.csv");
      String time = Long.toString(System.currentTimeMillis());
      BufferedWriter bw = new BufferedWriter(new FileWriter(csv, true));
      bw.write("TestTimeoutusefeelimit:(HaveEnergyAndHaveFreezeNet)" + "," + "timestamp" + time 
          + "," + "min:" + minBalance + "," + "beforeBalance:" + beforeBalance + "," 
          + "beforeNetLimit:" 
          + beforeNetLimit + "," + "beforeFreeNet:" +  beforeFreeNet + "," + "beforeNetused:" 
          + beforeNetUsed
          + "," + "beforeEnergyLimit:" + beforeEnergyLimit + "," + "beforeEnergyUsed:" 
          + beforeEnergyUsed 
          + "," + "beforeFreeNetUsed" + beforeFreeNetUsed + "," + "energyUsageTotal:" 
          + energyUsageTotal 
          + "," + "fee:" + fee + "," + "energyFee:" + energyFee + "," + "netUsed:" + netUsed + "," 
          + "energyUsed:" + energyUsed + "," + "netFee:" + netFee + "," + "afterBalance:" 
          + afterBalance + "," + "afterEnergyLimit:" + afterEnergyLimit + ","
          + "afterEnergyUsed:" + afterEnergyUsed + "," + "afterFreeNetUsed:" + afterFreeNetUsed 
          + "," + "afterFreeNet:" + afterFreeNet + "," + "afterNetLimit:" + afterNetLimit + "," 
          + "afterNetUsed:" + afterNetUsed + "," + txid + "," + testKeyForAssetIssue016);
      bw.newLine();
      bw.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

}



