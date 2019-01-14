package stest.tron.wallet.assetissue;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.core.Wallet;
import org.tron.protos.Contract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.TransactionUtils;

@Slf4j
public class WalletTestAssetIssue004 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);


  //testng001、testng002、testng003、testng004
  /*  private static final byte[] fromAddress = Base58
      .decodeFromBase58Check("THph9K2M2nLvkianrMGswRhz5hjSA9fuH7");*/
  private static final byte[] NO_ASSET_ADDRESS = Base58
      .decodeFromBase58Check("27XeWZUtufGk8jdjF3m1tuPnnRqqKgzS3pT");
  private static final byte[] INVALID_ADDRESS = Base58
      .decodeFromBase58Check("27cu1ozb4mX3m2afY68FSAqn3HmMp815d48");

  private static final long now = System.currentTimeMillis();
  private static final String name = "testAssetIssue004_" + Long.toString(now);
  private static final long totalSupply = now;
  String description = "just-test";
  String url = "https://github.com/tronprotocol/wallet-cli/";

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

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

    ByteString addressBs1 = ByteString.copyFrom(fromAddress);
    Account request1 = Account.newBuilder().setAddress(addressBs1).build();
    GrpcAPI.AssetIssueList assetIssueList1 = blockingStubFull
        .getAssetIssueByAccount(request1);
    Optional<GrpcAPI.AssetIssueList> queryAssetByAccount = Optional.ofNullable(assetIssueList1);
    if (queryAssetByAccount.get().getAssetIssueCount() == 0) {
      Long start = System.currentTimeMillis() + 2000;
      Long end = System.currentTimeMillis() + 1000000000;
      //Create a new asset issue
      Assert.assertTrue(PublicMethed.createAssetIssue(fromAddress, name, totalSupply, 6, 1000,
          start, end, 2, description, url,10000L,10000L,
          1L, 1L, testKey002,blockingStubFull));
    } else {
      logger.info("This account already create an assetisue");
    }

  }

  @Test(enabled = true)
  public void testGetAssetIssueByAccount() {
    ByteString addressBs = ByteString.copyFrom(fromAddress);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    GrpcAPI.AssetIssueList assetIssueList = blockingStubFull
        .getAssetIssueByAccount(request);
    Optional<GrpcAPI.AssetIssueList> queryAssetIssueByAccount = Optional.ofNullable(assetIssueList);
    logger.info(Integer.toString(queryAssetIssueByAccount.get().getAssetIssueCount()));
    Assert.assertTrue(queryAssetIssueByAccount.get().getAssetIssueCount() == 1);
    for (Integer j = 0; j < queryAssetIssueByAccount.get().getAssetIssueCount(); j++) {
      if (queryAssetIssueByAccount.get().getAssetIssue(j).getTotalSupply() == totalSupply) {
        Assert.assertTrue(queryAssetIssueByAccount.isPresent());
        //Assert.assertTrue(queryAssetIssueByAccount.get().getAssetIssue(j).getDecayRatio() > 0);
        Assert.assertTrue(queryAssetIssueByAccount.get().getAssetIssue(j).getTrxNum() > 0);
        Assert.assertTrue(queryAssetIssueByAccount.get().getAssetIssue(j).getVoteScore() > 0);
        Assert.assertFalse(queryAssetIssueByAccount.get().getAssetIssue(j).getUrl().isEmpty());
        logger.info("TestGetAssetIssueByAccount ok!");
      }
    }

    //No exception when the address didn't create asset issue.
    ByteString addressBS1 = ByteString.copyFrom(NO_ASSET_ADDRESS);
    Account request1 = Account.newBuilder().setAddress(addressBS1).build();
    GrpcAPI.AssetIssueList assetIssueList1 = blockingStubFull
        .getAssetIssueByAccount(request1);
    Optional<GrpcAPI.AssetIssueList> queryNoAssetByAccount = Optional.ofNullable(assetIssueList1);
    Assert.assertTrue(queryNoAssetByAccount.get().getAssetIssueCount() == 0);
    logger.info("No asset account queryed nothing");

    //No exception when the address is invalid.
    addressBS1 = ByteString.copyFrom(INVALID_ADDRESS);
    request1 = Account.newBuilder().setAddress(addressBS1).build();
    assetIssueList1 = blockingStubFull
        .getAssetIssueByAccount(request1);
    queryNoAssetByAccount = Optional.ofNullable(assetIssueList1);
    Assert.assertTrue(queryNoAssetByAccount.get().getAssetIssueCount() == 0);
    logger.info("No asset account queryed nothing");

  }

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  public Boolean createAssetIssue(byte[] address, String name, Long totalSupply, Integer trxNum,
      Integer icoNum, Long startTime, Long endTime,
      Integer voteScore, String description, String url, Long fronzenAmount, Long frozenDay,
      String priKey) {
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
      builder.setTotalSupply(totalSupply);
      builder.setTrxNum(trxNum);
      builder.setNum(icoNum);
      builder.setStartTime(startTime);
      builder.setEndTime(endTime);
      builder.setVoteScore(voteScore);
      builder.setDescription(ByteString.copyFrom(description.getBytes()));
      builder.setUrl(ByteString.copyFrom(url.getBytes()));
      builder.setFreeAssetNetLimit(20000);
      builder.setPublicFreeAssetNetLimit(20000);
      Contract.AssetIssueContract.FrozenSupply.Builder
          frozenBuilder = Contract.AssetIssueContract.FrozenSupply
          .newBuilder();
      frozenBuilder.setFrozenAmount(fronzenAmount);
      frozenBuilder.setFrozenDays(frozenDay);
      builder.addFrozenSupply(0, frozenBuilder);

      Transaction transaction = blockingStubFull.createAssetIssue(builder.build());
      if (transaction == null || transaction.getRawData().getContractCount() == 0) {
        return false;
      }
      transaction = signTransaction(ecKey, transaction);
      Return response = blockingStubFull.broadcastTransaction(transaction);
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

  public Account queryAccount(ECKey ecKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    byte[] address;
    if (ecKey == null) {
      String pubKey = loadPubKey(); //04 PubKey[128]
      if (StringUtils.isEmpty(pubKey)) {
        logger.warn("Warning: QueryAccount failed, no wallet address !!");
        return null;
      }
      byte[] pubKeyAsc = pubKey.getBytes();
      byte[] pubKeyHex = Hex.decode(pubKeyAsc);
      ecKey = ECKey.fromPublicOnly(pubKeyHex);
    }
    return grpcQueryAccount(ecKey.getAddress(), blockingStubFull);
  }

  public static String loadPubKey() {
    char[] buf = new char[0x100];
    return String.valueOf(buf, 32, 130);
  }

  public byte[] getAddress(ECKey ecKey) {
    return ecKey.getAddress();
  }

  public Account grpcQueryAccount(byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccount(request);
  }

  public Block getBlock(long blockNum, WalletGrpc.WalletBlockingStub blockingStubFull) {
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(blockNum);
    return blockingStubFull.getBlockByNum(builder.build());

  }

  private Transaction signTransaction(ECKey ecKey, Transaction transaction) {
    if (ecKey == null || ecKey.getPrivKey() == null) {
      logger.warn("Warning: Can't sign,there is no private key !!");
      return null;
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    return TransactionUtils.sign(transaction, ecKey);
  }

  public boolean transferAsset(byte[] to, byte[] assertName, long amount, byte[] address,
      String priKey) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    Contract.TransferAssetContract.Builder builder = Contract.TransferAssetContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsName = ByteString.copyFrom(assertName);
    ByteString bsOwner = ByteString.copyFrom(address);
    builder.setToAddress(bsTo);
    builder.setAssetName(bsName);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    Contract.TransferAssetContract contract = builder.build();
    Transaction transaction = blockingStubFull.transferAsset(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }
    transaction = signTransaction(ecKey, transaction);
    Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      return false;
    } else {
      Account search = queryAccount(ecKey, blockingStubFull);
      return true;
    }

  }
}


