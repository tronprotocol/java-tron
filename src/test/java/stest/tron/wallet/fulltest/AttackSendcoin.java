package stest.tron.wallet.fulltest;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.slf4j.Logger;
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
import org.tron.protos.Contract.TransferContract;
import org.tron.protos.Contract.UnfreezeBalanceContract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.TransactionUtils;

@Slf4j
public class AttackSendcoin {

  //testng001、testng002、testng003、testng004
  //Devaccount
  private final String testKey001 =
      "8CB4480194192F30907E14B52498F594BD046E21D7C4D8FE866563A6760AC891";
  //Zion
  private final String testKey002 =
      "FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";
  //Sun
  private final String testKey003 =
      "6815B367FDDE637E53E9ADC8E69424E07724333C9A2B973CFA469975E20753FC";
  //Normal1
  private final String normalKey001 =
      "36c0710378a34634e6baba0d3a79d7439a81183030147e7f4a0dd43bfed1a32f";
  //Normal2
  private final String normalKey002 =
      "a6bfbcf98bbe07770bf79bc6b2970bae0992771c1dcbf24cc063a3f033f17fbf";
  //Normal3
  private final String normalKey003 =
      "8273f6b26202526cbffb77569b830c1ba8a920040e77f6f26062a67315580ed7";
  //Normal4
  private final String normalKey004 =
      "271c824fcb55f04a9f86f768424a80edeb26ab79cf12aa56643b595f689c008a";


  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress   = PublicMethed.getFinalAddress(testKey003);
  private final byte[] attackAddress = PublicMethed.getFinalAddress(testKey001);
  private final byte[] normal1Address = PublicMethed.getFinalAddress(normalKey001);
  private final byte[] normal2Address = PublicMethed.getFinalAddress(normalKey002);
  private final byte[] normal3Address = PublicMethed.getFinalAddress(normalKey003);
  private final byte[] normal4Address = PublicMethed.getFinalAddress(normalKey004);


