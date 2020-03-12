package stest.tron.wallet.dailybuild.delaytransaction;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
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
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

//import org.tron.protos.Protocol.DeferredTransaction;

@Slf4j
public class DelayTransaction010 {

  public static final long ONE_DELAY_SECONDS = 60 * 60 * 24L;
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  ECKey ecKey = new ECKey(Utils.getRandom());
  byte[] delayFeeAccountAddress = ecKey.getAddress();
  String delayFeeAccountKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] delayAccount2Address = ecKey2.getAddress();
  //Optional<DeferredTransaction> deferredTransactionById = null;
  String delayAccount2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
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

  @Test(enabled = false, description = "Delayed transaction cost 0.1TRX every day.")
  public void test1TestDelayedTransactionFee() {
    //get account
    ecKey = new ECKey(Utils.getRandom());
    delayFeeAccountAddress = ecKey.getAddress();
    delayFeeAccountKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    ecKey2 = new ECKey(Utils.getRandom());
    delayAccount2Address = ecKey2.getAddress();
    delayAccount2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    PublicMethed.printAddress(delayFeeAccountKey);
    Assert.assertTrue(PublicMethed.sendcoin(delayFeeAccountAddress, 100000000L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    final String txidZeroDay = PublicMethed.sendcoinDelayedGetTxid(delayAccount2Address, 1L,
        0, delayFeeAccountAddress, delayFeeAccountKey, blockingStubFull);
    PublicMethed.cancelDeferredTransactionByIdGetTxid(txidZeroDay,
        delayFeeAccountAddress, delayFeeAccountKey, blockingStubFull);
    final String txidOneDay = PublicMethed.sendcoinDelayedGetTxid(delayAccount2Address, 1L,
        ONE_DELAY_SECONDS - 1, delayFeeAccountAddress, delayFeeAccountKey, blockingStubFull);
    final String txidTwoDay = PublicMethed.sendcoinDelayedGetTxid(delayAccount2Address, 1L,
        ONE_DELAY_SECONDS * 2 - 1, delayFeeAccountAddress, delayFeeAccountKey, blockingStubFull);
    final String txidFiveDay = PublicMethed.sendcoinDelayedGetTxid(delayAccount2Address, 1L,
        ONE_DELAY_SECONDS * 6 - 1, delayFeeAccountAddress, delayFeeAccountKey, blockingStubFull);
    final String txidTenDay = PublicMethed.sendcoinDelayedGetTxid(delayAccount2Address, 1L,
        ONE_DELAY_SECONDS * 9, delayFeeAccountAddress, delayFeeAccountKey, blockingStubFull);
    final String txid45Day = PublicMethed.sendcoinDelayedGetTxid(delayAccount2Address, 1L,
        ONE_DELAY_SECONDS * 45, delayFeeAccountAddress, delayFeeAccountKey, blockingStubFull);
    final String txid28Day = PublicMethed.sendcoinDelayedGetTxid(delayAccount2Address, 1L,
        ONE_DELAY_SECONDS * 28 - 1000, delayFeeAccountAddress, delayFeeAccountKey,
        blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.getTransactionInfoById(txidZeroDay, blockingStubFull)
        .get().getFee() == delayTransactionFee);
    Assert.assertTrue(PublicMethed.getTransactionInfoById(txidOneDay, blockingStubFull)
        .get().getFee() == delayTransactionFee);
    Assert.assertTrue(PublicMethed.getTransactionInfoById(txidTwoDay, blockingStubFull)
        .get().getFee() == delayTransactionFee * 2);
    Assert.assertTrue(PublicMethed.getTransactionInfoById(txidFiveDay, blockingStubFull)
        .get().getFee() == delayTransactionFee * 6);
    Assert.assertTrue(PublicMethed.getTransactionInfoById(txidTenDay, blockingStubFull)
        .get().getFee() == delayTransactionFee * 10);
    Assert.assertTrue(PublicMethed.getTransactionInfoById(txid45Day, blockingStubFull)
        .get().getFee() == delayTransactionFee * 46);
    Assert.assertTrue(PublicMethed.getTransactionInfoById(txid28Day, blockingStubFull)
        .get().getFee() == delayTransactionFee * 28);

    String cancelTxid = PublicMethed.cancelDeferredTransactionByIdGetTxid(txid28Day,
        delayFeeAccountAddress, delayFeeAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.getTransactionInfoById(cancelTxid, blockingStubFull)
        .get().getFee() == cancleDelayTransactionFee);
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Delayed transaction finally fialed.")
  public void test2DelaydTransactionFinallyFailed() {
    Long sendAmount = 12345L;
    Long beforeBalance = PublicMethed.queryAccount(delayFeeAccountAddress, blockingStubFull)
        .getBalance();
    final String preTxid = PublicMethed.sendcoinDelayedGetTxid(delayAccount2Address, sendAmount,
        6L, delayFeeAccountAddress, delayFeeAccountKey, blockingStubFull);
    try {
      Thread.sleep(3000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    //deferredTransactionById = PublicMethed.getDeferredTransactionById(preTxid,blockingStubFull);
    //DeferredTransaction transaction = deferredTransactionById.get();
    //String finalTxid = ByteArray.toHexString(Sha256Hash.hash(transaction.getTransaction()
    // .getRawData().toByteArray()));

    Assert.assertTrue(PublicMethed.sendcoin(fromAddress, PublicMethed.queryAccount(
        delayFeeAccountAddress, blockingStubFull).getBalance(), delayFeeAccountAddress,
        delayFeeAccountKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //Assert.assertTrue(PublicMethed.getTransactionById(finalTxid,blockingStubFull)
    // .get().getRawData().getContractCount() == 0);

    Long afterBalance = PublicMethed.queryAccount(delayFeeAccountAddress, blockingStubFull)
        .getBalance();
    Long afterSendCoinAccount2Balance = PublicMethed.queryAccount(delayAccount2Address,
        blockingStubFull).getBalance();
    Long beforeSendCoinAccount2Balance = PublicMethed.queryAccount(delayAccount2Address,
        blockingStubFull).getBalance();
    //Assert.assertTrue(beforeBalance - afterBalance == delayTransactionFee);
    Assert.assertTrue(beforeSendCoinAccount2Balance == afterSendCoinAccount2Balance);
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Delayed transaction finally successfully even during "
      + "delaying time the account has no money has no money.")
  public void test3DelaydTransactionFinallySuccessfully() {
    Assert.assertTrue(PublicMethed.sendcoin(delayFeeAccountAddress, 10000000L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Long sendAmount = 5432L;
    Long beforeBalance = PublicMethed.queryAccount(delayFeeAccountAddress, blockingStubFull)
        .getBalance();
    final String preTxid = PublicMethed.sendcoinDelayedGetTxid(delayAccount2Address, sendAmount,
        9L, delayFeeAccountAddress, delayFeeAccountKey, blockingStubFull);
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    //deferredTransactionById = PublicMethed.getDeferredTransactionById(preTxid,blockingStubFull);
    //DeferredTransaction transaction = deferredTransactionById.get();
    //String finalTxid = ByteArray.toHexString(Sha256Hash.hash(transaction.getTransaction()
    // .getRawData().toByteArray()));
    Assert.assertTrue(PublicMethed.sendcoin(fromAddress, PublicMethed.queryAccount(
        delayFeeAccountAddress, blockingStubFull).getBalance(), delayFeeAccountAddress,
        delayFeeAccountKey, blockingStubFull));
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    Assert.assertTrue(PublicMethed.sendcoin(delayFeeAccountAddress, 10000000L, fromAddress,
        testKey002, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //Assert.assertTrue(PublicMethed.getTransactionById(finalTxid,blockingStubFull)
    // .get().getRawData().getContractCount() == 1);

    Long afterBalance = PublicMethed.queryAccount(delayFeeAccountAddress, blockingStubFull)
        .getBalance();
    Long afterSendCoinAccount2Balance = PublicMethed.queryAccount(delayAccount2Address,
        blockingStubFull).getBalance();
    Long beforeSendCoinAccount2Balance = PublicMethed.queryAccount(delayAccount2Address,
        blockingStubFull).getBalance();
    Assert.assertTrue(beforeSendCoinAccount2Balance + sendAmount == afterSendCoinAccount2Balance);

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


