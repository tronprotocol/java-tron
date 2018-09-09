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
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
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
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  String contractName;
  String code;
  String abi;
  byte [] contractAddress;

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

  }

  @Test(enabled = true)
  public void testRangeOfFeeLimit() {
    //Now the feelimit range is 0-1000000000,including 0 and 1000000000
    Assert.assertTrue(PublicMethed.sendcoin(linkage007Address,2000000000L,fromAddress,
        testKey002,blockingStubFull));
    contractName = "testRangeOfFeeLimit";
    code = "60806040526000805561026c806100176000396000f3006080604052600436106100565763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166306661abd811461005b5780631548567714610082578063399ae724146100a8575b600080fd5b34801561006757600080fd5b506100706100cc565b60408051918252519081900360200190f35b6100a673ffffffffffffffffffffffffffffffffffffffff600435166024356100d2565b005b6100a673ffffffffffffffffffffffffffffffffffffffff600435166024356101af565b60005481565b80600054101561017257600080546001018155604080517f1548567700000000000000000000000000000000000000000000000000000000815273ffffffffffffffffffffffffffffffffffffffff8516600482015260248101849052905130926315485677926044808201939182900301818387803b15801561015557600080fd5b505af1158015610169573d6000803e3d6000fd5b505050506100d2565b8060005414156101ab5760405173ffffffffffffffffffffffffffffffffffffffff83169060009060149082818181858883f150505050505b5050565b6000808055604080517f1548567700000000000000000000000000000000000000000000000000000000815273ffffffffffffffffffffffffffffffffffffffff8516600482015260248101849052905130926315485677926044808201939182900301818387803b15801561022457600080fd5b505af1158015610238573d6000803e3d6000fd5b5050505050505600a165627a7a72305820ecdc49ccf0dea5969829debf8845e77be6334f348e9dcaeabf7e98f2d6c7f5270029";
    abi = "[{\"constant\":true,\"inputs\":[],\"name\":\"count\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"addr\",\"type\":\"address\"},{\"name\":\"max\",\"type\":\"uint256\"}],\"name\":\"hack\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"addr\",\"type\":\"address\"},{\"name\":\"max\",\"type\":\"uint256\"}],\"name\":\"init\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";

    //When the feelimit is large, the deploy will be failed.
    String txid = PublicMethed.deployContractAndGetTransactionInfoById(contractName,abi,code,
        "",maxFeeLimit + 1, 0L, 100,null,linkage007Key,
        linkage007Address,blockingStubFull);
    Optional<TransactionInfo> infoById;
    Assert.assertTrue(txid == null);

    //When the feelimit is 0, the deploy will be failed.
    txid = PublicMethed.deployContractAndGetTransactionInfoById(contractName,abi,code,
        "",0L, 0L, 100,null,linkage007Key,
        linkage007Address,blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid,blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 1);

    //Deploy the contract.
    txid = PublicMethed.deployContractAndGetTransactionInfoById(contractName,abi,code,
        "",maxFeeLimit, 0L, 100,null,linkage007Key,
        linkage007Address,blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid,blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    contractAddress = infoById.get().getContractAddress().toByteArray();
    String initParmes = "\"" + Base58.encode58Check(fromAddress) + "\",\"63\"";

    //When the feelimit is large, the trigger will be failed.
    txid = PublicMethed.triggerContract(contractAddress,
        "init(address,uint256)",initParmes, false,
        1000, maxFeeLimit + 1, linkage007Address, linkage007Key, blockingStubFull);
    Assert.assertTrue(txid == null);

    //When the feelimit is 0, the trigger will be failed
    PublicMethed.triggerContract(contractAddress,
        "init(address,uint256)",initParmes, false,
        1000, 0, linkage007Address, linkage007Key, blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid,blockingStubFull);
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


