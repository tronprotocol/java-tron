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
public class ContractScenario005 {

  //testng001、testng002、testng003、testng004
  private final String testKey002 =
      "FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contract005Address = ecKey1.getAddress();
  String contract005Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(contract005Key);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    Assert.assertTrue(PublicMethed.sendcoin(contract005Address,20000000L,fromAddress,
        testKey002,blockingStubFull));
    logger.info(Long.toString(PublicMethed.queryAccount(contract005Key,blockingStubFull)
        .getBalance()));
    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(contract005Address, 1000000L,
        3,1,contract005Key,blockingStubFull));
    Assert.assertTrue(PublicMethed.buyStorage(5000000L,contract005Address,contract005Key,
        blockingStubFull));

  }

  @Test(enabled = true)
  public void deployIcoContract() {
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(contract005Address,
        blockingStubFull);
    Long energyLimit = accountResource.getEnergyLimit();
    Long storageLimit = accountResource.getStorageLimit();
    Long energyUsage = accountResource.getEnergyUsed();
    Long storageUsage = accountResource.getStorageUsed();

    logger.info("before energy limit is " + Long.toString(energyLimit));
    logger.info("before energy usage is " + Long.toString(energyUsage));
    logger.info("before storage limit is " + Long.toString(storageLimit));
    logger.info("before storage usaged is " + Long.toString(storageUsage));
    Long maxFeeLimit = 5000000L;
    String contractName = "ICO";
    String code = "60c0604052600660808190527f54726f6e6978000000000000000000000000000000000000000000000000000060a090815261003e916000919061013c565b506040805180820190915260038082527f545258000000000000000000000000000000000000000000000000000000000060209092019182526100839160019161013c565b506006600281905560006005558054600160a860020a03191690553480156100aa57600080fd5b50604051602080610abc83398101604081815291516006805461010060a860020a031916336101000217905567016345785d8a00006005819055600160a060020a03821660008181526003602090815286822084905592855294519294909390927fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef929181900390910190a3506101d7565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f1061017d57805160ff19168380011785556101aa565b828001600101855582156101aa579182015b828111156101aa57825182559160200191906001019061018f565b506101b69291506101ba565b5090565b6101d491905b808211156101b657600081556001016101c0565b90565b6108d6806101e66000396000f3006080604052600436106100cf5763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166306fdde0381146100d457806307da68f51461015e578063095ea7b31461017557806318160ddd146101ad57806323b872dd146101d4578063313ce567146101fe57806342966c681461021357806370a082311461022b57806375f12b211461024c57806395d89b4114610261578063a9059cbb14610276578063be9a65551461029a578063c47f0027146102af578063dd62ed3e14610308575b600080fd5b3480156100e057600080fd5b506100e961032f565b6040805160208082528351818301528351919283929083019185019080838360005b8381101561012357818101518382015260200161010b565b50505050905090810190601f1680156101505780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b34801561016a57600080fd5b506101736103bd565b005b34801561018157600080fd5b50610199600160a060020a03600435166024356103e5565b604080519115158252519081900360200190f35b3480156101b957600080fd5b506101c261049e565b60408051918252519081900360200190f35b3480156101e057600080fd5b50610199600160a060020a03600435811690602435166044356104a4565b34801561020a57600080fd5b506101c26105c1565b34801561021f57600080fd5b506101736004356105c7565b34801561023757600080fd5b506101c2600160a060020a036004351661065e565b34801561025857600080fd5b50610199610670565b34801561026d57600080fd5b506100e9610679565b34801561028257600080fd5b50610199600160a060020a03600435166024356106d3565b3480156102a657600080fd5b5061017361079d565b3480156102bb57600080fd5b506040805160206004803580820135601f81018490048402850184019095528484526101739436949293602493928401919081908401838280828437509497506107c29650505050505050565b34801561031457600080fd5b506101c2600160a060020a03600435811690602435166107f2565b6000805460408051602060026001851615610100026000190190941693909304601f810184900484028201840190925281815292918301828280156103b55780601f1061038a576101008083540402835291602001916103b5565b820191906000526020600020905b81548152906001019060200180831161039857829003601f168201915b505050505081565b6006546101009004600160a060020a031633146103d657fe5b6006805460ff19166001179055565b60065460009060ff16156103f557fe5b3315156103fe57fe5b81158061042c5750336000908152600460209081526040808320600160a060020a0387168452909152902054155b151561043757600080fd5b336000818152600460209081526040808320600160a060020a03881680855290835292819020869055805186815290519293927f8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b925929181900390910190a350600192915050565b60055481565b60065460009060ff16156104b457fe5b3315156104bd57fe5b600160a060020a0384166000908152600360205260409020548211156104e257600080fd5b600160a060020a038316600090815260036020526040902054828101101561050957600080fd5b600160a060020a038416600090815260046020908152604080832033845290915290205482111561053957600080fd5b600160a060020a03808416600081815260036020908152604080832080548801905593881680835284832080548890039055600482528483203384528252918490208054879003905583518681529351929391927fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef9281900390910190a35060019392505050565b60025481565b336000908152600360205260409020548111156105e357600080fd5b336000818152600360209081526040808320805486900390558280527f3617319a054d772f909f7c479a2cebe5066e836a939412e32403c99029b92eff805486019055805185815290519293927fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef929181900390910190a350565b60036020526000908152604090205481565b60065460ff1681565b60018054604080516020600284861615610100026000190190941693909304601f810184900484028201840190925281815292918301828280156103b55780601f1061038a576101008083540402835291602001916103b5565b60065460009060ff16156106e357fe5b3315156106ec57fe5b3360009081526003602052604090205482111561070857600080fd5b600160a060020a038316600090815260036020526040902054828101101561072f57600080fd5b33600081815260036020908152604080832080548790039055600160a060020a03871680845292819020805487019055805186815290519293927fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef929181900390910190a350600192915050565b6006546101009004600160a060020a031633146107b657fe5b6006805460ff19169055565b6006546101009004600160a060020a031633146107db57fe5b80516107ee90600090602084019061080f565b5050565b600460209081526000928352604080842090915290825290205481565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f1061085057805160ff191683800117855561087d565b8280016001018555821561087d579182015b8281111561087d578251825591602001919060010190610862565b5061088992915061088d565b5090565b6108a791905b808211156108895760008155600101610893565b905600a165627a7a72305820d00bcb788ca406de94859b8bc4bda50c3c65ca67e1217ccccee92f59a92ae5e20029";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"checkGoalReached\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"deadline\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"beneficiary\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"tokenReward\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"\",\"type\":\"address\"}],\"name\":\"balanceOf\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"fundingGoal\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"amountRaised\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"price\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"safeWithdrawal\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"name\":\"ifSuccessfulSendTo\",\"type\":\"address\"},{\"name\":\"fundingGoalInEthers\",\"type\":\"uint256\"},{\"name\":\"durationInMinutes\",\"type\":\"uint256\"},{\"name\":\"finneyCostOfEachToken\",\"type\":\"uint256\"},{\"name\":\"addressOfTokenUsedAsReward\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"},{\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"fallback\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"recipient\",\"type\":\"address\"},{\"indexed\":false,\"name\":\"totalAmountRaised\",\"type\":\"uint256\"}],\"name\":\"GoalReached\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"backer\",\"type\":\"address\"},{\"indexed\":false,\"name\":\"amount\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"isContribution\",\"type\":\"bool\"}],\"name\":\"FundTransfer\",\"type\":\"event\"}]";
    byte[] contractAddress = PublicMethed.deployContract(contractName,abi,code,"",maxFeeLimit,
        0L, 100,null,contract005Key,contract005Address,blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress,blockingStubFull);

    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
    //logger.info(smartContract.getName());
    //logger.info(smartContract.getAbi().toString());
    accountResource = PublicMethed.getAccountResource(contract005Address,blockingStubFull);
    energyLimit = accountResource.getEnergyLimit();
    storageLimit = accountResource.getStorageLimit();
    energyUsage = accountResource.getEnergyUsed();
    storageUsage = accountResource.getStorageUsed();
    Assert.assertTrue(storageUsage > 0);
    Assert.assertTrue(storageLimit > 0);
    Assert.assertTrue(energyLimit > 0);
    Assert.assertTrue(energyUsage > 0);

    logger.info("after energy limit is " + Long.toString(energyLimit));
    logger.info("after energy usage is " + Long.toString(energyUsage));
    logger.info("after storage limit is " + Long.toString(storageLimit));
    logger.info("after storage usaged is " + Long.toString(storageUsage));
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


