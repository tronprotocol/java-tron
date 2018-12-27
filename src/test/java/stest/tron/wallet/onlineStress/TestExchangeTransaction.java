package stest.tron.wallet.onlineStress;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.ExchangeList;
import org.tron.api.GrpcAPI.ProposalList;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Contract.TransferContract;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.ChainParameters;
import org.tron.protos.Protocol.Exchange;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Sha256Hash;


@Slf4j
public class TestExchangeTransaction {
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("mainWitness.key17");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private static final long now = System.currentTimeMillis();

  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

  Optional<ExchangeList> listExchange;
  Optional<Exchange> exchangeIdInfo;
  Integer exchangeId = 0;
  Integer exchangeRate = 10;
  Long firstTokenInitialBalance = 10000L;
  Long secondTokenInitialBalance = firstTokenInitialBalance * exchangeRate;


  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

  }

  @Test(enabled = true,threadPoolSize = 20, invocationCount = 20)
  public void testExchangeTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] exchangeAddress = ecKey1.getAddress();
    String exchangeKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    ECKey ecKey2 = new ECKey(Utils.getRandom());
    final byte[] transactionAddress = ecKey2.getAddress();
    String transactionKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    PublicMethed.printAddress(exchangeKey);
    PublicMethed.printAddress(transactionKey);

    Assert.assertTrue(PublicMethed.sendcoin(exchangeAddress,1500000000000000L,fromAddress,
        testKey002,blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(transactionAddress,1500000000000000L,fromAddress,
        testKey002,blockingStubFull));
    Long totalSupply = 1500000000000000L;
    Random rand = new Random();
    Integer randNum = rand.nextInt(900000000) + 1;
    String name = "exchange_" +  Long.toString(randNum);
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
    Assert.assertTrue(PublicMethed.transferAsset(transactionAddress,name.getBytes(),
        1500000000L,exchangeAddress,exchangeKey,blockingStubFull));
    try {
      Thread.sleep(30000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    //500000000000000L  //5000000L
    Assert.assertTrue(PublicMethed.exchangeCreate(name.getBytes(),500000000000000L,
            "_".getBytes(),500000000000000L,exchangeAddress,exchangeKey, blockingStubFull));
    try {
      Thread.sleep(300000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    listExchange = PublicMethed.getExchangeList(blockingStubFull);
    exchangeId = listExchange.get().getExchangesCount();

    Integer i = 0;
    while (i++ < 10000) {
      PublicMethed.exchangeTransaction(exchangeId, "_".getBytes(), 100000, 99,
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


  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


