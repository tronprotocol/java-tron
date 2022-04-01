package stest.tron.wallet.onlinestress;

import static org.tron.core.config.Parameter.ChainSymbol.TRX_SYMBOL_BYTES;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.ExchangeList;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Exchange;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;


@Slf4j
public class TestExchangeTransaction {

  private static final long now = System.currentTimeMillis();
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  Optional<ExchangeList> listExchange;
  Optional<Exchange> exchangeIdInfo;
  Integer exchangeId = 0;
  Integer exchangeRate = 10;
  Long firstTokenInitialBalance = 10000L;
  Long secondTokenInitialBalance = firstTokenInitialBalance * exchangeRate;
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

  

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

  }

  @Test(enabled = true)
  public void testCreateShieldToken() {
    String tokenOwnerKey = "2925e186bb1e88988855f11ebf20ea3a6e19ed92328b0ffb576122e769d45b68";
    byte[] tokenOwnerAddress = PublicMethed.getFinalAddress(tokenOwnerKey);
    PublicMethed.printAddress(tokenOwnerKey);
    Assert.assertTrue(PublicMethed.sendcoin(tokenOwnerAddress, 20480000000L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String name = "shieldToken";
    Long start = System.currentTimeMillis() + 20000;
    Long end = System.currentTimeMillis() + 10000000000L;
    Long totalSupply = 1500000000000001L;
    String description = "This asset issue is use for exchange transaction stress";
    String url = "This asset issue is use for exchange transaction stress";
    Assert.assertTrue(PublicMethed.createAssetIssue(tokenOwnerAddress, name, totalSupply, 1, 1,
        start, end, 1, description, url, 1000L, 1000L,
        1L, 1L, tokenOwnerKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account getAssetIdFromThisAccount =
        PublicMethed.queryAccount(tokenOwnerAddress, blockingStubFull);
    ByteString assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();
    logger.info("AssetId:" + assetAccountId.toString());


  }

  @Test(enabled = true, threadPoolSize = 20, invocationCount = 20)
  public void testExchangeTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] exchangeAddress = ecKey1.getAddress();
    String exchangeKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    ECKey ecKey2 = new ECKey(Utils.getRandom());
    final byte[] transactionAddress = ecKey2.getAddress();
    String transactionKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    PublicMethed.printAddress(exchangeKey);
    PublicMethed.printAddress(transactionKey);

    Assert.assertTrue(PublicMethed.sendcoin(exchangeAddress, 1500000000000000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(transactionAddress, 1500000000000000L, fromAddress,
        testKey002, blockingStubFull));
    Long totalSupply = 1500000000000000L;
    Random rand = new Random();
    Integer randNum = rand.nextInt(900000000) + 1;
    String name = "exchange_" + Long.toString(randNum);
    Long start = System.currentTimeMillis() + 20000;
    Long end = System.currentTimeMillis() + 10000000000L;
    String description = "This asset issue is use for exchange transaction stress";
    String url = "This asset issue is use for exchange transaction stress";
    Assert.assertTrue(PublicMethed.createAssetIssue(exchangeAddress, name, totalSupply, 1, 1,
        start, end, 1, description, url, 100000000L, 10000000000L,
        10L, 10L, exchangeKey, blockingStubFull));
    try {
      Thread.sleep(30000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    Assert.assertTrue(PublicMethed.transferAsset(transactionAddress, name.getBytes(),
        1500000000L, exchangeAddress, exchangeKey, blockingStubFull));
    try {
      Thread.sleep(30000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    //500000000000000L  //5000000L
    Assert.assertTrue(PublicMethed.exchangeCreate(name.getBytes(), 500000000000000L,
        TRX_SYMBOL_BYTES, 500000000000000L, exchangeAddress, exchangeKey, blockingStubFull));
    try {
      Thread.sleep(300000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    listExchange = PublicMethed.getExchangeList(blockingStubFull);
    exchangeId = listExchange.get().getExchangesCount();

    Integer i = 0;
    while (i++ < 10000) {
      PublicMethed.exchangeTransaction(exchangeId, TRX_SYMBOL_BYTES, 100000, 99,
          transactionAddress, transactionKey, blockingStubFull);
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      PublicMethed.exchangeTransaction(exchangeId, name.getBytes(), 100000, 1,
          transactionAddress, transactionKey, blockingStubFull);
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
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
}


