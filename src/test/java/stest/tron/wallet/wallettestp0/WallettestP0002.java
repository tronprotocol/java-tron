package stest.tron.wallet.wallettestp0;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.TransactionUtils;



@Slf4j
public class WallettestP0002 {

  private WalletClient walletClient;

  //Devaccount
  private final String testKey001 =
      "8CB4480194192F30907E14B52498F594BD046E21D7C4D8FE866563A6760AC891";
  //Zion
  private final String testKey002 =
      "FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";
  //Sun
  private final String testKey003 =
      "6815B367FDDE637E53E9ADC8E69424E07724333C9A2B973CFA469975E20753FC";

  /*  //Devaccount
  private static final byte[] BACK_ADDRESS =
      Base58.decodeFromBase58Check("TKVyqEJaq8QRPQfWE8s8WPb5c92kanAdLo");
  //Zion
  private static final byte[] fromAddress =
      Base58.decodeFromBase58Check("THph9K2M2nLvkianrMGswRhz5hjSA9fuH7");
  //Sun
  private static final byte[] toAddress =
      Base58.decodeFromBase58Check("TV75jZpdmP2juMe1dRwGrwpV6AMU6mr1EU");*/
  private final byte[] backAddress = PublicMethed.getFinalAddress(testKey001);
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);

  private static final Long AMOUNT = 101L;

  private static final long now = System.currentTimeMillis();
  private static String name = "testAssetIssue_" + Long.toString(now);
  private static final long TotalSupply = now;
  String description = "just-test";
  String url = "https://github.com/tronprotocol/wallet-cli/";

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  //private String fullnode = "39.105.111.178:50051";
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  //private String search_fullnode = Configuration.getByPath("testng.conf")
  // .getStringList("fullnode.ip.list").get(1);

  public static void main(String[] args) {
    logger.info("test man.");
  }

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass
  public void beforeClass() {
    logger.info("this is before class");
    walletClient = new WalletClient(testKey002);
    walletClient.init(0);

    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = false)
  public void testAssetIssue() {

    ByteString addressBS1 = ByteString.copyFrom(fromAddress);
    Protocol.Account request1 = Protocol.Account.newBuilder().setAddress(addressBS1).build();
    GrpcAPI.AssetIssueList assetIssueList1 = blockingStubFull
        .getAssetIssueByAccount(request1);
    Optional<GrpcAPI.AssetIssueList> queryAssetByAccount = Optional.ofNullable(assetIssueList1);
    if (queryAssetByAccount.get().getAssetIssueCount() == 0) {
      //Create a new AssetIssue
      Assert.assertTrue(PublicMethed.createAssetIssue(fromAddress, name, TotalSupply, 1, 100,
          now + 900000, now + 10000000000L, 1,
          description, url, 10000L,10000L,1L,
          1L, testKey002,blockingStubFull));
    } else {
      logger.info("This account already create an assetisue");
      logger.info(Integer.toString(queryAssetByAccount.get().getAssetIssueCount()));
      Optional<GrpcAPI.AssetIssueList> queryAssetByAccount1 = Optional.ofNullable(assetIssueList1);
      name = ByteArray.toStr(queryAssetByAccount1.get().getAssetIssue(0).getName().toByteArray());
    }
  }

  /*  @Test(enabled = true)
  public void testGetAssetIssueByName() {
    Contract.AssetIssueContract ret = walletClient.getAssetIssueByName(name);
    Assert.assertTrue(ret.getOwnerAddress() != null);
  }*/

  @Test(enabled = false)
  public void testTransferAsset() {
    //byte assertName[] = name.getBytes();
    //logger.info(Long.toString(walletClient.getAssetIssueByName(name).getTotalSupply()));
    Assert.assertTrue(PublicMethed.freezeBalance(fromAddress, 10000000, 3, testKey002,
        blockingStubFull));
    Boolean ret = walletClient.transferAsset(toAddress, name.getBytes(), AMOUNT);

    //logger.info(Long.toString(walletClient.getAssetIssueByName(name).getTotalSupply()));
    Assert.assertTrue(ret);
    logger.info("this is TestTransferAsset");
  }


  /*  @Test(enabled = true)
  public void testGetAssetIssueByAccount() {
    Optional<GrpcAPI.AssetIssueList> result = walletClient.getAssetIssueByAccount(fromAddress);
    logger.info("client  " + Integer.toString(result.get().getAssetIssueCount()));
    //logger.info(Integer.toString(result.get().getAssetIssue(0).getNum()));
    Assert.assertTrue(result.get().getAssetIssueCount() == 1);
  }*/

  @Test(enabled = false)
  public void testGetAssetIssueList() {
    Optional<GrpcAPI.AssetIssueList> result = walletClient.getAssetIssueList();
    GrpcAPI.AssetIssueList getAssetIssueList = result.get();
    logger.info(Integer.toString(result.get().getAssetIssueCount()));
    Assert.assertTrue(result.get().getAssetIssueCount() > 0);
  }

  /*  @Test(enabled = false)
  public void testGetAssetIssueByTimestamp() {
    long now = System.currentTimeMillis();
    Optional<GrpcAPI.AssetIssueList> result = walletClient.getAssetIssueListByTimestamp(now);
    logger.info(Integer.toString(result.get().getAssetIssueCount()));
    Boolean foundThisName = false;
    for (int j = 0; j < result.get().getAssetIssueCount(); j++) {
      logger.info(ByteArray.toStr(result.get().getAssetIssue(j).getName().toByteArray()));
      if (result.get().getAssetIssue(j).getTotalSupply() == TotalSupply) {
        foundThisName = true;
      }
    }
    Assert.assertTrue(foundThisName);
    logger.info("This is TestGetAssetIssueByTimestamp.");
  }*/

  @Test(enabled = false)
  public void testParticipateAssetIssue() {
    Contract.ParticipateAssetIssueContract result = walletClient.participateAssetIssueContract(
        toAddress, name.getBytes(), fromAddress, AMOUNT);

    Assert.assertTrue(result.getAmount() == AMOUNT);
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  public Boolean createAssetIssue(byte[] address, String name, Long totalSupply, Integer trxNum,
      Integer icoNum, Long startTime, Long endTime, Integer voteScore, String description,
      String url, Long fronzenAmount, Long frozenDay, String priKey) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
    try {
      Contract.AssetIssueContract.Builder builder = Contract.AssetIssueContract.newBuilder();
      builder.setOwnerAddress(ByteString.copyFrom(address));
      builder.setName(ByteString.copyFrom(name.getBytes()));
      builder.setTotalSupply(TotalSupply);
      builder.setTrxNum(trxNum);
      builder.setNum(icoNum);
      builder.setStartTime(startTime);
      builder.setEndTime(endTime);
      builder.setVoteScore(voteScore);
      builder.setDescription(ByteString.copyFrom(description.getBytes()));
      builder.setUrl(ByteString.copyFrom(url.getBytes()));
      Contract.AssetIssueContract.FrozenSupply.Builder frozenBuilder =
          Contract.AssetIssueContract.FrozenSupply.newBuilder();
      frozenBuilder.setFrozenAmount(fronzenAmount);
      frozenBuilder.setFrozenDays(frozenDay);
      builder.addFrozenSupply(0, frozenBuilder);

      Protocol.Transaction transaction = blockingStubFull.createAssetIssue(builder.build());
      if (transaction == null || transaction.getRawData().getContractCount() == 0) {
        return false;
      }
      transaction = signTransaction(ecKey, transaction);
      GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
      if (response.getResult() == false) {
        return false;
      } else {
        logger.info(name);
        return true;
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      return false;
    }
  }

  private Protocol.Transaction signTransaction(ECKey ecKey, Protocol.Transaction transaction) {
    if (ecKey == null || ecKey.getPrivKey() == null) {
      logger.warn("Warning: Can't sign,there is no private key !!");
      return null;
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    return TransactionUtils.sign(transaction, ecKey);
  }

}
