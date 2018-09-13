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
import org.tron.common.utils.ByteArray;
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
public class WalletTestAssetIssue003 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);

  private static final long now = System.currentTimeMillis();
  private static final String name = "testAssetIssue003_" + Long.toString(now);
  private static final String shortname = "a";
  private static final String tooLongName = "qazxswedcvfrtgbnhyujmkiolpoiuytre";
  private static final String chineseAssetIssuename = "中文都名字";
  private static final String tooLongDescription =
      "1qazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqa"
          + "zxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvq"
          + "azxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcv";
  private static final String tooLongUrl =
      "qaswqaswqaswqaswqaswqaswqaswqaswqaswqaswqaswqaswqaswqasw1qazxswedcvqazxswedcv"
          + "qazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedc"
          + "vqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqaz"
          + "xswedcvqazxswedcvqazxswedcvqazxswedcv";
  private static final long totalSupply = now;
  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = false)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    ByteString addressBS1 = ByteString.copyFrom(fromAddress);
    Account request1 = Account.newBuilder().setAddress(addressBS1).build();
    GrpcAPI.AssetIssueList assetIssueList1 = blockingStubFull
        .getAssetIssueByAccount(request1);
    Optional<GrpcAPI.AssetIssueList> queryAssetByAccount = Optional.ofNullable(assetIssueList1);
    if (queryAssetByAccount.get().getAssetIssueCount() == 0) {
      Long start = System.currentTimeMillis() + 100000;
      Long end = System.currentTimeMillis() + 1000000000;
      //Freeze amount is large than total supply, create asset issue failed.
      Assert.assertFalse(PublicMethed.createAssetIssue(toAddress, name, totalSupply, 1, 10,
          start, end, 2, description, url, 10000L,10000L,
          9000000000000000000L, 1L, testKey003,blockingStubFull));
      //Freeze day is 0, create failed
      Assert.assertFalse(PublicMethed.createAssetIssue(toAddress, name, totalSupply, 1, 10,
          start, end, 2, description, url, 10000L,10000L,
          100L, 0L, testKey003,blockingStubFull));
      //Freeze amount is 0, create failed
      Assert.assertFalse(PublicMethed.createAssetIssue(toAddress, name, totalSupply, 1, 10,
          start, end, 2, description, url, 10000L,10000L,
          0L, 1L, testKey003,blockingStubFull));
      //Freeze day is -1, create failed
      Assert.assertFalse(PublicMethed.createAssetIssue(toAddress, name, totalSupply, 1, 10,
          start, end, 2, description, url, 1000L,1000L,
          1000L, -1L, testKey003,blockingStubFull));
      //Freeze amount is -1, create failed
      Assert.assertFalse(PublicMethed.createAssetIssue(toAddress, name, totalSupply, 1, 10,
          start, end, 2, description, url, 10000L,10000L,
          -1L, 1L, testKey003,blockingStubFull));
      //Freeze day is 3653(10 years + 1 day), create failed
      Assert.assertFalse(PublicMethed.createAssetIssue(fromAddress, name, totalSupply, 1, 10,
          start, end, 2, description, url, 10000L,10000L,
          1L, 3653L, testKey002,blockingStubFull));
      //Start time is late than end time.
      Assert.assertFalse(PublicMethed.createAssetIssue(fromAddress, name, totalSupply, 1, 10,
          end, start, 2, description, url, 10000L,10000L,
          1L, 2L, testKey002,blockingStubFull));
      //Start time is early than currently time.
      Assert.assertFalse(PublicMethed.createAssetIssue(fromAddress, name, totalSupply, 1, 10,
          start - 1000000L, end, 2, description, url, 10000L,
          10000L,1L, 2L, testKey002,blockingStubFull));
      //totalSupply is zero.
      Assert.assertFalse(PublicMethed.createAssetIssue(fromAddress, name, 0L, 1, 10,
          start, end, 2, description, url, 10000L,10000L,
          1L, 3652L, testKey002,blockingStubFull));
      //Total supply is -1.
      Assert.assertFalse(PublicMethed.createAssetIssue(fromAddress, name, -1L, 1, 10,
          start, end, 2, description, url, 10000L,10000L,
          1L, 3652L, testKey002,blockingStubFull));
      //TrxNum is zero.
      Assert.assertFalse(PublicMethed.createAssetIssue(fromAddress, name, totalSupply, 0, 10,
          start, end, 2, description, url, 10000L,10000L,
          1L, 3652L, testKey002,blockingStubFull));
      //TrxNum is -1.
      Assert.assertFalse(PublicMethed.createAssetIssue(fromAddress, name, totalSupply, -1, 10,
          start, end, 2, description, url, 10000L,10000L,
          1L, 3652L, testKey002,blockingStubFull));
      //IcoNum is 0.
      Assert.assertFalse(PublicMethed.createAssetIssue(fromAddress, name, totalSupply, 1, 0,
          start, end, 2, description, url, 10000L,10000L,
          1L, 3652L, testKey002,blockingStubFull));
      //IcoNum is -1.
      Assert.assertFalse(PublicMethed.createAssetIssue(fromAddress, name, totalSupply, 1, -1,
          start, end, 2, description, url, 10000L,10000L,
          1L, 3652L, testKey002,blockingStubFull));
      //The asset issue name is null.
      Assert.assertFalse(PublicMethed.createAssetIssue(fromAddress, "", totalSupply, 1, 10,
          start, end, 2, description, url,10000L,10000L,
          1L, 3652L, testKey002,blockingStubFull));
      //The asset issue name is large than 33 char.
      Assert.assertFalse(PublicMethed.createAssetIssue(fromAddress, tooLongName, totalSupply, 1, 10,
          start, end, 2, description, url,10000L,10000L,
          1L, 3652L, testKey002,blockingStubFull));
      //The asset issue name is chinese name.
      Assert.assertFalse(PublicMethed.createAssetIssue(fromAddress, chineseAssetIssuename, totalSupply, 1,
          10, start, end, 2, description, url,10000L,
          10000L,1L, 3652L, testKey002,blockingStubFull));
      //The URL is null.
      Assert.assertFalse(PublicMethed.createAssetIssue(fromAddress, name, totalSupply, 1, 10,
          start, end, 2, description, "",10000L,10000L,
          1L, 3652L, testKey002,blockingStubFull));
      //The URL is too long.
      Assert.assertFalse(PublicMethed.createAssetIssue(fromAddress, name, totalSupply, 1, 10,
          start, end, 2, description, tooLongUrl, 10000L,10000L,
          1L, 3652L, testKey002,blockingStubFull));
      //The description is too long, create failed.
      Assert.assertFalse(PublicMethed.createAssetIssue(fromAddress, name, totalSupply, 1, 10,
          start, end, 2, tooLongDescription, url,10000L,
          10000L,1L, 3652L, testKey002,blockingStubFull));

      //FreezeBalance
      Assert.assertTrue(PublicMethed.freezeBalance(fromAddress, 10000000L, 3, testKey002,
          blockingStubFull));
      //Create success
      start = System.currentTimeMillis() + 6000;
      end = System.currentTimeMillis() + 1000000000;
      Assert.assertTrue(PublicMethed.createAssetIssue(fromAddress, name, totalSupply, 1, 10,
          start, end, 2, description, url,10000L,10000L,
          1L, 3652L, testKey002,blockingStubFull));
      //Test not in the duration time, participate failed.
      Assert.assertFalse(PublicMethed.participateAssetIssue(fromAddress, name.getBytes(), 1L,
          toAddress, testKey003,blockingStubFull));
      try {
        Thread.sleep(4000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      //Assert.assertFalse(participateAssetIssue
      // (fromAddress, name.getBytes(),1L, toAddress, testKey003));
      GrpcAPI.AssetIssueList assetIssueList = blockingStubFull
          .getAssetIssueList(GrpcAPI.EmptyMessage.newBuilder().build());
      logger.info(Integer.toString(assetIssueList.getAssetIssue(0).getFrozenSupplyCount()));
      Assert.assertTrue(assetIssueList.getAssetIssue(0).getFrozenSupplyCount() == 1);
      //Assert.assertTrue(assetIssueList.getAssetIssue(j).getFrozenSupplyCount() > 0);
      Assert.assertTrue(assetIssueList.getAssetIssue(0).getFrozenSupply(0).getFrozenAmount() > 0);
      Assert.assertTrue(assetIssueList.getAssetIssue(0).getFrozenSupply(0).getFrozenDays() > 0);
      Assert.assertFalse(unFreezeAsset(fromAddress, testKey002));
      logger.info("unfreezeasset test");

      //Test one account only can create one asset issue.
      start = System.currentTimeMillis() + 3000;
      end = System.currentTimeMillis() + 1000000000;
      Assert.assertFalse(PublicMethed.createAssetIssue(fromAddress, shortname, totalSupply, 1, 10,
          start, end, 2, description, url,10000L,10000L,
          1L, 3652L, testKey002,blockingStubFull));
      logger.info("FROM ADDRESS create asset issue in this case!!!");

    } else {
      logger.info("This account already create an assetisue");
    }
  }

  @Test(enabled = false)
  public void testGetAllAssetIssue() {
    GrpcAPI.AssetIssueList assetIssueList = blockingStubFull
        .getAssetIssueList(GrpcAPI.EmptyMessage.newBuilder().build());
    Assert.assertTrue(assetIssueList.getAssetIssueCount() >= 1);
    Integer times = assetIssueList.getAssetIssueCount();
    if (assetIssueList.getAssetIssueCount() >= 10) {
      times = 10;
    }
    for (Integer j = 0; j < times; j++) {
      Assert.assertFalse(assetIssueList.getAssetIssue(j).getOwnerAddress().isEmpty());
      Assert.assertFalse(assetIssueList.getAssetIssue(j).getName().isEmpty());
      Assert.assertFalse(assetIssueList.getAssetIssue(j).getUrl().isEmpty());
      Assert.assertTrue(assetIssueList.getAssetIssue(j).getTotalSupply() > 0);
      logger.info("test get all assetissue");
    }

    //Improve coverage.
    assetIssueList.equals(assetIssueList);
    assetIssueList.equals(null);
    GrpcAPI.AssetIssueList newAssetIssueList = blockingStubFull
        .getAssetIssueList(GrpcAPI.EmptyMessage.newBuilder().build());
    assetIssueList.equals(newAssetIssueList);
    assetIssueList.hashCode();
    assetIssueList.getSerializedSize();

  }

  @AfterClass(enabled = false)
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
    Account search = queryAccount(ecKey, blockingStubFull);

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
      builder.setFreeAssetNetLimit(10000);
      builder.setPublicFreeAssetNetLimit(10000);
      builder.setUrl(ByteString.copyFrom(url.getBytes()));
      Contract.AssetIssueContract.FrozenSupply.Builder frozenBuilder =
          Contract.AssetIssueContract.FrozenSupply
              .newBuilder();
      frozenBuilder.setFrozenAmount(fronzenAmount);
      frozenBuilder.setFrozenDays(frozenDay);
      builder.addFrozenSupply(0, frozenBuilder);

      Transaction transaction = blockingStubFull.createAssetIssue(builder.build());
      if (transaction == null || transaction.getRawData().getContractCount() == 0) {
        logger.info("transaction == null");
        return false;
      }
      transaction = signTransaction(ecKey, transaction);
      Return response = blockingStubFull.broadcastTransaction(transaction);
      if (response.getResult() == false) {
        logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
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

  public boolean unFreezeAsset(byte[] addRess, String priKey) {
    byte[] address = addRess;

    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    Contract.UnfreezeAssetContract.Builder builder = Contract.UnfreezeAssetContract
        .newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);

    builder.setOwnerAddress(byteAddreess);

    Contract.UnfreezeAssetContract contract = builder.build();

    Transaction transaction = blockingStubFull.unfreezeAsset(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }

    transaction = TransactionUtils.setTimestamp(transaction);
    transaction = TransactionUtils.sign(transaction, ecKey);
    Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      return false;
    } else {
      return true;
    }
  }


  public boolean participateAssetIssue(byte[] to, byte[] assertName, long amount, byte[] from,
      String priKey) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    Contract.ParticipateAssetIssueContract.Builder builder = Contract.ParticipateAssetIssueContract
        .newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsName = ByteString.copyFrom(assertName);
    ByteString bsOwner = ByteString.copyFrom(from);
    builder.setToAddress(bsTo);
    builder.setAssetName(bsName);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);
    Contract.ParticipateAssetIssueContract contract = builder.build();

    Transaction transaction = blockingStubFull.participateAssetIssue(contract);
    transaction = signTransaction(ecKey, transaction);
    Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      return false;
    } else {
      logger.info(name);
      return true;
    }
  }

}


