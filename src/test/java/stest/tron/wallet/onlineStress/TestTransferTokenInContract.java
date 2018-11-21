package stest.tron.wallet.onlineStress;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AssetIssueList;
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
public class TestTransferTokenInContract {
  private AtomicLong count = new AtomicLong();
  private AtomicLong errorCount = new AtomicLong();
  private long startTime = System.currentTimeMillis();

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  private static final long TotalSupply = 1000000L;

  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {

    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
  }

  private static int randomInt(int minInt, int maxInt) {
    return (int) Math.round(Math.random() * (maxInt - minInt) + minInt);
  }

  public ByteString createAssetissue(byte[] devAddress, String devKey, String tokenName) {

    ByteString assetAccountId = null;
    ByteString addressBS1 = ByteString.copyFrom(devAddress);
    Account request1 = Account.newBuilder().setAddress(addressBS1).build();
    GrpcAPI.AssetIssueList assetIssueList1 = blockingStubFull
        .getAssetIssueByAccount(request1);
    Optional<AssetIssueList> queryAssetByAccount = Optional.ofNullable(assetIssueList1);
    if (queryAssetByAccount.get().getAssetIssueCount() == 0) {
      Long start = System.currentTimeMillis() + 2000;
      Long end = System.currentTimeMillis() + 1000000000;

      logger.info("The token name: " + tokenName);

      //Create a new AssetIssue success.
      Assert.assertTrue(PublicMethed.createAssetIssue(devAddress, tokenName, TotalSupply, 1,
          100, start, end, 1, description, url, 10000L,10000L,
          1L,1L, devKey, blockingStubFull));

      Account getAssetIdFromThisAccount = PublicMethed.queryAccount(devAddress, blockingStubFull);
      assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();
    } else {
      logger.info("This account already create an assetisue");
      Optional<GrpcAPI.AssetIssueList> queryAssetByAccount1 = Optional.ofNullable(assetIssueList1);
      tokenName = ByteArray.toStr(queryAssetByAccount1.get().getAssetIssue(0)
          .getName().toByteArray());
    }
    return assetAccountId;
  }

  @Test(enabled = true, threadPoolSize = 10, invocationCount = 10)
  public void continueRun() {

    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] dev001Address = ecKey1.getAddress();
    String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    ECKey ecKey2 = new ECKey(Utils.getRandom());
    byte[] user001Address = ecKey2.getAddress();
    String user001Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    Assert
        .assertTrue(PublicMethed.sendcoin(dev001Address, 2048000000, fromAddress,
            testKey002, blockingStubFull));
    Assert
        .assertTrue(PublicMethed.sendcoin(user001Address, 4048000000L, fromAddress,
            testKey002, blockingStubFull));

