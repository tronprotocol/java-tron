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
public class DelayTransaction011 {

  public static final long ONE_DELAY_SECONDS = 60 * 60 * 24L;
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  ECKey ecKey = new ECKey(Utils.getRandom());
  byte[] noBandwidthAddress = ecKey.getAddress();
  String noBandwidthKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
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

  @Test(enabled = false, description = "When Bandwidth not enough, create delay transaction.")
  public void test1BandwidthInDelayTransaction() {
    //get account
    ecKey = new ECKey(Utils.getRandom());
    noBandwidthAddress = ecKey.getAddress();
    noBandwidthKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    PublicMethed.printAddress(noBandwidthKey);
    ecKey2 = new ECKey(Utils.getRandom());
    delayAccount2Address = ecKey2.getAddress();
    delayAccount2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
    PublicMethed.printAddress(delayAccount2Key);

    Assert.assertTrue(PublicMethed.sendcoin(noBandwidthAddress, 10000000000L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    while (PublicMethed.queryAccount(noBandwidthAddress, blockingStubFull).getFreeNetUsage()
        < 4700L) {
      PublicMethed.sendcoin(delayAccount2Address, 1L, noBandwidthAddress, noBandwidthKey,
          blockingStubFull);
    }
    PublicMethed.sendcoin(delayAccount2Address, 1L, noBandwidthAddress, noBandwidthKey,
        blockingStubFull);
    PublicMethed.sendcoin(delayAccount2Address, 1L, noBandwidthAddress, noBandwidthKey,
        blockingStubFull);
    Assert.assertTrue(PublicMethed.sendcoin(fromAddress, PublicMethed.queryAccount(
        noBandwidthAddress, blockingStubFull).getBalance() - 3000L, noBandwidthAddress,
        noBandwidthKey, blockingStubFull));
    logger.info("balance is: " + PublicMethed.queryAccount(noBandwidthAddress,
        blockingStubFull).getBalance());
    logger.info("Free net usage is " + PublicMethed.queryAccount(noBandwidthAddress,
        blockingStubFull).getFreeNetUsage());

    String updateAccountName = "account_" + Long.toString(System.currentTimeMillis());
    byte[] accountNameBytes = ByteArray.fromString(updateAccountName);
    String txid = PublicMethed.updateAccountDelayGetTxid(noBandwidthAddress, accountNameBytes,
        10L, noBandwidthKey, blockingStubFull);
    logger.info(txid);
    Assert.assertTrue(PublicMethed.getTransactionById(txid, blockingStubFull)
        .get().getRawData().getContractCount() == 0);

    Assert.assertTrue(PublicMethed.sendcoin(noBandwidthAddress, 103332L - 550L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    txid = PublicMethed.updateAccountDelayGetTxid(noBandwidthAddress, accountNameBytes,
        10L, noBandwidthKey, blockingStubFull);

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


