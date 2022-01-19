package stest.tron.wallet.account;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.contract.AssetIssueContractOuterClass;
import org.tron.protos.contract.BalanceContract;
import org.tron.protos.contract.WitnessContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.TransactionUtils;

//import stest.tron.wallet.common.client.AccountComparator;

@Slf4j
public class WalletTestAccount003 {

  private static final long now = System.currentTimeMillis();
  private static final String name = "testAssetIssue_" + Long.toString(now);
  private static final long TotalSupply = now;
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  String mostLongNamePlusOneChar = "1abcdeabcdefabcdefg1abcdefg10o0og1abcdefg10o0oabcd"
      + "efabcdefg1abcdefg10o0og1abcdefg10o0oabcdefabcdefg1abcdefg10o0og1abcdefg10o0oab"
      + "cdefabcdefg1abcdefg10o0og1abcdefg10o0ofabcdefg1abcdefg10o0og1abcdefg10o0o";

  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");

  //get account
  ECKey ecKey = new ECKey(Utils.getRandom());
  byte[] lowBalAddress = ecKey.getAddress();
  String lowBalTest = ByteArray.toHexString(ecKey.getPrivKeyBytes());
  //get account
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] noBandwitchAddress = ecKey1.getAddress();
  String noBandwitch = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
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

  public static String getRandomStr(int length) {
    String base = "abcdefghijklmnopqrstuvwxyz0123456789";
    int randomNum;
    char randomChar;
    Random random = new Random();
    StringBuffer str = new StringBuffer();

    for (int i = 0; i < length; i++) {
      randomNum = random.nextInt(base.length());
      randomChar = base.charAt(randomNum);
      str.append(randomChar);
    }
    return str.toString();
  }



  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {
    PublicMethed.printAddress(lowBalTest);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test
  public void test1CreateAccount() {
    Account noCreateAccount = PublicMethed.queryAccount(lowBalTest, blockingStubFull);
    while (noCreateAccount.getBalance() != 0) {
      ecKey = new ECKey(Utils.getRandom());
      lowBalAddress = ecKey.getAddress();
      lowBalTest = ByteArray.toHexString(ecKey.getPrivKeyBytes());
      noCreateAccount = PublicMethed.queryAccount(lowBalTest, blockingStubFull);
    }
    Assert.assertTrue(sendCoin(lowBalAddress, 1L, fromAddress, testKey002));
    noCreateAccount = PublicMethed.queryAccount(lowBalTest, blockingStubFull);
    logger.info(Long.toString(noCreateAccount.getBalance()));
    Assert.assertTrue(noCreateAccount.getBalance() == 1);
  }

  @Test(enabled = true)
  public void test2UpdateAccount() {
    Assert.assertFalse(PublicMethed.updateAccount(lowBalAddress,
        mostLongNamePlusOneChar.getBytes(), lowBalTest, blockingStubFull));
    Assert.assertFalse(PublicMethed.updateAccount(lowBalAddress, "".getBytes(), lowBalTest,
        blockingStubFull));
    String mostLongName = getRandomStr(33);
    Assert.assertTrue(PublicMethed.updateAccount(lowBalAddress, mostLongName.getBytes(), lowBalTest,
        blockingStubFull));
    String firstUpdateName = getRandomStr(32);
    Assert.assertFalse(PublicMethed.updateAccount(lowBalAddress, firstUpdateName.getBytes(),
        lowBalTest, blockingStubFull));
    String secondUpdateName = getRandomStr(15);
    Assert.assertFalse(PublicMethed.updateAccount(lowBalAddress, secondUpdateName.getBytes(),
        lowBalTest, blockingStubFull));
  }

  @Test(enabled = true)
  public void test3NoBalanceCreateAssetIssue() {
    Account lowaccount = PublicMethed.queryAccount(lowBalTest, blockingStubFull);
    if (lowaccount.getBalance() > 0) {
      Assert.assertTrue(sendCoin(toAddress, lowaccount.getBalance(), lowBalAddress, lowBalTest));
    }
    //Create AssetIssue failed when there is no enough balance.
    Assert.assertFalse(PublicMethed.createAssetIssue(lowBalAddress, name, TotalSupply, 1,
        1, now + 100000000L, now + 10000000000L, 2,
        description, url, 10000L, 10000L, 1L,
        1L, lowBalTest, blockingStubFull));
    logger.info("nobalancecreateassetissue");
  }

  @Test(enabled = true)
  public void test4NoBalanceTransferTrx() {
    //Send Coin failed when there is no enough balance.
    Assert.assertFalse(sendCoin(toAddress, 100000000000000000L, lowBalAddress, lowBalTest));
  }

  @Test(enabled = true)
  public void test5NoBalanceCreateWitness() {
    //Apply to be super witness failed when no enough balance.
    Assert.assertFalse(createWitness(lowBalAddress, fromAddress, lowBalTest));
  }

  @Test(enabled = true)
  public void test6NoFreezeBalanceToUnfreezeBalance() {
    //Unfreeze account failed when no freeze balance
    Account noFreezeAccount = PublicMethed.queryAccount(lowBalTest, blockingStubFull);
    if (noFreezeAccount.getFrozenCount() == 0) {
      Assert.assertFalse(unFreezeBalance(lowBalAddress, lowBalTest));
    } else {
      logger.info("This account has freeze balance, please test this case for manual");
    }
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  /**
   * constructor.
   */

  public Boolean createWitness(byte[] owner, byte[] url, String priKey) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    WitnessContract.WitnessCreateContract.Builder builder = WitnessContract.WitnessCreateContract
        .newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setUrl(ByteString.copyFrom(url));
    WitnessContract.WitnessCreateContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.createWitness(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
    return response.getResult();
  }

  /**
   * constructor.
   */

  public Boolean sendCoin(byte[] to, long amount, byte[] owner, String priKey) {
    //String priKey = testKey002;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    BalanceContract.TransferContract.Builder builder = BalanceContract.TransferContract
        .newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    BalanceContract.TransferContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.createTransaction(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction == null");
      return false;
    }
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      return false;
    } else {
      return true;
    }
  }

  /**
   * constructor.
   */

  public Boolean createAssetIssue(byte[] address, String name, Long totalSupply, Integer trxNum,
      Integer icoNum, Long startTime, Long endTime,
      Integer voteScore, String description, String url, String priKey) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;

    try {
      AssetIssueContractOuterClass.AssetIssueContract.Builder builder
          = AssetIssueContractOuterClass.AssetIssueContract.newBuilder();
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

      Protocol.Transaction transaction = blockingStubFull.createAssetIssue(builder.build());
      if (transaction == null || transaction.getRawData().getContractCount() == 0) {
        logger.info("Please check!!! transaction == null");
        return false;
      }
      transaction = signTransaction(ecKey, transaction);
      GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
      if (response.getResult() == false) {
        logger.info("Please check!!! response.getresult==false");
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

  private Protocol.Transaction signTransaction(ECKey ecKey, Protocol.Transaction transaction) {
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

  public boolean unFreezeBalance(byte[] address, String priKey) {
    //byte[] address = address;

    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    BalanceContract.UnfreezeBalanceContract.Builder builder
        = BalanceContract.UnfreezeBalanceContract.newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);

    builder.setOwnerAddress(byteAddreess);

    BalanceContract.UnfreezeBalanceContract contract = builder.build();

    Protocol.Transaction transaction = blockingStubFull.unfreezeBalance(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }

    transaction = TransactionUtils.setTimestamp(transaction);
    transaction = TransactionUtils.sign(transaction, ecKey);
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      return false;
    } else {
      return true;
    }
  }

  /**
   * constructor.
   */

  public Boolean voteWitness(HashMap<String, String> witness, byte[] address, String priKey) {

    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    WitnessContract.VoteWitnessContract.Builder builder = WitnessContract.VoteWitnessContract
        .newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(address));
    for (String addressBase58 : witness.keySet()) {
      String value = witness.get(addressBase58);
      long count = Long.parseLong(value);
      WitnessContract.VoteWitnessContract.Vote.Builder voteBuilder
          = WitnessContract.VoteWitnessContract.Vote.newBuilder();
      byte[] addRess = WalletClient.decodeFromBase58Check(addressBase58);
      if (addRess == null) {
        return false;
      }
      voteBuilder.setVoteAddress(ByteString.copyFrom(addRess));
      voteBuilder.setVoteCount(count);
      builder.addVotes(voteBuilder.build());
    }

    WitnessContract.VoteWitnessContract contract = builder.build();

    Protocol.Transaction transaction = blockingStubFull.voteWitnessAccount(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction == null");
      return false;
    }
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);

    if (response.getResult() == false) {
      logger.info("response.getresult() == false");
      return false;
    }
    return true;
  }

  /**
   * constructor.
   */

  public Boolean freezeBalance(byte[] addRess, long freezeBalance, long freezeDuration,
      String priKey) {
    byte[] address = addRess;
    long frozenBalance = freezeBalance;
    long frozenDuration = freezeDuration;

    //String priKey = testKey002;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    BalanceContract.FreezeBalanceContract.Builder builder = BalanceContract.FreezeBalanceContract
        .newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);

    builder.setOwnerAddress(byteAddreess).setFrozenBalance(frozenBalance)
        .setFrozenDuration(frozenDuration);

    BalanceContract.FreezeBalanceContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.freezeBalance(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }

    transaction = TransactionUtils.setTimestamp(transaction);
    transaction = TransactionUtils.sign(transaction, ecKey);
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);

    if (response.getResult() == false) {
      return false;
    }
    return true;


  }

  class AccountComparator implements Comparator {

    public int compare(Object o1, Object o2) {
      return Long.compare(((Account) o2).getBalance(), ((Account) o1).getBalance());
    }
  }

}



