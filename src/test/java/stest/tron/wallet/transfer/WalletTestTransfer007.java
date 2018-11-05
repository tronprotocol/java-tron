package stest.tron.wallet.transfer;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class WalletTestTransfer007 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);


  private ManagedChannel channelFull = null;
  private ManagedChannel searchChannelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;

  private WalletGrpc.WalletBlockingStub searchBlockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String searchFullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] sendAccountAddress = ecKey1.getAddress();
  String sendAccountKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ManagedChannel channelSolidity = null;
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);


  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    searchChannelFull = ManagedChannelBuilder.forTarget(searchFullnode)
        .usePlaintext(true)
        .build();
    searchBlockingStubFull = WalletGrpc.newBlockingStub(searchChannelFull);
    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
  }


  @Test
  public void testSendCoin() {
    String transactionId = PublicMethed.sendcoinGetTransactionId(sendAccountAddress, 90000000000L,
        fromAddress, testKey002, blockingStubFull);
    Optional<Transaction> infoById = PublicMethed
        .getTransactionById(transactionId, blockingStubFull);
    Long timestamptis = PublicMethed.printTransactionRow(infoById.get().getRawData());
    Long timestamptispBlockOne = PublicMethed.getBlock(1, blockingStubFull).getBlockHeader()
        .getRawData().getTimestamp();
    Assert.assertTrue(timestamptis >= timestamptispBlockOne);
  }

  @Test
  public void testSendCoin2() {
    String transactionId = PublicMethed.sendcoinGetTransactionId(sendAccountAddress, 90000000000L,
        fromAddress, testKey002, blockingStubFull);
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity);

    Optional<Transaction> infoById = PublicMethed
        .getTransactionById(transactionId, blockingStubSolidity);
    Long timestamptis = PublicMethed.printTransactionRow(infoById.get().getRawData());
    Long timestampBlockOne = PublicMethed.getBlock(1, blockingStubFull).getBlockHeader()
        .getRawData().getTimestamp();
    Assert.assertTrue(timestamptis >= timestampBlockOne);

    infoById = PublicMethed.getTransactionById(transactionId, blockingStubFull);
    timestamptis = PublicMethed.printTransactionRow(infoById.get().getRawData());
    timestampBlockOne = PublicMethed.getBlock(1, blockingStubFull).getBlockHeader()
        .getRawData().getTimestamp();
    Assert.assertTrue(timestamptis >= timestampBlockOne);

    ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(transactionId));
    BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
    TransactionInfo transactionInfo;

    transactionInfo = blockingStubSolidity.getTransactionInfoById(request);
    Assert.assertTrue(transactionInfo.getBlockTimeStamp() >= timestampBlockOne);

    transactionInfo = blockingStubFull.getTransactionInfoById(request);

    Assert.assertTrue(transactionInfo.getBlockTimeStamp() >= timestampBlockOne);


  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (searchChannelFull != null) {
      searchChannelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}
