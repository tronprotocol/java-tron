package stest.tron.wallet.dailybuild.assetissue;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.ParticipateAssetIssueContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.TransferAssetContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.TransactionUtils;

@Slf4j
public class WalletTestAssetIssue002 {

  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = now;
  private static String name = "testAssetIssue002_" + Long.toString(now);
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  public static String loadPubKey() {
    char[] buf = new char[0x100];
    return String.valueOf(buf, 32, 130);
  }


  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);


  }

  @Test(enabled = true, description = "Participate token")
  public void testParticipateAssetissue() {
    //get account
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] participateAccountAddress = ecKey1.getAddress();
    final String participateAccountKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    ECKey ecKey2 = new ECKey(Utils.getRandom());
    byte[] toAddress = ecKey2.getAddress();
    final String testKey003 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    //send coin to the new account
    Assert.assertTrue(PublicMethed.sendcoin(participateAccountAddress, 2048000000, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.sendcoin(toAddress, 2048000000, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //Create a new Asset Issue
    Assert.assertTrue(PublicMethed.createAssetIssue(participateAccountAddress,
        name, totalSupply, 1, 1, System.currentTimeMillis() + 5000,
        System.currentTimeMillis() + 1000000000, 1, description, url,
        2000L, 2000L, 1L, 1L,
        participateAccountKey, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account getAssetIdFromThisAccount;
    getAssetIdFromThisAccount = PublicMethed.queryAccount(participateAccountKey, blockingStubFull);
    final ByteString assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();

    //Participate AssetIssue success
    logger.info(name);
    //Freeze amount to get bandwitch.
    logger.info("toaddress balance is "
        + PublicMethed.queryAccount(toAddress, blockingStubFull).getBalance());
    Assert.assertTrue(PublicMethed.freezeBalance(toAddress, 10000000, 3, testKey003,
        blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.participateAssetIssue(participateAccountAddress,
        assetAccountId.toByteArray(),
        100L, toAddress, testKey003, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    //The amount is large than the total supply, participate failed.
    Assert.assertFalse(PublicMethed.participateAssetIssue(participateAccountAddress,
        assetAccountId.toByteArray(), 9100000000000000000L, toAddress, testKey003,
        blockingStubFull));

    //The amount is 0, participate asset issue failed.
    Assert.assertFalse(PublicMethed.participateAssetIssue(participateAccountAddress,
        assetAccountId.toByteArray(), 0L, toAddress, testKey003, blockingStubFull));

    //The amount is -1, participate asset issue failed.
    Assert.assertFalse(PublicMethed.participateAssetIssue(participateAccountAddress,
        assetAccountId.toByteArray(), -1L, toAddress, testKey003, blockingStubFull));

    //The asset issue owner address is not correct, participate asset issue failed.
    Assert.assertFalse(PublicMethed.participateAssetIssue(fromAddress,
        assetAccountId.toByteArray(), 100L,
        toAddress, testKey003, blockingStubFull));

    PublicMethed.freedResource(participateAccountAddress, participateAccountKey, fromAddress,
        blockingStubFull);
    PublicMethed.freedResource(toAddress, testKey003, fromAddress, blockingStubFull);

  }

  /**
   * constructor.
   */

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  /**
   * constructor.
   */

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

    ParticipateAssetIssueContract.Builder builder = ParticipateAssetIssueContract
        .newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsName = ByteString.copyFrom(assertName);
    ByteString bsOwner = ByteString.copyFrom(from);
    builder.setToAddress(bsTo);
    builder.setAssetName(bsName);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);
    ParticipateAssetIssueContract contract = builder.build();

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

  /**
   * constructor.
   */

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
      AssetIssueContract.Builder builder = AssetIssueContract.newBuilder();
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
      AssetIssueContract.FrozenSupply.Builder frozenBuilder =
          AssetIssueContract.FrozenSupply.newBuilder();
      frozenBuilder.setFrozenAmount(fronzenAmount);
      frozenBuilder.setFrozenDays(frozenDay);
      builder.addFrozenSupply(0, frozenBuilder);

      Transaction transaction = blockingStubFull.createAssetIssue(builder.build());
      if (transaction == null || transaction.getRawData().getContractCount() == 0) {
        return false;
      }
      transaction = signTransaction(ecKey, transaction);
      Return response = blockingStubFull.broadcastTransaction(transaction);
      if (!response.getResult()) {
        logger.info(name);
      }
      return response.getResult();
    } catch (Exception ex) {
      ex.printStackTrace();
      return false;
    }
  }

  /**
   * constructor.
   */

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

  public byte[] getAddress(ECKey ecKey) {
    return ecKey.getAddress();
  }

  /**
   * constructor.
   */

  public Account grpcQueryAccount(byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccount(request);
  }

  /**
   * constructor.
   */

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

  /**
   * constructor.
   */

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

    TransferAssetContract.Builder builder = TransferAssetContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsName = ByteString.copyFrom(assertName);
    ByteString bsOwner = ByteString.copyFrom(address);
    builder.setToAddress(bsTo);
    builder.setAssetName(bsName);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    TransferAssetContract contract = builder.build();
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


