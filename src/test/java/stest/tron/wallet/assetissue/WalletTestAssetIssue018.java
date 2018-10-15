package stest.tron.wallet.assetissue;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
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
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Contract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.TransactionUtils;

@Slf4j
public class WalletTestAssetIssue018 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  private static final long now = System.currentTimeMillis();
  private static final String name = "testAssetIssue018_" + Long.toString(now);
  private static final long totalSupply = now;
  String description = "just-test";
  String url = "https://github.com/tronprotocol/wallet-cli/";

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  //get account
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] assetAccount1Address = ecKey1.getAddress();
  String assetAccount1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] assetAccount2Address = ecKey2.getAddress();
  String assetAccount2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] assetAccount3Address = ecKey3.getAddress();
  String assetAccount3Key = ByteArray.toHexString(ecKey3.getPrivKeyBytes());

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
    PublicMethed.printAddress(assetAccount1Key);
    PublicMethed.printAddress(assetAccount2Key);
    PublicMethed.printAddress(assetAccount3Key);
  }

  @Test(enabled = true)
  public void testSameAssetissueName() {
    logger.info(name);
    logger.info("total supply is " + Long.toString(totalSupply));
    //send coin to the new account
    Assert.assertTrue(PublicMethed.sendcoin(assetAccount1Address,2048000000,fromAddress,
        testKey002,blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(assetAccount2Address,2048000000,fromAddress,
        testKey002,blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(assetAccount3Address,2048000000,fromAddress,
        testKey002,blockingStubFull));

    //Create 3 the same name token.
    Long start = System.currentTimeMillis() + 2000;
    Long end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethed.createAssetIssue(assetAccount1Address,
        name, totalSupply, 1, 1, start, end, 1, description, url,
        2000L,2000L, 1L,1L,assetAccount1Key,blockingStubFull));
    start = System.currentTimeMillis() + 2000;
    end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethed.createAssetIssue(assetAccount2Address,
        name, totalSupply + 1, 2, 2, start, end, 2, description, url,
        3000L,3000L, 2L,2L,assetAccount2Key,blockingStubFull));
    start = System.currentTimeMillis() + 2000;
    end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethed.createAssetIssue(assetAccount3Address,
        name, totalSupply + 2, 3, 3, start, end, 3, description, url,
        4000L,4000L, 3L,2L,assetAccount3Key,blockingStubFull));


    //Get asset issue by name
    String asset1Name = name;
    String asset2Name = name + "_1";
    final String asset3Name = name + "_2";
    ByteString assetNameBs = ByteString.copyFrom(asset1Name.getBytes());
    GrpcAPI.BytesMessage request = GrpcAPI.BytesMessage.newBuilder().setValue(assetNameBs).build();
    Contract.AssetIssueContract assetIssueByName = blockingStubFull.getAssetIssueByName(request);
    Assert.assertTrue(assetIssueByName.getVoteScore() == 1);

    assetNameBs = ByteString.copyFrom(asset2Name.getBytes());
    request = GrpcAPI.BytesMessage.newBuilder().setValue(assetNameBs).build();
    assetIssueByName = blockingStubFull.getAssetIssueByName(request);
    Assert.assertTrue(assetIssueByName.getVoteScore() == 2);

    assetNameBs = ByteString.copyFrom(asset3Name.getBytes());
    request = GrpcAPI.BytesMessage.newBuilder().setValue(assetNameBs).build();
    assetIssueByName = blockingStubFull.getAssetIssueByName(request);
    Assert.assertTrue(assetIssueByName.getVoteScore() == 3);

    //Transfer asset issue.
    Assert.assertTrue(PublicMethed.transferAsset(assetAccount2Address,
        asset1Name.getBytes(),1L,assetAccount1Address,assetAccount1Key,blockingStubFull));

    Assert.assertTrue(PublicMethed.transferAsset(assetAccount3Address,
        asset2Name.getBytes(),2L,assetAccount2Address,assetAccount2Key,blockingStubFull));

    Assert.assertTrue(PublicMethed.transferAsset(assetAccount1Address,
        asset3Name.getBytes(),3L,assetAccount3Address,assetAccount3Key,blockingStubFull));

    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    //Participate asset issue.
    Assert.assertTrue(PublicMethed.participateAssetIssue(assetAccount3Address,asset3Name.getBytes(),
        1L,assetAccount2Address,assetAccount2Key,blockingStubFull));

    Assert.assertTrue(PublicMethed.participateAssetIssue(assetAccount1Address,asset1Name.getBytes(),
        2L,assetAccount3Address,assetAccount3Key,blockingStubFull));

    Assert.assertTrue(PublicMethed.participateAssetIssue(assetAccount2Address,asset2Name.getBytes(),
        3L,assetAccount1Address,assetAccount1Key,blockingStubFull));


  }

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
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

  public Boolean createAssetIssue(byte[] address, String name, Long totalSupply, Integer trxNum,
      Integer icoNum, Long startTime, Long endTime,
      Integer voteScore, String description, String url, Long fronzenAmount, Long frozenDay,
      String priKey,Long order) {
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
      Contract.AssetIssueContract.FrozenSupply.Builder frozenBuilder =
          Contract.AssetIssueContract.FrozenSupply.newBuilder();
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

  public Account queryAccount(String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    byte[] address;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
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
      logger.info("transaction == null || transaction.getRawData().getContractCount() == 0");
      return false;
    }
    transaction = signTransaction(ecKey, transaction);
    Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      return false;
    } else {
      //Account search = queryAccount(ecKey, blockingStubFull);
      return true;
    }

  }
}


