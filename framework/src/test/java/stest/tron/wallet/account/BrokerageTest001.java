package stest.tron.wallet.account;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
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

  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private String solidytnode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidyty = null;
  private ManagedChannel channelSolidity = null;

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

    channelSolidity = ManagedChannelBuilder.forTarget(solidytnode)
        .usePlaintext(true)
        .build();
    blockingStubSolidyty = WalletSolidityGrpc.newBlockingStub(channelSolidity);

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
    Assert.assertEquals(20, blockingStubSolidyty.getBrokerageInfo(bytesMessage).getNum());
  }

  @Test
  public void getRewardTest002() {
    BytesMessage bytesMessage = BytesMessage.newBuilder().setValue(ByteString
        .copyFrom(witnessAddress001))
        .build();
    Assert.assertTrue(blockingStubFull.getRewardInfo(bytesMessage) != null);

    // getRewardInfo from solidity node
    Assert.assertTrue(blockingStubSolidyty.getRewardInfo(bytesMessage) != null);
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