    // freeze balance
    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(dev001Address, 204800000,
        3, 1, dev001Key, blockingStubFull));

    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(user001Address, 2048000000,
        3, 1, user001Key, blockingStubFull));

    String tokenName = "testAI_" + randomInt(10000, 90000);
    ByteString tokenId = createAssetissue(user001Address, user001Key, tokenName);

    // devAddress transfer token to A
    PublicMethed.transferAsset(dev001Address, tokenId.toByteArray(), 101, user001Address,
        user001Key, blockingStubFull);

    // deploy transferTokenContract
    String contractName = "transferTokenContract";
    String code = "608060405260e2806100126000396000f300608060405260043610603e5763ffffffff7c01000000"
        + "000000000000000000000000000000000000000000000000006000350416633be9ece781146043575b600080"
        + "fd5b606873ffffffffffffffffffffffffffffffffffffffff60043516602435604435606a565b005b604051"
        + "73ffffffffffffffffffffffffffffffffffffffff84169082156108fc029083908590600081818185878a8a"
        + "d094505050505015801560b0573d6000803e3d6000fd5b505050505600a165627a7a723058200ba246bdb58b"
        + "e0f221ad07e1b19de843ab541150b329ddd01558c2f1cefe1e270029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"},"
        + "{\"name\":\"id\",\"type\":\"trcToken\"},{\"name\":\"amount\",\"type\":\"uint256\"}],"
        + "\"name\":\"TransferTokenTo\",\"outputs\":[],\"payable\":true,\"stateMutability\":"
        + "\"payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\""
        + ":\"payable\",\"type\":\"constructor\"}]";
    byte[] transferTokenContractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit,
            0L, 100, 10000, tokenId.toStringUtf8(),
            100, null, dev001Key, dev001Address,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    // deploy receiveTokenContract
    contractName = "recieveTokenContract";
    code = "60806040526000805560c5806100166000396000f30060806040526004361060485763ffffffff7c0100000"
        + "00000000000000000000000000000000000000000000000000060003504166362548c7b8114604a578063890"
        + "eba68146050575b005b6048608c565b348015605b57600080fd5b50d38015606757600080fd5b50d28015607"
        + "357600080fd5b50607a6093565b60408051918252519081900360200190f35b6001600055565b60005481560"
        + "0a165627a7a723058204c4f1bb8eca0c4f1678cc7cc1179e03d99da2a980e6792feebe4d55c89c022830029";
    abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"setFlag\",\"outputs\":[],\"payable\":true,"
        + "\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],"
        + "\"name\":\"flag\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,"
        + "\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,"
        + "\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,"
        + "\"stateMutability\":\"payable\",\"type\":\"fallback\"}]";
    byte[] receiveTokenContractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit,
            0L, 100, null, dev001Key, dev001Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    // deploy tokenBalanceContract
    contractName = "tokenBalanceContract";
    code = "608060405260ff806100126000396000f30060806040526004361060485763ffffffff7c010000000000000"
        + "0000000000000000000000000000000000000000000600035041663a730416e8114604d578063b69ef8a8146"
        + "081575b600080fd5b606f73ffffffffffffffffffffffffffffffffffffffff6004351660243560ab565b604"
        + "08051918252519081900360200190f35b348015608c57600080fd5b50d38015609857600080fd5b50d280156"
        + "0a457600080fd5b50606f60cd565b73ffffffffffffffffffffffffffffffffffffffff90911690d16000908"
        + "15590565b600054815600a165627a7a723058202b6235122df66c062c2e723ad58a9fea93346f3bc19898971"
        + "8f211aa1dbd2d7a0029";
    abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"},"
        + "{\"name\":\"tokenId\",\"type\":\"trcToken\"}],\"name\":\"getTokenBalnce\",\"outputs\":"
        + "[{\"name\":\"b\",\"type\":\"uint256\"}],\"payable\":true,\"stateMutability\":"
        + "\"payable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":"
        + "\"balance\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,"
        + "\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,"
        + "\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
    byte[] tokenBalanceContractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit,
            0L, 100, null, dev001Key, dev001Address, blockingStubFull);

    // devAddress transfer token to userAddress
    PublicMethed.transferAsset(user001Address, tokenId.toByteArray(), 100, dev001Address, dev001Key,
        blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull1);

    while (true) {
      count.getAndAdd(4);
      if (count.get() % 500 == 0) {
        long cost = (System.currentTimeMillis() - startTime) / 1000;
        logger.info("Count:" + count.get() + ", cost:" + cost
            + ", avg:" + count.get() / cost + ", errCount:" + errorCount);
      }
      // user trigger A to transfer token to B
      String param =
          "\"" + Base58.encode58Check(receiveTokenContractAddress) + "\",\"" + tokenId
              .toStringUtf8()
          + "\",\"5\"";

      String triggerTxid = PublicMethed.triggerContract(transferTokenContractAddress,
          "TransferTokenTo(address,trcToken,uint256)",
          param, false, 0, 100000000L, tokenId.toStringUtf8(),
          10, user001Address, user001Key,
          blockingStubFull);

      Optional<TransactionInfo> infoById = PublicMethed
          .getTransactionInfoById(triggerTxid, blockingStubFull);

      if (infoById.get().getResultValue() != 0) {
        errorCount.incrementAndGet();
      }

      // user trigger A to transfer token to devAddress
      param =
          "\"" + Base58.encode58Check(dev001Address) + "\",\"" + tokenId.toStringUtf8()
              + "\",\"5\"";

      triggerTxid = PublicMethed.triggerContract(transferTokenContractAddress,
          "TransferTokenTo(address,trcToken,uint256)",
          param, false, 0, 100000000L, user001Address,
          user001Key, blockingStubFull);

      infoById = PublicMethed
          .getTransactionInfoById(triggerTxid, blockingStubFull);

      if (infoById.get().getResultValue() != 0) {
        errorCount.incrementAndGet();
      }

      // user trigger C to get B's token balance
      param =
          "\"" + Base58.encode58Check(receiveTokenContractAddress) + "\",\"" + tokenId
              .toStringUtf8()
              + "\"";

      triggerTxid = PublicMethed
          .triggerContract(tokenBalanceContractAddress, "getTokenBalnce(address,trcToken)",
              param, false, 0, 1000000000L, user001Address,
              user001Key, blockingStubFull);

      infoById = PublicMethed
          .getTransactionInfoById(triggerTxid, blockingStubFull);

      if (infoById.get().getResultValue() != 0) {
        errorCount.incrementAndGet();
      }

      triggerTxid = PublicMethed.triggerContract(tokenBalanceContractAddress, "balance()",
          "#", false, 0, 1000000000L, user001Address,
          user001Key, blockingStubFull);

      // user trigger C to get devAddress's token balance
      param = "\"" + Base58.encode58Check(dev001Address) + "\",\"" + tokenId.toStringUtf8() + "\"";

      triggerTxid = PublicMethed
          .triggerContract(tokenBalanceContractAddress, "getTokenBalnce(address,trcToken)",
              param, false, 0, 1000000000L, user001Address,
              user001Key, blockingStubFull);

      infoById = PublicMethed
          .getTransactionInfoById(triggerTxid, blockingStubFull);

      if (infoById.get().getResultValue() != 0) {
        errorCount.incrementAndGet();
      }

      triggerTxid = PublicMethed.triggerContract(tokenBalanceContractAddress, "balance()",
          "#", false, 0, 1000000000L, user001Address,
          user001Key, blockingStubFull);
    }
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


