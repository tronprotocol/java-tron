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
public class DelayTransaction009 {

  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = now;
  private static final String name = "Asset008_" + Long.toString(now);
  private static String accountId;
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
  byte[] doSetIdAddress = ecKey.getAddress();
  String doSetIdKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
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

  @Test(enabled = false, description = "Delay set account id contract")
  public void test1DelaySetAccountId() {
    //get account
    ecKey = new ECKey(Utils.getRandom());
    doSetIdAddress = ecKey.getAddress();
    doSetIdKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    PublicMethed.printAddress(doSetIdKey);

    Assert.assertTrue(PublicMethed.sendcoin(doSetIdAddress, 10000000L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    final Long beforeSetAccountIdBalance = PublicMethed.queryAccount(doSetIdKey,
        blockingStubFull).getBalance();
    accountId = "accountId_" + Long.toString(System.currentTimeMillis());
    byte[] accountIdBytes = ByteArray.fromString(accountId);
    final String txid = PublicMethed.setAccountIdDelayGetTxid(accountIdBytes,
        delaySecond, doSetIdAddress, doSetIdKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String getAccountId = new String(PublicMethed.queryAccount(doSetIdKey,
        blockingStubFull).getAccountId().toByteArray(), Charset.forName("UTF-8"));
    Assert.assertTrue(getAccountId.isEmpty());

    Long balanceInDelay = PublicMethed.queryAccount(doSetIdKey, blockingStubFull)
        .getBalance();
    Assert.assertTrue(beforeSetAccountIdBalance - balanceInDelay == delayTransactionFee);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    getAccountId = new String(PublicMethed.queryAccount(doSetIdKey, blockingStubFull)
        .getAccountId().toByteArray(), Charset.forName("UTF-8"));
    logger.info(accountId);
    Assert.assertTrue(accountId.equalsIgnoreCase(getAccountId));
    Long afterCreateAccountBalance = PublicMethed.queryAccount(doSetIdKey, blockingStubFull)
        .getBalance();
    Long netFee = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get().getReceipt()
        .getNetFee();
    Long fee = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get().getFee();
    Assert.assertTrue(fee - netFee == delayTransactionFee);
    Assert.assertTrue(beforeSetAccountIdBalance - afterCreateAccountBalance
        == delayTransactionFee);

  }

  @Test(enabled = false, description = "Cancel delay set account id contract")
  public void test2CancelDelayUpdateAccount() {
    //get account
    ecKey = new ECKey(Utils.getRandom());
    doSetIdAddress = ecKey.getAddress();
    doSetIdKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    PublicMethed.printAddress(doSetIdKey);

    Assert.assertTrue(PublicMethed.sendcoin(doSetIdAddress, 10000000L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    final Long beforeSetAccountIdBalance = PublicMethed.queryAccount(doSetIdKey,
        blockingStubFull).getBalance();
    accountId = "accountId_" + Long.toString(System.currentTimeMillis());
    byte[] accountIdBytes = ByteArray.fromString(accountId);
    final String txid = PublicMethed.setAccountIdDelayGetTxid(accountIdBytes,
        delaySecond, doSetIdAddress, doSetIdKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertFalse(PublicMethed.cancelDeferredTransactionById(txid, fromAddress, testKey002,
        blockingStubFull));
    final String cancelTxid = PublicMethed.cancelDeferredTransactionByIdGetTxid(txid,
        doSetIdAddress, doSetIdKey, blockingStubFull);
    Assert.assertFalse(PublicMethed.cancelDeferredTransactionById(txid, doSetIdAddress,
        doSetIdKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    final Long afterUpdateBalance = PublicMethed.queryAccount(doSetIdKey, blockingStubFull)
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
    Assert.assertTrue(beforeSetAccountIdBalance - afterUpdateBalance
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


