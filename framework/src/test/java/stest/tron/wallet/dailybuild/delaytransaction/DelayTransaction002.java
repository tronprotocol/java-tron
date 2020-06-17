package stest.tron.wallet.dailybuild.delaytransaction;

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
public class DelayTransaction002 {

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
      .get(1);
  private Long delayTransactionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.delayTransactionFee");
  private Long cancleDelayTransactionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.cancleDelayTransactionFee");

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = false)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = false, description = "Cancel deferred transaction")
  public void test1CancleDeferredTransaction() {
    //get account
    ecKey = new ECKey(Utils.getRandom());
    delayAccount1Address = ecKey.getAddress();
    delayAccount1Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    PublicMethed.printAddress(delayAccount1Key);
    ecKey3 = new ECKey(Utils.getRandom());
    receiverAccountAddress = ecKey3.getAddress();
    receiverAccountKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
    PublicMethed.printAddress(receiverAccountKey);

    Assert.assertTrue(PublicMethed.sendcoin(delayAccount1Address, 100000000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(receiverAccountAddress, 100L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //Do delay send coin transaction.
    Long delaySecond = 10L;
    Long sendCoinAmount = 1L;

    //Query balance before send coin.
    Long deplayAccountBeforeBalance = PublicMethed.queryAccount(delayAccount1Address,
        blockingStubFull).getBalance();
    Long recevierAccountBeforeBalance = PublicMethed.queryAccount(receiverAccountAddress,
        blockingStubFull).getBalance();
    logger.info("deplayAccountBeforeBalance " + deplayAccountBeforeBalance);
    logger.info("recevierAccountBeforeBalance " + recevierAccountBeforeBalance);
    String txid = PublicMethed.sendcoinDelayedGetTxid(receiverAccountAddress, sendCoinAmount,
        delaySecond, delayAccount1Address,
        delayAccount1Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    //deferredTransactionById = PublicMethed.getDeferredTransactionById(txid,blockingStubFull);
    //DeferredTransaction transaction = deferredTransactionById.get();
    //String finalTxid = ByteArray.toHexString(Sha256Hash.hash(transaction.getTransaction()
    //   .getRawData().toByteArray()));

    //Assert.assertFalse(PublicMethed.cancelDeferredTransactionById(finalTxid,receiverAccountAddress
    //   ,receiverAccountKey,blockingStubFull));
    Assert.assertFalse(PublicMethed.cancelDeferredTransactionById(txid, receiverAccountAddress,
        receiverAccountKey, blockingStubFull));
    Assert.assertTrue(PublicMethed.cancelDeferredTransactionById(txid, delayAccount1Address,
        delayAccount1Key, blockingStubFull));
    Assert.assertFalse(PublicMethed.cancelDeferredTransactionById(txid, delayAccount1Address,
        delayAccount1Key, blockingStubFull));
    PublicMethed.cancelDeferredTransactionById(txid, delayAccount1Address, delayAccount1Key,
        blockingStubFull);
    PublicMethed.cancelDeferredTransactionById(txid, delayAccount1Address, delayAccount1Key,
        blockingStubFull);
    PublicMethed.cancelDeferredTransactionById(txid, delayAccount1Address, delayAccount1Key,
        blockingStubFull);
    PublicMethed.cancelDeferredTransactionById(txid, delayAccount1Address, delayAccount1Key,
        blockingStubFull);
    PublicMethed.cancelDeferredTransactionById(txid, delayAccount1Address, delayAccount1Key,
        blockingStubFull);
    PublicMethed.cancelDeferredTransactionById(txid, delayAccount1Address, delayAccount1Key,
        blockingStubFull);
    PublicMethed.cancelDeferredTransactionById(txid, delayAccount1Address, delayAccount1Key,
        blockingStubFull);
    PublicMethed.cancelDeferredTransactionById(txid, delayAccount1Address, delayAccount1Key,
        blockingStubFull);
    PublicMethed.cancelDeferredTransactionById(txid, delayAccount1Address, delayAccount1Key,
        blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Long deplayAccountAfterBalance = PublicMethed.queryAccount(delayAccount1Address,
        blockingStubFull).getBalance();
    Long recevierAccountAfterDelayalance = PublicMethed.queryAccount(receiverAccountAddress,
        blockingStubFull).getBalance();
    logger.info("deplayAccountAfterBalance " + deplayAccountAfterBalance);
    logger.info("recevierAccountAfterDelayalance " + recevierAccountAfterDelayalance);
    //Assert.assertFalse(PublicMethed.cancelDeferredTransactionById(txid,delayAccount1Address,
    //   delayAccount1Key,blockingStubFull));
    //PublicMethed.cancelDeferredTransactionById(txid,delayAccount1Address,delayAccount1Key,
    //   blockingStubFull);
    //PublicMethed.cancelDeferredTransactionById(txid,delayAccount1Address,delayAccount1Key,
    // blockingStubFull);
    //PublicMethed.cancelDeferredTransactionById(txid,delayAccount1Address,delayAccount1Key,
    // blockingStubFull);
    //PublicMethed.cancelDeferredTransactionById(txid,delayAccount1Address,delayAccount1Key,
    // blockingStubFull);
    //PublicMethed.cancelDeferredTransactionById(txid,delayAccount1Address,delayAccount1Key,
    // blockingStubFull);

    Assert.assertTrue(deplayAccountBeforeBalance - deplayAccountAfterBalance
        == delayTransactionFee + cancleDelayTransactionFee);
    Assert.assertTrue(recevierAccountBeforeBalance == recevierAccountAfterDelayalance);

  }

  @Test(enabled = false, description = "Cancel deferred transaction")
  public void test2CancleDeferredTransactionQuickly() {
    //get account
    ecKey = new ECKey(Utils.getRandom());
    delayAccount1Address = ecKey.getAddress();
    delayAccount1Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    PublicMethed.printAddress(delayAccount1Key);
    ecKey3 = new ECKey(Utils.getRandom());
    receiverAccountAddress = ecKey3.getAddress();
    receiverAccountKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
    PublicMethed.printAddress(receiverAccountKey);

    Assert.assertTrue(PublicMethed.sendcoin(delayAccount1Address, 100000000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(receiverAccountAddress, 100L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //Do delay send coin transaction.
    Long delaySecond = 10L;
    Long sendCoinAmount = 1000L;

    //Query balance before send coin.
    Long deplayAccountBeforeBalance = PublicMethed.queryAccount(delayAccount1Address,
        blockingStubFull).getBalance();
    Long recevierAccountBeforeBalance = PublicMethed.queryAccount(receiverAccountAddress,
        blockingStubFull).getBalance();
    logger.info("deplayAccountBeforeBalance " + deplayAccountBeforeBalance);
    logger.info("recevierAccountBeforeBalance " + recevierAccountBeforeBalance);
    String txid = PublicMethed.sendcoinDelayedGetTxid(receiverAccountAddress, sendCoinAmount,
        delaySecond, delayAccount1Address,
        delayAccount1Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertFalse(PublicMethed.cancelDeferredTransactionById(txid, receiverAccountAddress,
        receiverAccountKey, blockingStubFull));
    Assert.assertTrue(PublicMethed.cancelDeferredTransactionById(txid, delayAccount1Address,
        delayAccount1Key, blockingStubFull));
    Assert.assertFalse(PublicMethed.cancelDeferredTransactionById(txid, delayAccount1Address,
        delayAccount1Key, blockingStubFull));
    //PublicMethed.waitProduceNextBlock(blockingStubFull);

    Long deplayAccountAfterBalance = PublicMethed.queryAccount(delayAccount1Address,
        blockingStubFull).getBalance();
    Long recevierAccountAfterDelayalance = PublicMethed.queryAccount(receiverAccountAddress,
        blockingStubFull).getBalance();
    logger.info("deplayAccountAfterBalance " + deplayAccountAfterBalance);
    logger.info("recevierAccountAfterDelayalance " + recevierAccountAfterDelayalance);
    Assert.assertFalse(PublicMethed.cancelDeferredTransactionById(txid, delayAccount1Address,
        delayAccount1Key, blockingStubFull));

    Assert.assertTrue(deplayAccountBeforeBalance - deplayAccountAfterBalance
        == delayTransactionFee + cancleDelayTransactionFee);
    Assert.assertTrue(recevierAccountBeforeBalance == recevierAccountAfterDelayalance);


  }


  /**
   * constructor.
   */

  @AfterClass(enabled = false)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


