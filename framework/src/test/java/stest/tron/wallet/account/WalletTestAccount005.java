package stest.tron.wallet.account;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.contract.BalanceContract;
import org.tron.protos.contract.WitnessContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.TransactionUtils;

@Slf4j
public class WalletTestAccount005 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  private final String notWitnessTestKey =
      "8CB4480194192F30907E14B52498F594BD046E21D7C4D8FE866563A6760AC891";

  private final byte[] notWitness = PublicMethed.getFinalAddress(notWitnessTestKey);

  private ManagedChannel channelFull = null;
  private ManagedChannel searchChannelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletGrpc.WalletBlockingStub searchBlockingStubFull = null;
  private String fullnode =
      Configuration.getByPath("testng.conf")
          .getStringList("fullnode.ip.list").get(0);
  private String searchFullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);

  public static String loadPubKey() {
    char[] buf = new char[0x100];
    return String.valueOf(buf, 32, 130);
  }

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    searchChannelFull = ManagedChannelBuilder.forTarget(searchFullnode)
        .usePlaintext(true)
        .build();
    searchBlockingStubFull = WalletGrpc.newBlockingStub(searchChannelFull);


  }

  @Test
  public void testWithdrawBalance() {
    //Withdraw failed when you are not witness
    Assert.assertFalse(withdrawBalance(notWitness, notWitnessTestKey));
    //Due to it's hard to automation, withdraw balance success case is not automation,
    // please test by manual
    //Assert.assertTrue(WithdrawBalance(fromAddress,testKey002));
    //Withdraw failed when the latest time to withdraw within 1 day.

    if (withdrawBalance(fromAddress, testKey002)) {
      Assert.assertFalse(withdrawBalance(fromAddress, testKey002));
    } else {
      logger.info("This account has withdraw within 1 day, please confirm");
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
    if (searchChannelFull != null) {
      searchChannelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  /**
   * constructor.
   */

  public boolean withdrawBalance(byte[] address, String priKey) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;

    BalanceContract.WithdrawBalanceContract.Builder builder =
        BalanceContract.WithdrawBalanceContract.newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);
    builder.setOwnerAddress(byteAddreess);
    BalanceContract.WithdrawBalanceContract contract = builder.build();

    Transaction transaction = blockingStubFull.withdrawBalance(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }

    transaction = signTransaction(ecKey, transaction);
    Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      return false;
    }
    logger.info("test withdraw" + priKey);
    return true;

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
    ECKey ecKey = temKey;
    Account beforeVote = queryAccount(ecKey, blockingStubFull);
    Long beforeVoteNum = 0L;
    if (beforeVote.getVotesCount() != 0) {
      beforeVoteNum = beforeVote.getVotes(0).getVoteCount();
    }

    WitnessContract.VoteWitnessContract.Builder builder = WitnessContract.VoteWitnessContract
        .newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(address));
    for (String addressBase58 : witness.keySet()) {
      String value = witness.get(addressBase58);
      long count = Long.parseLong(value);
      WitnessContract.VoteWitnessContract.Vote.Builder voteBuilder =
          WitnessContract.VoteWitnessContract.Vote.newBuilder();
      byte[] addRess = WalletClient.decodeFromBase58Check(addressBase58);
      if (addRess == null) {
        continue;
      }
      voteBuilder.setVoteAddress(ByteString.copyFrom(address));
      voteBuilder.setVoteCount(count);
      builder.addVotes(voteBuilder.build());
    }

    WitnessContract.VoteWitnessContract contract = builder.build();

    Transaction transaction = blockingStubFull.voteWitnessAccount(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }
    transaction = signTransaction(ecKey, transaction);
    Return response = blockingStubFull.broadcastTransaction(transaction);

    if (response.getResult() == false) {
      return false;
    }
    Account afterVote = queryAccount(ecKey, searchBlockingStubFull);
    //Long afterVoteNum = afterVote.getVotes(0).getVoteCount();
    for (String key : witness.keySet()) {
      for (int j = 0; j < afterVote.getVotesCount(); j++) {
        if (key.equals(afterVote.getVotes(j).getVoteAddress())) {
          Long afterVoteNum = Long.parseLong(witness.get(key));
          Assert.assertTrue(afterVoteNum == afterVote.getVotes(j).getVoteCount());
          logger.info("test equal vote");
        }
      }
    }
    return true;
  }

  /**
   * constructor.
   */

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
}


