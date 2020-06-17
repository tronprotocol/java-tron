package stest.tron.wallet.onlinestress;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

//import org.tron.protos.Protocol.DeferredTransaction;

@Slf4j
public class DelayTransactionStress {

  public static final long MAX_DEFERRED_TRANSACTION_DELAY_SECONDS = 45 * 24 * 3_600L; //45 days
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  Optional<TransactionInfo> infoById = null;
  //Optional<DeferredTransaction> deferredTransactionById = null;
  Optional<Transaction> getTransactionById = null;
  ECKey ecKey = new ECKey(Utils.getRandom());
  byte[] delayAccount1Address = ecKey.getAddress();
  String delayAccount1Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] delayAccount2Address = ecKey2.getAddress();
  String delayAccount2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] receiverAccountAddress = ecKey3.getAddress();
  String receiverAccountKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
  ECKey ecKey4 = new ECKey(Utils.getRandom());
  byte[] delayAccount3Address = ecKey4.getAddress();
  String delayAccount3Key = ByteArray.toHexString(ecKey4.getPrivKeyBytes());
  ECKey ecKey5 = new ECKey(Utils.getRandom());
  byte[] receiverAccount4Address = ecKey5.getAddress();
  String receiverAccount4Key = ByteArray.toHexString(ecKey5.getPrivKeyBytes());
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private Long delayTransactionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.delayTransactionFee");

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
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

  @Test(enabled = true, threadPoolSize = 30, invocationCount = 2000)
  public void test1DelaySendcoinStress() {
    String txid = "";
    Integer i = 0;
    String cancelId = "";
    while (i++ <= 10000000) {
      ECKey ecKey2 = new ECKey(Utils.getRandom());
      byte[] delayAccount2Address = ecKey2.getAddress();
      String delayAccount2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

      txid = PublicMethed.sendcoinDelayedGetTxid(delayAccount2Address, 1L, 20, fromAddress,
          testKey002, blockingStubFull);
      //PublicMethed.waitProduceNextBlock(blockingStubFull);
      if (i % 20 == 0) {
        cancelId = txid;
        //PublicMethed.sendcoin(delayAccount2Address,1L,fromAddress,testKey002,blockingStubFull);
      }
      if (i % 39 == 0) {
        PublicMethed.cancelDeferredTransactionById(cancelId, fromAddress, testKey002,
            blockingStubFull);
        PublicMethed.sendcoin(delayAccount2Address, 1L, fromAddress, testKey002,
            blockingStubFull);
      }

    }


  }

  /*  @Test(enabled = true, description = "Get deferred transaction by id")
  public void test2getDeferredTransactionByid() {
    //get account
    ECKey ecKey2 = new ECKey(Utils.getRandom());
    byte[] delayAccount2Address = ecKey2.getAddress();
    String delayAccount2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    ecKey3 = new ECKey(Utils.getRandom());
    receiverAccountAddress = ecKey3.getAddress();
    receiverAccountKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
    PublicMethed.printAddress(delayAccount2Key);
    PublicMethed.printAddress(receiverAccountKey);

    //Pre sendcoin to the test account
    Assert.assertTrue(PublicMethed.sendcoin(delayAccount2Address, 100000000L,fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(receiverAccountAddress, 10L,fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //Do delay send coin transaction.
    Long delaySecond = 10L;
    Long sendCoinAmout = 100L;

    //Query balance before send coin.
    Long deplayAccountBeforeBalance = PublicMethed.queryAccount(delayAccount2Address,
       blockingStubFull).getBalance();
    Long recevierAccountBeforeBalance = PublicMethed.queryAccount(receiverAccountAddress,
       blockingStubFull).getBalance();
    logger.info("deplayAccountBeforeBalance " + deplayAccountBeforeBalance);
    logger.info("recevierAccountBeforeBalance " + recevierAccountBeforeBalance);
    String txid = PublicMethed.sendcoinDelayedGetTxid(receiverAccountAddress, sendCoinAmout,
       delaySecond,delayAccount2Address,
        delayAccount2Key, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //Query balance when pre-sendcoin stage.
    Long deplayAccountInDelayBalance = PublicMethed.queryAccount(delayAccount2Address,
       blockingStubFull).getBalance();
    Long recevierAccountInDelayalance = PublicMethed.queryAccount(receiverAccountAddress,
       blockingStubFull).getBalance();
    logger.info("deplayAccountInDelayBalance " + deplayAccountInDelayBalance);
    logger.info("recevierAccountInDelayalance " + recevierAccountInDelayalance);
    Assert.assertTrue(recevierAccountBeforeBalance == recevierAccountInDelayalance);
    //Assert.assertTrue();


    deferredTransactionById = PublicMethed.getDeferredTransactionById(txid,blockingStubFull);
    DeferredTransaction transaction = deferredTransactionById.get();
    String finalTxid = ByteArray.toHexString(Sha256Hash.hash(transaction.getTransaction()
      .getRawData().toByteArray()));
    PublicMethed.getDeferredTransactionById(finalTxid,blockingStubFull);
    logger.info(finalTxid);
    //logger.info("receiver address is " + Base58.encode58Check(transaction
      .getReceiverAddress().toByteArray()));
    Assert.assertTrue(Base58.encode58Check(transaction.getReceiverAddress().toByteArray())
      .equalsIgnoreCase(PublicMethed.getAddressString(receiverAccountKey)));
    //logger.info("sender address is " + Base58.encode58Check(transaction
      .getSenderAddress().toByteArray()));
    Assert.assertTrue(Base58.encode58Check(transaction.getSenderAddress().toByteArray())
      .equalsIgnoreCase(PublicMethed.getAddressString(delayAccount2Key)));
   // logger.info("delaySeconds is " + transaction.getDelaySeconds());
    Assert.assertTrue(delaySecond == transaction.getDelaySeconds());
    //logger.info("DelayUntil " + transaction.getDelayUntil());
    Assert.assertTrue(transaction.getDelayUntil() > System.currentTimeMillis());
    //logger.info("Expiration " + transaction.getExpiration());
    Assert.assertTrue(transaction.getExpiration() > System.currentTimeMillis());
    //logger.info("PublishTime " + transaction.getPublishTime());
    Assert.assertTrue(transaction.getPublishTime() < System.currentTimeMillis());
    //Assert.assertTrue(transaction.getDelayUntil() + 60000 == transaction.getExpiration());
    getTransactionById = PublicMethed.getTransactionById(txid, blockingStubFull);
    logger.info("transaction stage in txid is " + getTransactionById.get().getRawData()
      .getDeferredStage().getStage());

    Assert.assertTrue(getTransactionById.get().getRawData().getDeferredStage().getStage() == 1);
    getTransactionById = PublicMethed.getTransactionById(finalTxid, blockingStubFull);
    logger.info("transaction stage in final id is " + getTransactionById
      .get().getRawData().getDeferredStage().getStage());
    Assert.assertTrue(getTransactionById.get().getRawData().getDeferredStage().getStage() == 0);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    PublicMethed.getDeferredTransactionById(finalTxid,blockingStubFull);
    deferredTransactionById = PublicMethed.getDeferredTransactionById(txid,blockingStubFull);
    finalTxid = ByteArray.toHexString(Sha256Hash.hash(transaction.getTransaction()
      .getRawData().toByteArray()));
    transaction = deferredTransactionById.get();
    logger.info(finalTxid);
    //logger.info("receiver address is " + Base58.encode58Check(transaction.getReceiverAddress()
      .toByteArray()));
    //logger.info("receiver address is " + Base58.encode58Check(transaction.getSenderAddress()
      .toByteArray()));
    //logger.info("delaySeconds is " + transaction.getDelaySeconds());
    //logger.info("DelayUntil " + transaction.getDelayUntil());
    //logger.info("Expiration " + transaction.getExpiration());
    //logger.info("PublishTime " + transaction.getPublishTime());
    getTransactionById = PublicMethed.getTransactionById(txid, blockingStubFull);
    logger.info("transaction stage in txid is " + getTransactionById.get().getRawData()
      .getDeferredStage().getStage());
    getTransactionById = PublicMethed.getTransactionById(finalTxid, blockingStubFull);
    logger.info("transaction stage in final id is " + getTransactionById.get().getRawData()
      .getDeferredStage().getStage());



    //Query balance after delay send coin.
    Long deplayAccountAfterBalance = PublicMethed.queryAccount(delayAccount2Address,
       blockingStubFull).getBalance();
    Long recevierAccountAfterDelayalance = PublicMethed.queryAccount(receiverAccountAddress,
       blockingStubFull).getBalance();
    logger.info("deplayAccountAfterBalance " + deplayAccountAfterBalance);
    logger.info("recevierAccountAfterDelayalance " + recevierAccountAfterDelayalance);

    Assert.assertTrue(deplayAccountBeforeBalance - deplayAccountAfterBalance == sendCoinAmout
       + 100000L);
    Assert.assertTrue(recevierAccountAfterDelayalance - recevierAccountBeforeBalance
       == sendCoinAmout);

  }*/

  @Test(enabled = true, description = "Delay send coin")
  public void test3DelaySendCoin() {
    ecKey4 = new ECKey(Utils.getRandom());
    delayAccount3Address = ecKey4.getAddress();
    delayAccount3Key = ByteArray.toHexString(ecKey4.getPrivKeyBytes());
    PublicMethed.printAddress(delayAccount3Key);

    ecKey5 = new ECKey(Utils.getRandom());
    receiverAccount4Address = ecKey5.getAddress();
    receiverAccount4Key = ByteArray.toHexString(ecKey5.getPrivKeyBytes());
    PublicMethed.printAddress(receiverAccount4Key);

    Long sendCoinAmount = 100000000L;
    //Pre sendcoin to the test account
    Assert.assertTrue(PublicMethed.sendcoin(delayAccount3Address, sendCoinAmount, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //Do delay send coin transaction.
    logger.info("----------------No balance to send coin--------------------");
    //Test no balance to send coin.
    //Query balance before send coin.
    Long deplayAccountBeforeBalance = PublicMethed.queryAccount(delayAccount3Address,
        blockingStubFull).getBalance();
    Long recevierAccountBeforeBalance = PublicMethed.queryAccount(receiverAccount4Address,
        blockingStubFull).getBalance();
    logger.info("deplayAccountBeforeBalance " + deplayAccountBeforeBalance);
    logger.info("recevierAccountBeforeBalance " + recevierAccountBeforeBalance);
    Long delaySecond = 4L;
    Assert.assertFalse(PublicMethed.sendcoinDelayed(receiverAccount4Address, sendCoinAmount,
        delaySecond, delayAccount3Address, delayAccount3Key, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //Query balance after delay send coin.
    Long deplayAccountAfterBalance = PublicMethed.queryAccount(delayAccount3Address,
        blockingStubFull).getBalance();
    Long recevierAccountAfterDelayalance = PublicMethed.queryAccount(receiverAccount4Address,
        blockingStubFull).getBalance();
    logger.info("deplayAccountAfterBalance " + deplayAccountAfterBalance);
    logger.info("recevierAccountAfterDelayalance " + recevierAccountAfterDelayalance);

    Assert.assertTrue(recevierAccountAfterDelayalance == 0);
    logger.info("deplayAccountBeforeBalance: " + deplayAccountBeforeBalance);
    logger.info("deplayAccountAfterBalance: " + deplayAccountAfterBalance);

    Assert.assertEquals(deplayAccountBeforeBalance, deplayAccountAfterBalance);

    logger.info("----------------No balance to create account send coin--------------------");
    //Test delay send coin to create account.
    deplayAccountBeforeBalance = PublicMethed.queryAccount(delayAccount3Address,
        blockingStubFull).getBalance();
    recevierAccountBeforeBalance = PublicMethed.queryAccount(receiverAccount4Address,
        blockingStubFull).getBalance();
    logger.info("deplayAccountBeforeBalance " + deplayAccountBeforeBalance);
    logger.info("recevierAccountBeforeBalance " + recevierAccountBeforeBalance);
    Long createAccountFee = 100000L;
    Assert.assertTrue(PublicMethed.sendcoinDelayed(receiverAccount4Address,
        deplayAccountBeforeBalance - createAccountFee, delaySecond, delayAccount3Address,
        delayAccount3Key, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //Query balance after delay send coin.
    deplayAccountAfterBalance = PublicMethed.queryAccount(delayAccount3Address, blockingStubFull)
        .getBalance();
    recevierAccountAfterDelayalance = PublicMethed.queryAccount(receiverAccount4Address,
        blockingStubFull).getBalance();
    logger.info("deplayAccountAfterBalance " + deplayAccountAfterBalance);
    logger.info("recevierAccountAfterDelayalance " + recevierAccountAfterDelayalance);

    Assert.assertTrue(recevierAccountAfterDelayalance == 0);
    Assert.assertTrue(deplayAccountBeforeBalance - deplayAccountAfterBalance == 100000);

    logger.info("---------------Balance enough to create account send coin--------------------");
    //Test delay send coin to create account.
    createAccountFee = 100000L;
    deplayAccountBeforeBalance = PublicMethed.queryAccount(delayAccount3Address,
        blockingStubFull).getBalance();
    recevierAccountBeforeBalance = PublicMethed.queryAccount(receiverAccount4Address,
        blockingStubFull).getBalance();
    logger.info("deplayAccountBeforeBalance " + deplayAccountBeforeBalance);
    logger.info("recevierAccountBeforeBalance " + recevierAccountBeforeBalance);
    Assert.assertTrue(PublicMethed.sendcoinDelayed(receiverAccount4Address,
        deplayAccountBeforeBalance - createAccountFee - delayTransactionFee,
        delaySecond, delayAccount3Address, delayAccount3Key, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //Query balance after delay send coin.
    deplayAccountAfterBalance = PublicMethed.queryAccount(delayAccount3Address,
        blockingStubFull).getBalance();
    recevierAccountAfterDelayalance = PublicMethed.queryAccount(receiverAccount4Address,
        blockingStubFull).getBalance();
    logger.info("deplayAccountAfterBalance " + deplayAccountAfterBalance);
    logger.info("recevierAccountAfterDelayalance " + recevierAccountAfterDelayalance);
    Long receiverBalanceShouldBe = deplayAccountBeforeBalance - createAccountFee
        - delayTransactionFee;

    Assert.assertEquals(recevierAccountAfterDelayalance, receiverBalanceShouldBe);
    Assert.assertTrue(deplayAccountAfterBalance == 0);
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
}


