package stest.tron.wallet.dailybuild.manual;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.Assert;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.core.Wallet;
import org.tron.protos.Protocol;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.utils.PublicMethed;

public class GetTransactionInfoByBlockNumFromSolidity {

  public ManagedChannel channelFull = null;
  public WalletGrpc.WalletBlockingStub blockingStubFull = null;
  public ManagedChannel channelSolidity = null;
  public WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  public String fullNode =
      Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list").get(0);
  public String solidityNode =
      Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list").get(0);

  @Test(enabled = true, description = "test getTransactionInfoByBlockNumFromSolidity")
  public void test01GetTransactionInfoByBlockNumFromSolidity() {
    channelFull = ManagedChannelBuilder.forTarget(fullNode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelSolidity = ManagedChannelBuilder.forTarget(solidityNode).usePlaintext(true).build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);

    Protocol.Block solidityCurrentBlock =
        blockingStubSolidity.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    long block = solidityCurrentBlock.getBlockHeader().getRawData().getNumber();
    long targetBlock;
    for (targetBlock = block; targetBlock > 0; targetBlock--) {
      GrpcAPI.NumberMessage.Builder builder = GrpcAPI.NumberMessage.newBuilder();
      builder.setNum(targetBlock);
      if (blockingStubSolidity.getTransactionCountByBlockNum(builder.build()).getNum() > 0) {
        break;
      }
    }

    GrpcAPI.TransactionInfoList transactionList =
        PublicMethed.getTransactionInfoByBlockNum(targetBlock, blockingStubFull).get();

    GrpcAPI.TransactionInfoList transactionListFromSolidity =
        PublicMethed.getTransactionInfoByBlockNum(targetBlock, blockingStubFull).get();
    Assert.assertEquals(transactionList, transactionListFromSolidity);
  }
}