  private static final long now = System.currentTimeMillis();
  private static long start;
  private static long end;
  private static long beforeFromBalance;
  private static long beforeNormal1Balance;
  private static long beforeNormal2Balance;
  private static long beforeNormal3Balance;
  private static long beforeNormal4Balance;
  private static long beforeAttackBalance;
  private static long afterFromBalance;
  private static long afterNormal1Balance;
  private static long afterNormal2Balance;
  private static long afterNormal3Balance;
  private static long afterNormal4Balance;
  private static long afterAttackBalance;
  private final Long sendNromal1Amount = 1L;
  private final Long sendNromal2Amount = 2L;
  private final Long sendNromal3Amount = 3L;
  private final Long sendNromal4Amount = 4L;
  private final Long attackAmount = 5L;

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
    final Account fromInfo = PublicMethed.queryAccount(testKey002,blockingStubFull);
    final Account attackInfo = PublicMethed.queryAccount(testKey001,blockingStubFull);
    final Account normal1Info = PublicMethed.queryAccount(normalKey001,blockingStubFull);
    final Account normal2Info = PublicMethed.queryAccount(normalKey002,blockingStubFull);
    final Account normal3Info = PublicMethed.queryAccount(normalKey003,blockingStubFull);
    final Account normal4Info = PublicMethed.queryAccount(normalKey004,blockingStubFull);
    beforeFromBalance = fromInfo.getBalance();
    beforeNormal1Balance = normal1Info.getBalance();
    beforeNormal2Balance = normal2Info.getBalance();
    beforeNormal3Balance = normal3Info.getBalance();
    beforeNormal4Balance = normal4Info.getBalance();
    beforeAttackBalance = attackInfo.getBalance();
    start = System.currentTimeMillis();
  }

  //@Test(enabled = true)
  @Test(enabled = false,threadPoolSize = 200, invocationCount = 200)
  public void freezeAndSendcoin() throws InterruptedException {

    Integer i = 0;
    Random rand = new Random();
    Integer randNum = 0;
    Integer n = 0;

    while (i < 20) {
      randNum = i % 4;
      i++;
      fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
          .get(randNum);
      channelFull = ManagedChannelBuilder.forTarget(fullnode)
          .usePlaintext(true)
          .build();
      blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

      if (randNum == 3) {
        PublicMethed.sendcoin(attackAddress, attackAmount, fromAddress, testKey002, 
            blockingStubFull);
        PublicMethed.sendcoin(attackAddress, attackAmount, fromAddress, testKey002, 
            blockingStubFull);
        /*        PublicMethed.sendcoin(attackAddress, attackAmount, fromAddress, testKey002,
            blockingStubFull);
        PublicMethed.sendcoin(attackAddress, attackAmount, fromAddress, testKey002,
            blockingStubFull);
        PublicMethed.sendcoin(attackAddress, attackAmount, fromAddress, testKey002,
            blockingStubFull);
        PublicMethed.sendcoin(attackAddress, attackAmount, fromAddress, testKey002,
            blockingStubFull);
        PublicMethed.sendcoin(attackAddress, attackAmount, fromAddress, testKey002,
            blockingStubFull);
        PublicMethed.sendcoin(attackAddress, attackAmount, fromAddress, testKey002,
            blockingStubFull);*/
      }

      if (randNum == 0) {
        PublicMethed.sendcoin(normal1Address, sendNromal1Amount, fromAddress, 
            testKey002, blockingStubFull);
        continue;
      }
      if (randNum == 1) {
        PublicMethed.sendcoin(normal2Address, sendNromal2Amount, fromAddress, 
            testKey002, blockingStubFull);
        continue;
      }
      if (randNum == 2) {
        PublicMethed.sendcoin(normal3Address, sendNromal3Amount, fromAddress, 
            testKey002, blockingStubFull);
        continue;
      }
      if (randNum == 3) {
        PublicMethed.sendcoin(normal4Address, sendNromal4Amount, fromAddress, 
            testKey002, blockingStubFull);
        continue;
      }
    }
  }

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    //Print the duration.
    end = System.currentTimeMillis();
    logger.info("The time is " + Long.toString(end - start));

    //Print 6 account balance information.
    final Account fromInfo = PublicMethed.queryAccount(testKey002,blockingStubFull);
    final Account attackInfo = PublicMethed.queryAccount(testKey001,blockingStubFull);
    final Account normal1Info = PublicMethed.queryAccount(normalKey001,blockingStubFull);
    final Account normal2Info = PublicMethed.queryAccount(normalKey002,blockingStubFull);
    final Account normal3Info = PublicMethed.queryAccount(normalKey003,blockingStubFull);
    final Account normal4Info = PublicMethed.queryAccount(normalKey004,blockingStubFull);

    afterFromBalance = fromInfo.getBalance();
    afterNormal1Balance = normal1Info.getBalance();
    afterNormal2Balance = normal2Info.getBalance();
    afterNormal3Balance = normal3Info.getBalance();
    afterNormal4Balance = normal4Info.getBalance();
    afterAttackBalance = attackInfo.getBalance();

    logger.info("attack transaction success num is " 
        + (afterAttackBalance - beforeAttackBalance) / attackAmount);
    logger.info("Normal 1 transaction success num is " 
        + (afterNormal1Balance - beforeNormal1Balance) / sendNromal1Amount);
    logger.info("Normal 2 transaction success num is " 
        + (afterNormal2Balance - beforeNormal2Balance) / sendNromal2Amount);
    logger.info("Normal 3 transaction success num is " 
        + (afterNormal3Balance - beforeNormal3Balance) / sendNromal3Amount);
    logger.info("Normal 4 transaction success num is " 
        + (afterNormal4Balance - beforeNormal4Balance) / sendNromal4Amount);

    Long totalSuccessNum = (afterAttackBalance - beforeAttackBalance) / attackAmount
        + (afterNormal1Balance - beforeNormal1Balance) / sendNromal1Amount
        + (afterNormal3Balance - beforeNormal3Balance) / sendNromal3Amount
        + (afterNormal4Balance - beforeNormal4Balance) / sendNromal4Amount
        + (afterNormal2Balance - beforeNormal2Balance) / sendNromal2Amount;
    logger.info("Total success transaction is " + Long.toString(totalSuccessNum));

    Long normaltotalSuccessNum = (afterNormal1Balance - beforeNormal1Balance) / sendNromal1Amount
        + (afterNormal3Balance - beforeNormal3Balance) / sendNromal3Amount
        + (afterNormal4Balance - beforeNormal4Balance) / sendNromal4Amount
        + (afterNormal2Balance - beforeNormal2Balance) / sendNromal2Amount;
    logger.info("Total normal success transaction is " + Long.toString(normaltotalSuccessNum));




    Integer blockTimes = 0;
    Integer blockTransNum = 0;

    while (blockTimes < 5) {
      blockTimes++;
      //Print the current block transaction num.
      Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
      Long currentNum = currentBlock.getBlockHeader().getRawData().getNumber();
      logger.info("The block num " + Long.toString(currentNum)
          + "total transaction is " + Long.toString(currentBlock.getTransactionsCount()));
      //logger.info(Integer.toString(currentBlock.getTransactionsList()
      // .get(0).getRawData().getContract(0).getTypeValue()));

      Integer normal1Num = 0;
      Integer normal2Num = 0;
      Integer normal3Num = 0;
      Integer normal4Num = 0;
      Integer attackNum = 0;
      Long temp = 0L;
      for (Integer m = 0; m < currentBlock.getTransactionsCount(); m++) {
        try {
          temp = currentBlock.getTransactions(m).getRawData().getContract(0).getParameter()
              .unpack(TransferContract.class).getAmount();
        } catch (InvalidProtocolBufferException e) {
          e.printStackTrace();
        }
        if (temp == sendNromal1Amount) {
          normal1Num++;
        }
        if (temp == sendNromal2Amount) {
          normal2Num++;
        }
        if (temp == sendNromal3Amount) {
          normal3Num++;
        }
        if (temp == sendNromal4Amount) {
          normal4Num++;
        }
        if (temp == attackAmount) {
          attackNum++;
        }
      }
      logger.info("Block num " + Long.toString(currentNum) + ", Attack num is "
          + Integer.toString(attackNum));
      logger.info("Block num " + Long.toString(currentNum) + ", normal 1 num is "
          + Integer.toString(normal1Num));
      logger.info("Block num " + Long.toString(currentNum) + ", normal 2 num is "
          + Integer.toString(normal2Num));
      logger.info("Block num " + Long.toString(currentNum) + ", normal 3 num is "
          + Integer.toString(normal3Num));
      logger.info("Block num " + Long.toString(currentNum) + ", normal 4 num is "
          + Integer.toString(normal4Num));
      blockTransNum = blockTransNum + currentBlock.getTransactionsCount();
      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    logger.info("Total block record num is " + Integer.toString(blockTransNum));


  }

  public static Boolean freezeBalance(byte[] addRess, long freezeBalance, long freezeDuration,
      String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
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
    Protocol.Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI
        .EmptyMessage.newBuilder().build());
    final Long beforeBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Long beforeFrozenBalance = 0L;
    //Long beforeBandwidth     = beforeFronzen.getBandwidth();

    Contract.FreezeBalanceContract.Builder builder = Contract.FreezeBalanceContract.newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);

    builder.setOwnerAddress(byteAddreess).setFrozenBalance(frozenBalance)
        .setFrozenDuration(frozenDuration);

    Contract.FreezeBalanceContract contract = builder.build();
    Transaction transaction = blockingStubFull.freezeBalance(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction = null");
      return false;
    }

    transaction = TransactionUtils.setTimestamp(transaction);
    transaction = TransactionUtils.sign(transaction, ecKey);
    Return response = blockingStubFull.broadcastTransaction(transaction);

    if (response.getResult() == false) {
      logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      return false;
    }

    Long afterBlockNum = 0L;

    while (afterBlockNum < beforeBlockNum) {
      Protocol.Block currentBlock1 = blockingStubFull.getNowBlock(GrpcAPI
          .EmptyMessage.newBuilder().build());
      afterBlockNum = currentBlock1.getBlockHeader().getRawData().getNumber();
    }
    return true;
  }

  public boolean unFreezeBalance(byte[] addRess, String priKey) {
    byte[] address = addRess;

    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    // Account search = queryAccount(ecKey, blockingStubFull);

    UnfreezeBalanceContract.Builder builder = UnfreezeBalanceContract
        .newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);

    builder.setOwnerAddress(byteAddreess);

    UnfreezeBalanceContract contract = builder.build();

    Transaction transaction = blockingStubFull.unfreezeBalance(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }

    transaction = TransactionUtils.setTimestamp(transaction);
    transaction = TransactionUtils.sign(transaction, ecKey);
    Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      return false;
    } else {
      return true;
    }
  }

  public boolean withdrawBalance(byte[] address, String priKey) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;

    Contract.WithdrawBalanceContract.Builder builder = Contract.WithdrawBalanceContract
        .newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);
    builder.setOwnerAddress(byteAddreess);
    Contract.WithdrawBalanceContract contract = builder.build();

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

  private Transaction signTransaction(ECKey ecKey, Transaction transaction) {
    if (ecKey == null || ecKey.getPrivKey() == null) {
      logger.warn("Warning: Can't sign,there is no private key !!");
      return null;
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    return TransactionUtils.sign(transaction, ecKey);
  }

}


