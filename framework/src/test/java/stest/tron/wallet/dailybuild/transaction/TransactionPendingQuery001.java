package stest.tron.wallet.dailybuild.transaction;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.TransactionIdList;
import org.tron.api.GrpcAPI.TransactionList;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Sha256Hash;


@Slf4j

public class TransactionPendingQuery001 {
  private final String testKey002 = Configuration.getByPath("testng.conf")
          .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private ManagedChannel channelSolidity = null;
  public ManagedChannel channelPbft = null;
  public WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubPbft = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] receiverAddress = ecKey1.getAddress();
  final String receiverKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  private String fullnode = Configuration.getByPath("testng.conf")
          .getStringList("fullnode.ip.list").get(0);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
  private String soliInPbft = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(2);
  String txid = null;




  /**
   * constructor.
   */

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

    channelPbft = ManagedChannelBuilder.forTarget(soliInPbft)
        .usePlaintext(true)
        .build();
    blockingStubPbft = WalletSolidityGrpc.newBlockingStub(channelPbft);

  }

  @Test(enabled = true, description = "Test get pending size")
  public void test01GetPendingSize() {
    long pendingSizeInFullNode = 0;
    int retryTimes = 100;
    while (pendingSizeInFullNode == 0 && retryTimes-- > 0) {
      PublicMethed.sendcoin(receiverAddress,1L,fromAddress,testKey002,blockingStubFull);
      if (retryTimes % 5 == 0) {
        pendingSizeInFullNode = blockingStubFull
            .getPendingSize(EmptyMessage.newBuilder().build()).getNum();
      }
    }
    Assert.assertNotEquals(pendingSizeInFullNode,0);
  }


  @Test(enabled = true, description = "Test get pending transaction list")
  public void test02GetPendingTransactionList() {
    int retryTimes = 100;
    TransactionIdList transactionList = blockingStubFull
        .getTransactionListFromPending(EmptyMessage.newBuilder().build());
    while (transactionList.getTxIdCount() == 0 && retryTimes-- > 0) {
      PublicMethed.sendcoin(receiverAddress,1L,fromAddress,testKey002,blockingStubFull);
      if (retryTimes % 5 == 0) {
        transactionList = blockingStubFull
            .getTransactionListFromPending(EmptyMessage.newBuilder().build());
      }
    }
    Assert.assertNotEquals(transactionList.getTxIdCount(),0);

    txid = transactionList.getTxId(0);

    logger.info("txid:" + txid);

  }


  @Test(enabled = true, description = "Test transaction from pending")
  public void test03GetTransactionFromPending() {
    Transaction transaction = PublicMethed.getTransactionFromPending(txid,blockingStubFull).get();
    Assert.assertEquals(ByteArray.toHexString(Sha256Hash
        .hash(true, transaction.getRawData().toByteArray())),txid);
  }


  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed.unFreezeBalance(receiverAddress, receiverKey, 1, receiverAddress,
            blockingStubFull);
    PublicMethed.freedResource(receiverAddress, receiverKey, fromAddress, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

}
