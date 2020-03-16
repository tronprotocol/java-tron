package stest.tron.wallet.dailybuild.delaytransaction;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.nio.charset.Charset;
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
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class DelayTransaction008 {

  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = now;
  private static final String name = "Asset008_" + Long.toString(now);
  private static String updateAccountName;
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  String description = "just-test";
  String url = "https://github.com/tronprotocol/wallet-cli/";
  Long delaySecond = 10L;
  ByteString assetId;
  SmartContract smartContract;
  ECKey ecKey = new ECKey(Utils.getRandom());
  byte[] doUpdateAccountAddress = ecKey.getAddress();
  String doUpdateAccountKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] newAccountAddress = ecKey1.getAddress();
  String newAccountKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private Long delayTransactionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.delayTransactionFee");
  private Long cancleDelayTransactionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.cancleDelayTransactionFee");
  private byte[] contractAddress = null;

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

  @Test(enabled = false, description = "Delay account update contract")
  public void test1DelayAccountUpdate() {
    //get account
    ecKey = new ECKey(Utils.getRandom());
    doUpdateAccountAddress = ecKey.getAddress();
    doUpdateAccountKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    PublicMethed.printAddress(doUpdateAccountKey);

    Assert.assertTrue(PublicMethed.sendcoin(doUpdateAccountAddress, 1000000L, fromAddress,
        testKey002, blockingStubFull));

    final Long beforeUpdateAccountBalance = PublicMethed.queryAccount(doUpdateAccountKey,
        blockingStubFull).getBalance();
    updateAccountName = "account_" + Long.toString(System.currentTimeMillis());
    byte[] accountNameBytes = ByteArray.fromString(updateAccountName);
    final String txid = PublicMethed.updateAccountDelayGetTxid(doUpdateAccountAddress,
        accountNameBytes, delaySecond, doUpdateAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String accountName = new String(PublicMethed.queryAccount(doUpdateAccountKey,
        blockingStubFull).getAccountName().toByteArray(), Charset.forName("UTF-8"));
    Assert.assertTrue(accountName.isEmpty());
    Assert.assertTrue(PublicMethed.queryAccount(newAccountAddress, blockingStubFull)
        .getAccountName().isEmpty());
    Long balanceInDelay = PublicMethed.queryAccount(doUpdateAccountKey, blockingStubFull)
        .getBalance();
    Assert.assertTrue(beforeUpdateAccountBalance - balanceInDelay == delayTransactionFee);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    accountName = new String(PublicMethed.queryAccount(doUpdateAccountKey, blockingStubFull)
        .getAccountName().toByteArray(), Charset.forName("UTF-8"));
    logger.info(accountName);
    Assert.assertTrue(accountName.equalsIgnoreCase(updateAccountName));
    Long afterCreateAccountBalance = PublicMethed.queryAccount(doUpdateAccountKey, blockingStubFull)
        .getBalance();
    Long netFee = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get().getReceipt()
        .getNetFee();
    Long fee = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get().getFee();
    Assert.assertTrue(fee - netFee == delayTransactionFee);
    Assert.assertTrue(beforeUpdateAccountBalance - afterCreateAccountBalance
        == delayTransactionFee);

  }

  @Test(enabled = false, description = "Cancel delay account update contract")
  public void test2CancelDelayUpdateAccount() {
    //get account
    ecKey = new ECKey(Utils.getRandom());
    doUpdateAccountAddress = ecKey.getAddress();
    doUpdateAccountKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    PublicMethed.printAddress(doUpdateAccountKey);

    final Long beforeUpdateAccountBalance = PublicMethed.queryAccount(doUpdateAccountKey,
        blockingStubFull).getBalance();
    updateAccountName = "account_" + Long.toString(System.currentTimeMillis());
    byte[] accountNameBytes = ByteArray.fromString(updateAccountName);
    final String txid = PublicMethed.updateAccountDelayGetTxid(doUpdateAccountAddress,
        accountNameBytes, delaySecond, doUpdateAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertFalse(PublicMethed.cancelDeferredTransactionById(txid, fromAddress, testKey002,
        blockingStubFull));
    final String cancelTxid = PublicMethed.cancelDeferredTransactionByIdGetTxid(txid,
        doUpdateAccountAddress, doUpdateAccountKey, blockingStubFull);
    Assert.assertFalse(PublicMethed.cancelDeferredTransactionById(txid, doUpdateAccountAddress,
        doUpdateAccountKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    final Long afterUpdateBalance = PublicMethed.queryAccount(doUpdateAccountKey, blockingStubFull)
        .getBalance();
    final Long netFee = PublicMethed.getTransactionInfoById(cancelTxid, blockingStubFull).get()
        .getReceipt().getNetFee();
    final Long fee = PublicMethed.getTransactionInfoById(cancelTxid, blockingStubFull).get()
        .getFee();
    logger.info("net fee : " + PublicMethed.getTransactionInfoById(cancelTxid, blockingStubFull)
        .get().getReceipt().getNetFee());
    logger.info("Fee : " + PublicMethed.getTransactionInfoById(cancelTxid, blockingStubFull)
        .get().getFee());

    Assert.assertTrue(fee - netFee == cancleDelayTransactionFee);
    Assert.assertTrue(beforeUpdateAccountBalance - afterUpdateBalance
        == cancleDelayTransactionFee + delayTransactionFee);

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


