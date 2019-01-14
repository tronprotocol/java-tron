package stest.tron.wallet.exchangeandtoken;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.ExchangeList;
import org.tron.api.GrpcAPI.PaginatedMessage;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
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
public class WalletExchange001 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private ManagedChannel channelSolidity = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

  private static final long now = System.currentTimeMillis();
  private static String name1 = "exchange001_1_" + Long.toString(now);
  private static String name2 = "exchange001_2_" + Long.toString(now);
  private static final long totalSupply = 1000000001L;
  String description = "just-test";
  String url = "https://github.com/tronprotocol/wallet-cli/";

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] exchange001Address = ecKey1.getAddress();
  String exchange001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] secondExchange001Address = ecKey2.getAddress();
  String secondExchange001Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  Long secondTransferAssetToFirstAccountNum = 100000000L;
  Account firstAccount;
  ByteString assetAccountId1;
  ByteString assetAccountId2;

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

  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
  }

  @Test(enabled = true)
  public void test1CreateUsedAsset() {
    ecKey1 = new ECKey(Utils.getRandom());
    exchange001Address = ecKey1.getAddress();
    exchange001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    ecKey2 = new ECKey(Utils.getRandom());
    secondExchange001Address = ecKey2.getAddress();
    secondExchange001Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    PublicMethed.printAddress(exchange001Key);
    PublicMethed.printAddress(secondExchange001Key);

    Assert.assertTrue(PublicMethed.sendcoin(exchange001Address, 10240000000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(secondExchange001Address, 10240000000L, fromAddress,
        testKey002, blockingStubFull));

    Long start = System.currentTimeMillis() + 5000L;
    Long end = System.currentTimeMillis() + 5000000L;
    Assert.assertTrue(PublicMethed.createAssetIssue(exchange001Address, name1, totalSupply, 1,
        1, start, end, 1, description, url, 10000L, 10000L,
        1L, 1L, exchange001Key, blockingStubFull));
    Assert.assertTrue(PublicMethed.createAssetIssue(secondExchange001Address, name2, totalSupply, 1,
        1, start, end, 1, description, url, 10000L, 10000L,
        1L, 1L, secondExchange001Key, blockingStubFull));
  }

  @Test(enabled = true)
  public void test2CreateExchange() {
    listExchange = PublicMethed.getExchangeList(blockingStubFull);
    final Integer beforeCreateExchangeNum = listExchange.get().getExchangesCount();
    exchangeId = listExchange.get().getExchangesCount();


    Account getAssetIdFromThisAccount;
    getAssetIdFromThisAccount = PublicMethed.queryAccount(exchange001Address, blockingStubFull);
    assetAccountId1 = getAssetIdFromThisAccount.getAssetIssuedID();

    getAssetIdFromThisAccount = PublicMethed
        .queryAccount(secondExchange001Address, blockingStubFull);
    assetAccountId2 = getAssetIdFromThisAccount.getAssetIssuedID();

    firstAccount = PublicMethed.queryAccount(exchange001Address, blockingStubFull);
    Long token1BeforeBalance = 0L;
    for (String name : firstAccount.getAssetMap().keySet()) {
      token1BeforeBalance = firstAccount.getAssetMap().get(name);
    }
    Assert.assertTrue(PublicMethed.transferAsset(exchange001Address, assetAccountId2.toByteArray(),
        secondTransferAssetToFirstAccountNum, secondExchange001Address,
        secondExchange001Key, blockingStubFull));
    Long token2BeforeBalance = secondTransferAssetToFirstAccountNum;

    //logger.info("name1 is " + name1);
    //logger.info("name2 is " + name2);
    //logger.info("first balance is " + Long.toString(token1BeforeBalance));
    //logger.info("second balance is " + token2BeforeBalance.toString());
    //CreateExchange
    Assert.assertTrue(
        PublicMethed.exchangeCreate(assetAccountId1.toByteArray(), firstTokenInitialBalance,
            assetAccountId2.toByteArray(), secondTokenInitialBalance, exchange001Address,
            exchange001Key,
            blockingStubFull));
    listExchange = PublicMethed.getExchangeList(blockingStubFull);
    Integer afterCreateExchangeNum = listExchange.get().getExchangesCount();
    Assert.assertTrue(afterCreateExchangeNum - beforeCreateExchangeNum == 1);
    exchangeId = listExchange.get().getExchangesCount();

  }

  @Test(enabled = true)
  public void test3ListExchange() {
    listExchange = PublicMethed.getExchangeList(blockingStubFull);
    for (Integer i = 0; i < listExchange.get().getExchangesCount(); i++) {
      Assert.assertFalse(ByteArray.toHexString(listExchange.get().getExchanges(i)
          .getCreatorAddress().toByteArray()).isEmpty());
      Assert.assertTrue(listExchange.get().getExchanges(i).getExchangeId() > 0);
      Assert.assertFalse(ByteArray.toStr(listExchange.get().getExchanges(i).getFirstTokenId()
          .toByteArray()).isEmpty());
      Assert.assertTrue(listExchange.get().getExchanges(i).getFirstTokenBalance() > 0);
    }
  }

  @Test(enabled = true)
  public void test4InjectExchange() {
    exchangeIdInfo = PublicMethed.getExchange(exchangeId.toString(), blockingStubFull);
    final Long beforeExchangeToken1Balance = exchangeIdInfo.get().getFirstTokenBalance();
    final Long beforeExchangeToken2Balance = exchangeIdInfo.get().getSecondTokenBalance();

    firstAccount = PublicMethed.queryAccount(exchange001Address, blockingStubFull);
    Long beforeToken1Balance = 0L;
    Long beforeToken2Balance = 0L;
    for (String id : firstAccount.getAssetV2Map().keySet()) {
      if (assetAccountId1.toStringUtf8().equalsIgnoreCase(id)) {
        beforeToken1Balance = firstAccount.getAssetV2Map().get(id);
      }
      if (assetAccountId2.toStringUtf8().equalsIgnoreCase(id)) {
        beforeToken2Balance = firstAccount.getAssetV2Map().get(id);
      }
    }
    logger.info("before token 1 balance is " + Long.toString(beforeToken1Balance));
    logger.info("before token 2 balance is " + Long.toString(beforeToken2Balance));
    Integer injectBalance = 100;
    Assert.assertTrue(
        PublicMethed.injectExchange(exchangeId, assetAccountId1.toByteArray(), injectBalance,
            exchange001Address, exchange001Key, blockingStubFull));
    firstAccount = PublicMethed.queryAccount(exchange001Address, blockingStubFull);
    Long afterToken1Balance = 0L;
    Long afterToken2Balance = 0L;
    for (String id : firstAccount.getAssetV2Map().keySet()) {
      if (assetAccountId1.toStringUtf8().equalsIgnoreCase(id)) {
        afterToken1Balance = firstAccount.getAssetV2Map().get(id);
      }
      if (assetAccountId2.toStringUtf8().equalsIgnoreCase(id)) {
        afterToken2Balance = firstAccount.getAssetV2Map().get(id);
      }
    }
    logger.info("before token 1 balance is " + Long.toString(afterToken1Balance));
    logger.info("before token 2 balance is " + Long.toString(afterToken2Balance));

    Assert.assertTrue(beforeToken1Balance - afterToken1Balance == injectBalance);
    Assert.assertTrue(beforeToken2Balance - afterToken2Balance == injectBalance
        * exchangeRate);

    exchangeIdInfo = PublicMethed.getExchange(exchangeId.toString(), blockingStubFull);
    Long afterExchangeToken1Balance = exchangeIdInfo.get().getFirstTokenBalance();
    Long afterExchangeToken2Balance = exchangeIdInfo.get().getSecondTokenBalance();
    Assert.assertTrue(afterExchangeToken1Balance - beforeExchangeToken1Balance
        == injectBalance);
    Assert.assertTrue(afterExchangeToken2Balance - beforeExchangeToken2Balance
        == injectBalance * exchangeRate);
  }

  @Test(enabled = true)
  public void test5WithdrawExchange() {
    exchangeIdInfo = PublicMethed.getExchange(exchangeId.toString(), blockingStubFull);
    final Long beforeExchangeToken1Balance = exchangeIdInfo.get().getFirstTokenBalance();
    final Long beforeExchangeToken2Balance = exchangeIdInfo.get().getSecondTokenBalance();

    firstAccount = PublicMethed.queryAccount(exchange001Address, blockingStubFull);
    Long beforeToken1Balance = 0L;
    Long beforeToken2Balance = 0L;
    for (String id : firstAccount.getAssetV2Map().keySet()) {
      if (assetAccountId1.toStringUtf8().equalsIgnoreCase(id)) {
        beforeToken1Balance = firstAccount.getAssetV2Map().get(id);
      }
      if (assetAccountId2.toStringUtf8().equalsIgnoreCase(id)) {
        beforeToken2Balance = firstAccount.getAssetV2Map().get(id);
      }
    }

    logger.info("before token 1 balance is " + Long.toString(beforeToken1Balance));
    logger.info("before token 2 balance is " + Long.toString(beforeToken2Balance));
    Integer withdrawNum = 200;
    Assert.assertTrue(
        PublicMethed.exchangeWithdraw(exchangeId, assetAccountId1.toByteArray(), withdrawNum,
            exchange001Address, exchange001Key, blockingStubFull));
    firstAccount = PublicMethed.queryAccount(exchange001Address, blockingStubFull);
    Long afterToken1Balance = 0L;
    Long afterToken2Balance = 0L;
    for (String id : firstAccount.getAssetV2Map().keySet()) {
      if (assetAccountId1.toStringUtf8().equalsIgnoreCase(id)) {
        afterToken1Balance = firstAccount.getAssetV2Map().get(id);
      }
      if (assetAccountId2.toStringUtf8().equalsIgnoreCase(id)) {
        afterToken2Balance = firstAccount.getAssetV2Map().get(id);
      }
    }

    logger.info("before token 1 balance is " + Long.toString(afterToken1Balance));
    logger.info("before token 2 balance is " + Long.toString(afterToken2Balance));

    Assert.assertTrue(afterToken1Balance - beforeToken1Balance == withdrawNum);
    Assert.assertTrue(afterToken2Balance - beforeToken2Balance == withdrawNum
        * exchangeRate);
    exchangeIdInfo = PublicMethed.getExchange(exchangeId.toString(), blockingStubFull);
    Long afterExchangeToken1Balance = exchangeIdInfo.get().getFirstTokenBalance();
    Long afterExchangeToken2Balance = exchangeIdInfo.get().getSecondTokenBalance();
    Assert.assertTrue(afterExchangeToken1Balance - beforeExchangeToken1Balance
        == -withdrawNum);
    Assert.assertTrue(afterExchangeToken2Balance - beforeExchangeToken2Balance
        == -withdrawNum * exchangeRate);


  }

  @Test(enabled = true)
  public void test6TransactionExchange() {
    exchangeIdInfo = PublicMethed.getExchange(exchangeId.toString(), blockingStubFull);
    final Long beforeExchangeToken1Balance = exchangeIdInfo.get().getFirstTokenBalance();
    final Long beforeExchangeToken2Balance = exchangeIdInfo.get().getSecondTokenBalance();
    logger.info("beforeExchangeToken1Balance" + beforeExchangeToken1Balance);
    logger.info("beforeExchangeToken2Balance" + beforeExchangeToken2Balance);

    firstAccount = PublicMethed.queryAccount(exchange001Address, blockingStubFull);
    Long beforeToken1Balance = 0L;
    Long beforeToken2Balance = 0L;
    for (String id : firstAccount.getAssetV2Map().keySet()) {
      if (assetAccountId1.toStringUtf8().equalsIgnoreCase(id)) {
        beforeToken1Balance = firstAccount.getAssetV2Map().get(id);
      }
      if (assetAccountId2.toStringUtf8().equalsIgnoreCase(id)) {
        beforeToken2Balance = firstAccount.getAssetV2Map().get(id);
      }
    }

    logger.info("before token 1 balance is " + Long.toString(beforeToken1Balance));
    logger.info("before token 2 balance is " + Long.toString(beforeToken2Balance));
    Integer transactionNum = 50;
    Assert.assertTrue(
        PublicMethed
            .exchangeTransaction(exchangeId, assetAccountId1.toByteArray(), transactionNum, 1,
                exchange001Address, exchange001Key, blockingStubFull));
    firstAccount = PublicMethed.queryAccount(exchange001Address, blockingStubFull);
    Long afterToken1Balance = 0L;
    Long afterToken2Balance = 0L;
    for (String id : firstAccount.getAssetV2Map().keySet()) {
      if (assetAccountId1.toStringUtf8().equalsIgnoreCase(id)) {
        afterToken1Balance = firstAccount.getAssetV2Map().get(id);
      }
      if (assetAccountId2.toStringUtf8().equalsIgnoreCase(id)) {
        afterToken2Balance = firstAccount.getAssetV2Map().get(id);
      }
    }
    logger.info("before token 1 balance is " + Long.toString(afterToken1Balance));
    logger.info("before token 2 balance is " + Long.toString(afterToken2Balance));

    exchangeIdInfo = PublicMethed.getExchange(exchangeId.toString(), blockingStubFull);
    Long afterExchangeToken1Balance = exchangeIdInfo.get().getFirstTokenBalance();
    Long afterExchangeToken2Balance = exchangeIdInfo.get().getSecondTokenBalance();
    logger.info("afterExchangeToken1Balance" + afterExchangeToken1Balance);
    logger.info("afterExchangeToken2Balance" + afterExchangeToken2Balance);
    Assert.assertTrue(afterExchangeToken1Balance - beforeExchangeToken1Balance
        == beforeToken1Balance - afterToken1Balance);
    Assert.assertTrue(afterExchangeToken2Balance - beforeExchangeToken2Balance
        == beforeToken2Balance - afterToken2Balance);
  }

  @Test(enabled = true)
  public void test7GetExchangeListPaginated() {
    PaginatedMessage.Builder pageMessageBuilder = PaginatedMessage.newBuilder();
    pageMessageBuilder.setOffset(0);
    pageMessageBuilder.setLimit(100);
    ExchangeList exchangeList = blockingStubFull
        .getPaginatedExchangeList(pageMessageBuilder.build());
    Assert.assertTrue(exchangeList.getExchangesCount() >= 1);
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull,blockingStubSolidity);

    //Solidity support getExchangeId
    exchangeIdInfo = PublicMethed.getExchange(exchangeId.toString(),blockingStubSolidity);
    logger.info("createtime is" + exchangeIdInfo.get().getCreateTime());
    Assert.assertTrue(exchangeIdInfo.get().getCreateTime() > 0);

    //Solidity support listexchange
    listExchange = PublicMethed.getExchangeList(blockingStubSolidity);
    Assert.assertTrue(listExchange.get().getExchangesCount() > 0);
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


