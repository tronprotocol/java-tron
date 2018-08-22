package stest.tron.wallet.fulltest;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.HashMap;
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
import org.tron.api.GrpcAPI.WitnessList;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.TransactionUtils;

//import stest.tron.wallet.common.client.AccountComparator;

@Slf4j
public class SuperWitnessAllowance {

  //testng001、testng002、testng003、testng004
  private final String testKey002 =
      "FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";


  /*  //testng001、testng002、testng003、testng004
  private static final byte[] fromAddress = Base58
      .decodeFromBase58Check("THph9K2M2nLvkianrMGswRhz5hjSA9fuH7");*/
  private static final byte[] INVAILD_ADDRESS = Base58
      .decodeFromBase58Check("27cu1ozb4mX3m2afY68FSAqn3HmMp815d48");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);


  private static final Long costForCreateWitness = 10009000000L;
  String createWitnessUrl = "http://www.createwitnessurl.com";
  String updateWitnessUrl = "http://www.updatewitnessurl.com";
  String nullUrl = "";
  String spaceUrl = "          ##################~!@#$%^&*()_+}{|:'/.,<>?|]=-";
  byte[] createUrl = createWitnessUrl.getBytes();
  byte[] updateUrl = updateWitnessUrl.getBytes();
  byte[] wrongUrl = nullUrl.getBytes();
  byte[] updateSpaceUrl = spaceUrl.getBytes();
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  //get account
  ECKey ecKey = new ECKey(Utils.getRandom());
  byte[] lowBalAddress = ecKey.getAddress();
  String lowBalTest = ByteArray.toHexString(ecKey.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass
  public void beforeClass() {
    logger.info(lowBalTest);
    logger.info(ByteArray.toHexString(PublicMethed.getFinalAddress(lowBalTest)));
    logger.info(Base58.encode58Check(PublicMethed.getFinalAddress(lowBalTest)));


    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test
  public void testInvaildToApplyBecomeWitness() {
    Assert.assertFalse(createWitness(INVAILD_ADDRESS, createUrl, testKey002));
  }

  //@Test(enabled = true,threadPoolSize = 10, invocationCount = 10)
  @Test(enabled = false)
  public void testCreate130Witness() {
    WitnessList witnesslist = blockingStubFull
        .listWitnesses(GrpcAPI.EmptyMessage.newBuilder().build());
    Optional<WitnessList> result = Optional.ofNullable(witnesslist);
    WitnessList witnessList = result.get();
    while (witnessList.getWitnessesCount() < 130) {
      ECKey ecKey = new ECKey(Utils.getRandom());
      byte[] lowBalAddress = ecKey.getAddress();
      String lowBalTest = ByteArray.toHexString(ecKey.getPrivKeyBytes());
      logger.info(lowBalTest);
      Assert.assertTrue(sendcoin(lowBalAddress, costForCreateWitness, fromAddress, testKey002));
      Assert.assertTrue(PublicMethed.freezeBalance(lowBalAddress,1000000,
          3,lowBalTest,blockingStubFull));
      Assert.assertTrue(createWitness(lowBalAddress, createUrl, lowBalTest));
      String voteStr = Base58.encode58Check(PublicMethed.getFinalAddress(lowBalTest));
      HashMap<String, String> smallVoteMap = new HashMap<String, String>();
      smallVoteMap.put(voteStr, "1");
      Assert.assertTrue(voteWitness(smallVoteMap, lowBalAddress, lowBalTest));
      witnesslist = blockingStubFull
          .listWitnesses(GrpcAPI.EmptyMessage.newBuilder().build());
      result = Optional.ofNullable(witnesslist);
      witnessList = result.get();

    }

  }

  //@Test(enabled = true,threadPoolSize = 10, invocationCount = 10)
  @Test(enabled = false)
  public void testQueryAllowance() {
    WitnessList witnesslist = blockingStubFull
        .listWitnesses(GrpcAPI.EmptyMessage.newBuilder().build());
    Optional<WitnessList> result = Optional.ofNullable(witnesslist);
    WitnessList witnessList = result.get();
    Integer allowanceNum = 0;
    for (Integer i = 0; i < witnessList.getWitnessesCount(); i++) {
      /*      witnessList.getWitnesses(i).getAddress();
      witnessList.getWitnesses(i).getAddress();
      witnessList.getWitnesses(i).getAddress();
      witnessList.getWitnesses(i).getAddress();*/
      ByteString addressBs = witnessList.getWitnesses(i).getAddress();
      Account request = Account.newBuilder().setAddress(addressBs).build();
      request = blockingStubFull.getAccount(request);
      if (request.getAllowance() > 0) {
        allowanceNum++;
      }
      logger.info("Account " + Integer.toString(i) + " allowance is " + Long.toString(request
          .getAllowance()));

    }
    logger.info("Allowance num is " + Integer.toString(allowanceNum));


  }


  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


  public Boolean createWitness(byte[] owner, byte[] url, String priKey) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    Contract.WitnessCreateContract.Builder builder = Contract.WitnessCreateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setUrl(ByteString.copyFrom(url));
    Contract.WitnessCreateContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.createWitness(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      return false;
    } else {
      return true;
    }

  }

  public Boolean updateWitness(byte[] owner, byte[] url, String priKey) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    Contract.WitnessUpdateContract.Builder builder = Contract.WitnessUpdateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setUpdateUrl(ByteString.copyFrom(url));
    Contract.WitnessUpdateContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.updateWitness(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction == null");
      return false;
    }
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      logger.info("response.getRestult() == false");
      return false;
    } else {
      return true;
    }

  }


  public Boolean sendcoin(byte[] to, long amount, byte[] owner, String priKey) {

    //String priKey = testKey002;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    Contract.TransferContract.Builder builder = Contract.TransferContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    Contract.TransferContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.createTransaction(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
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

  private Protocol.Transaction signTransaction(ECKey ecKey, Protocol.Transaction transaction) {
    if (ecKey == null || ecKey.getPrivKey() == null) {
      logger.warn("Warning: Can't sign,there is no private key !!");
      return null;
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    return TransactionUtils.sign(transaction, ecKey);
  }

  public Boolean voteWitness(HashMap<String, String> witness, byte[] addRess, String priKey) {

    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    Account beforeVote  = PublicMethed.queryAccount(priKey,blockingStubFull);
    //Account beforeVote = queryAccount(ecKey, blockingStubFull);
    Long beforeVoteNum = 0L;
    if (beforeVote.getVotesCount() != 0) {
      beforeVoteNum = beforeVote.getVotes(0).getVoteCount();
    }

    Contract.VoteWitnessContract.Builder builder = Contract.VoteWitnessContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(addRess));
    for (String addressBase58 : witness.keySet()) {
      String value = witness.get(addressBase58);
      final long count = Long.parseLong(value);
      Contract.VoteWitnessContract.Vote.Builder voteBuilder = Contract.VoteWitnessContract.Vote
          .newBuilder();
      byte[] address = WalletClient.decodeFromBase58Check(addressBase58);
      logger.info("address ====== " + ByteArray.toHexString(address));
      if (address == null) {
        continue;
      }
      voteBuilder.setVoteAddress(ByteString.copyFrom(address));
      voteBuilder.setVoteCount(count);
      builder.addVotes(voteBuilder.build());
    }

    Contract.VoteWitnessContract contract = builder.build();

    Transaction transaction = blockingStubFull.voteWitnessAccount(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction == null");
      return false;
    }
    transaction = signTransaction(ecKey, transaction);
    Return response = blockingStubFull.broadcastTransaction(transaction);

    if (response.getResult() == false) {
      logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      return false;
    }
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    //Long afterVoteNum = afterVote.getVotes(0).getVoteCount();
    return true;
  }
}


