package stest.tron.wallet.contract.scenario;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
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
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractScenario010 {

  //testng001、testng002、testng003、testng004
  private final String testKey002 =
      "FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contract009Address = ecKey1.getAddress();
  String contract009Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(contract009Key);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    Assert.assertTrue(PublicMethed.sendcoin(contract009Address,600000000L,fromAddress,
        testKey002,blockingStubFull));
    logger.info(Long.toString(PublicMethed.queryAccount(contract009Key,blockingStubFull)
        .getBalance()));

    /*    Assert.assertTrue(PublicMethed.buyStorage(5000000L,contract009Address,contract009Key,
        blockingStubFull));*/

  }

  @Test(enabled = true)
  public void deployContainLibraryContract() {
    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(contract009Address, 10000000L,
        3,1,contract009Key,blockingStubFull));
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(contract009Address,
        blockingStubFull);
    Long energyLimit = accountResource.getEnergyLimit();
    //Long storageLimit = accountResource.getStorageLimit();
    Long energyUsage = accountResource.getEnergyUsed();
    //Long storageUsage = accountResource.getStorageUsed();

    logger.info("before energy limit is " + Long.toString(energyLimit));
    logger.info("before energy usage is " + Long.toString(energyUsage));
    //logger.info("before storage limit is " + Long.toString(storageLimit));
    //logger.info("before storage usaged is " + Long.toString(storageUsage));
    Long maxFeeLimit = 200000000L;
    String contractName = "Tron_ERC721_Token";
    String code = "608060405234801561001057600080fd5b50610731806100206000396000f3006080604052600436106100a35763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166306fdde0381146100a8578063095ea7b31461013257806318160ddd146101585780632f745c591461017f5780636352211e146101a35780636914db60146101d757806370a08231146101ef57806395d89b4114610210578063a9059cbb14610225578063b2e6ceeb14610249575b600080fd5b3480156100b457600080fd5b506100bd610261565b6040805160208082528351818301528351919283929083019185019080838360005b838110156100f75781810151838201526020016100df565b50505050905090810190601f1680156101245780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b34801561013e57600080fd5b50610156600160a060020a0360043516602435610298565b005b34801561016457600080fd5b5061016d61032d565b60408051918252519081900360200190f35b34801561018b57600080fd5b5061016d600160a060020a0360043516602435610336565b3480156101af57600080fd5b506101bb60043561035e565b60408051600160a060020a039092168252519081900360200190f35b3480156101e357600080fd5b506100bd600435610397565b3480156101fb57600080fd5b5061016d600160a060020a0360043516610438565b34801561021c57600080fd5b506100bd610453565b34801561023157600080fd5b50610156600160a060020a036004351660243561048a565b34801561025557600080fd5b5061015660043561059d565b60408051808201909152601181527f54726f6e2045524337323120546f6b656e000000000000000000000000000000602082015290565b6102a18161035e565b600160a060020a031633146102b557600080fd5b33600160a060020a03831614156102cb57600080fd5b336000818152600360209081526040808320600160a060020a03871680855290835292819020859055805185815290519293927f8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b925929181900390910190a35050565b64e8d4a5100090565b600160a060020a03919091166000908152600460209081526040808320938352929052205490565b60008181526002602052604081205460ff16151561037b57600080fd5b50600090815260016020526040902054600160a060020a031690565b60008181526005602090815260409182902080548351601f600260001961010060018616150201909316929092049182018490048402810184019094528084526060939283018282801561042c5780601f106104015761010080835404028352916020019161042c565b820191906000526020600020905b81548152906001019060200180831161040f57829003601f168201915b50505050509050919050565b600160a060020a031660009081526020819052604090205490565b60408051808201909152600581527f5437323154000000000000000000000000000000000000000000000000000000602082015290565b6000818152600260205260408120543391849160ff1615156104ab57600080fd5b6104b48461035e565b600160a060020a038481169116146104cb57600080fd5b600160a060020a0383811690831614156104e457600080fd5b600160a060020a03821615156104f957600080fd5b508161050581856106a7565b600160a060020a0381811660008181526020818152604080832080546000190190558883526001808352818420805473ffffffffffffffffffffffffffffffffffffffff19169689169687179055858452838352928190208054909301909255815188815291517fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef9281900390910190a35050505050565b600081815260026020526040812054819060ff1615156105bc57600080fd5b6105c58361035e565b9150339050600160a060020a0382168114156105e057600080fd5b600160a060020a03808316600090815260036020908152604080832093851683529290522054831461061157600080fd5b600160a060020a0382811660008181526020818152604080832080546000190190558783526001808352818420805473ffffffffffffffffffffffffffffffffffffffff19169688169687179055858452838352928190208054909301909255815187815291517fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef9281900390910190a3505050565b60005b600160a060020a0383166000908152600460209081526040808320848452909152902054821461070057600160a060020a03831660009081526004602090815260408083208484529091528120556001016106aa565b5050505600a165627a7a72305820d3ca2ca957b72f4c5028c633a6ad4bafe13572bf949793fabe72e34eb640d2c50029";
    String abi = "[{\"constant\":true,\"inputs\":[],\"name\":\"name\",\"outputs\":[{\"name\":\"name\",\"type\":\"string\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_to\",\"type\":\"address\"},{\"name\":\"_tokenId\",\"type\":\"uint256\"}],\"name\":\"approve\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"totalSupply\",\"outputs\":[{\"name\":\"supply\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"_owner\",\"type\":\"address\"},{\"name\":\"_index\",\"type\":\"uint256\"}],\"name\":\"tokenOfOwnerByIndex\",\"outputs\":[{\"name\":\"tokenId\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"_tokenId\",\"type\":\"uint256\"}],\"name\":\"ownerOf\",\"outputs\":[{\"name\":\"owner\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"_tokenId\",\"type\":\"uint256\"}],\"name\":\"tokenMetadata\",\"outputs\":[{\"name\":\"infoUrl\",\"type\":\"string\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"_owner\",\"type\":\"address\"}],\"name\":\"balanceOf\",\"outputs\":[{\"name\":\"balance\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"symbol\",\"outputs\":[{\"name\":\"symbol\",\"type\":\"string\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_to\",\"type\":\"address\"},{\"name\":\"_tokenId\",\"type\":\"uint256\"}],\"name\":\"transfer\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_tokenId\",\"type\":\"uint256\"}],\"name\":\"takeOwnership\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"name\":\"_from\",\"type\":\"address\"},{\"indexed\":true,\"name\":\"_to\",\"type\":\"address\"},{\"indexed\":false,\"name\":\"_tokenId\",\"type\":\"uint256\"}],\"name\":\"Transfer\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"name\":\"_owner\",\"type\":\"address\"},{\"indexed\":true,\"name\":\"_approved\",\"type\":\"address\"},{\"indexed\":false,\"name\":\"_tokenId\",\"type\":\"uint256\"}],\"name\":\"Approval\",\"type\":\"event\"}]";
    byte[] libraryAddress = PublicMethed.deployContract(contractName,abi,code,"",maxFeeLimit,
        0L, 100,null,contract009Key,contract009Address,blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(libraryAddress,blockingStubFull);

    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
    logger.info(ByteArray.toHexString(smartContract.getContractAddress().toByteArray()));
    //logger.info(smartContract.getName());
    //logger.info(smartContract.getAbi().toString());
    accountResource = PublicMethed.getAccountResource(contract009Address,blockingStubFull);
    energyLimit = accountResource.getEnergyLimit();
    //storageLimit = accountResource.getStorageLimit();
    energyUsage = accountResource.getEnergyUsed();
    //storageUsage = accountResource.getStorageUsed();
    //Assert.assertTrue(storageUsage == 0);
    //Assert.assertTrue(storageLimit > 0);
    Assert.assertTrue(energyLimit > 0);
    Assert.assertTrue(energyUsage > 0);

    logger.info("after energy limit is " + Long.toString(energyLimit));
    logger.info("after energy usage is " + Long.toString(energyUsage));
    //logger.info("after storage limit is " + Long.toString(storageLimit));
    //logger.info("after storage usaged is " + Long.toString(storageUsage));
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


