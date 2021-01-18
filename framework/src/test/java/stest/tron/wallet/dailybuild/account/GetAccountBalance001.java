package stest.tron.wallet.dailybuild.account;

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
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol;
import org.tron.protos.contract.BalanceContract.BlockBalanceTrace;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;


@Slf4j

public class GetAccountBalance001 {
  private final String testKey002 = Configuration.getByPath("testng.conf")
          .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] testAddress = ecKey1.getAddress();
  final String testKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private Integer sendAmount = 1234;
  private String fullnode = Configuration.getByPath("testng.conf")
          .getStringList("fullnode.ip.list").get(0);
  Long beforeFromBalance;
  Long beforeToBalance;
  Long afterFromBalance;
  Long afterToBalance;
  private final String blackHoleAdd = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.blackHoleAddress");

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

  @Test(enabled = true, description = "Test get account balance")
  public void test01GetAccountBalance() {
    Protocol.Block currentBlock = blockingStubFull
        .getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());

    beforeFromBalance = PublicMethed.getAccountBalance(currentBlock,fromAddress,blockingStubFull);
    beforeToBalance = PublicMethed.getAccountBalance(currentBlock,testAddress,blockingStubFull);


  }

  @Test(enabled = true, description = "Test get block balance")
  public void test02GetBlockBalance() {
    String txid = PublicMethed.sendcoinGetTransactionId(testAddress, sendAmount, fromAddress,
        testKey002, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<Protocol.TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Long blockNum = infoById.get().getBlockNumber();

    Protocol.Block currentBlock = PublicMethed.getBlock(blockNum,blockingStubFull);

    BlockBalanceTrace blockBalanceTrace
        = PublicMethed.getBlockBalance(currentBlock,blockingStubFull);


    Assert.assertEquals(ByteString.copyFrom(fromAddress),blockBalanceTrace
        .getTransactionBalanceTrace(0).getOperation(0).getAddress());
    Assert.assertEquals(-100000L,blockBalanceTrace.getTransactionBalanceTrace(0)
        .getOperation(0).getAmount());


    Assert.assertEquals(ByteString.copyFrom(fromAddress),blockBalanceTrace
        .getTransactionBalanceTrace(0).getOperation(1).getAddress());
    Assert.assertEquals(-sendAmount,blockBalanceTrace.getTransactionBalanceTrace(0)
        .getOperation(1).getAmount());



    Assert.assertEquals(ByteString.copyFrom(testAddress),blockBalanceTrace
        .getTransactionBalanceTrace(0).getOperation(2).getAddress());
    Assert.assertEquals(-sendAmount,-blockBalanceTrace.getTransactionBalanceTrace(0)
        .getOperation(2).getAmount());


    afterFromBalance = PublicMethed.getAccountBalance(currentBlock,fromAddress,blockingStubFull);
    afterToBalance = PublicMethed.getAccountBalance(currentBlock,testAddress,blockingStubFull);

    Assert.assertTrue(afterToBalance - beforeToBalance == sendAmount);
    Assert.assertTrue(beforeFromBalance - afterFromBalance >= sendAmount + 100000L);
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed.freedResource(testAddress, testKey, fromAddress, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

}
