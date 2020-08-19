package stest.tron.wallet.account;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol;
import org.tron.protos.contract.StorageContract.UpdateBrokerageContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class BrokerageTest001 {

  private String witnessKey001 = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private byte[] witnessAddress001 = PublicMethed.getFinalAddress(witnessKey001);

  private ManagedChannel channelFull = null;
  private ManagedChannel channelSolidity = null;
  private ManagedChannel channelSoliInFull = null;
  private ManagedChannel channelPbft = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSoliInFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubPbft = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
  private String soliInFullnode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(1);
  private String soliInPbft = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(2);

  private String dev001Key = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private byte[] dev001Address = PublicMethed.getFinalAddress(dev001Key);

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

    channelSoliInFull = ManagedChannelBuilder.forTarget(soliInFullnode)
        .usePlaintext(true)
        .build();
    blockingStubSoliInFull = WalletSolidityGrpc.newBlockingStub(channelSoliInFull);

    channelPbft = ManagedChannelBuilder.forTarget(soliInPbft)
        .usePlaintext(true)
        .build();
    blockingStubPbft = WalletSolidityGrpc.newBlockingStub(channelPbft);

    PublicMethed.printAddress(dev001Key);
  }

  @Test
  public void updateBrokerageTest001() {
    // witness updateBrokerage
    Assert.assertTrue(updateBrokerage(witnessAddress001, 55, blockingStubFull));

    Assert.assertTrue(updateBrokerage(witnessAddress001, 0, blockingStubFull));

    Assert.assertTrue(updateBrokerage(witnessAddress001, 100, blockingStubFull));

    Assert.assertFalse(updateBrokerage(witnessAddress001, -55, blockingStubFull));

    // normal account updateBrokerage fail
    Assert.assertFalse(updateBrokerage(dev001Address, 55, blockingStubFull));
  }

  @Test
  public void getBrokerageTest001() {
    BytesMessage bytesMessage = BytesMessage.newBuilder().setValue(ByteString
        .copyFrom(witnessAddress001))
        .build();

    Assert.assertEquals(20, blockingStubFull.getBrokerageInfo(bytesMessage).getNum());

    // getBrokerageInfo from solidity node
    Assert.assertEquals(20, blockingStubSolidity.getBrokerageInfo(bytesMessage).getNum());
    Assert.assertEquals(20, blockingStubSoliInFull.getBrokerageInfo(bytesMessage).getNum());
    Assert.assertEquals(20, blockingStubPbft.getBrokerageInfo(bytesMessage).getNum());
  }

  @Test
  public void getRewardTest002() {
    BytesMessage bytesMessage = BytesMessage.newBuilder().setValue(ByteString
        .copyFrom(witnessAddress001))
        .build();
    Assert.assertTrue(blockingStubFull.getRewardInfo(bytesMessage) != null);

    // getRewardInfo from solidity node
    Assert.assertTrue(blockingStubSolidity.getRewardInfo(bytesMessage) != null);
    Assert.assertTrue(blockingStubPbft.getRewardInfo(bytesMessage) != null);
    Assert.assertTrue(blockingStubSoliInFull.getRewardInfo(bytesMessage) != null);
  }


  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelPbft != null) {
      channelPbft.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSoliInFull != null) {
      channelSoliInFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


  boolean updateBrokerage(byte[] owner, int brokerage,
      WalletGrpc.WalletBlockingStub blockingStubFull) {

    UpdateBrokerageContract.Builder updateBrokerageContract = UpdateBrokerageContract.newBuilder();
    updateBrokerageContract.setOwnerAddress(ByteString.copyFrom(owner)).setBrokerage(brokerage);
    TransactionExtention transactionExtention = blockingStubFull
        .updateBrokerage(updateBrokerageContract.build());
    Protocol.Transaction transaction = transactionExtention.getTransaction();
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      if (transactionExtention != null) {
        System.out.println("Code = " + transactionExtention.getResult().getCode());
        System.out
            .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      }
      return false;
    }
    logger.info("transaction:" + transaction);
    if (transactionExtention.getResult().getResult()) {
      return true;
    }
    return true;
  }

  public void getBrokerage() {

  }
}