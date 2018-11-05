package stest.tron.wallet.newaddinterface2;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;

//import stest.tron.wallet.common.client.AccountComparator;

@Slf4j
public class GetNowBlock2Test {

  private ManagedChannel channelFull = null;
  private ManagedChannel channelSolidity = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

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

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
  }

  @Test
  public void testCurrentBlock2() {
    //Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    GrpcAPI.BlockExtention currentBlock = blockingStubFull
        .getNowBlock2(GrpcAPI.EmptyMessage.newBuilder().build());
    Assert.assertTrue(currentBlock.hasBlockHeader());
    Assert.assertFalse(currentBlock.getBlockHeader().getWitnessSignature().isEmpty());
    Assert.assertTrue(currentBlock.getBlockHeader().getRawData().getTimestamp() > 0);
    Assert.assertFalse(currentBlock.getBlockHeader().getRawData().getWitnessAddress().isEmpty());
    Assert.assertTrue(currentBlock.getBlockHeader().getRawData().getNumber() > 0);
    Assert.assertFalse(currentBlock.getBlockHeader().getRawData().getParentHash().isEmpty());
    Assert.assertTrue(currentBlock.getBlockHeader().getRawData().getWitnessId() >= 0);
    logger.info("test getcurrentblock is " + Long
        .toString(currentBlock.getBlockHeader().getRawData().getNumber()));
    Assert.assertFalse(currentBlock.getBlockid().isEmpty());

    //Improve coverage.
    currentBlock.equals(currentBlock);
    //Block newBlock = blockingStubFull.getNowBlock2(GrpcAPI.EmptyMessage.newBuilder().build());
    GrpcAPI.BlockExtention newBlock = blockingStubFull
        .getNowBlock2(GrpcAPI.EmptyMessage.newBuilder().build());
    newBlock.equals(currentBlock);
    newBlock.hashCode();
    newBlock.getSerializedSize();
    newBlock.getTransactionsCount();
    newBlock.getTransactionsList();
    Assert.assertFalse(newBlock.getBlockid().isEmpty());
  }

  @Test
  public void testCurrentBlockFromSolidity2() {
    GrpcAPI.BlockExtention currentBlock = blockingStubSolidity
        .getNowBlock2(GrpcAPI.EmptyMessage.newBuilder().build());
    Assert.assertTrue(currentBlock.hasBlockHeader());
    Assert.assertFalse(currentBlock.getBlockHeader().getWitnessSignature().isEmpty());
    Assert.assertTrue(currentBlock.getBlockHeader().getRawData().getTimestamp() > 0);
    Assert.assertFalse(currentBlock.getBlockHeader().getRawData().getWitnessAddress().isEmpty());
    Assert.assertTrue(currentBlock.getBlockHeader().getRawData().getNumber() > 0);
    Assert.assertFalse(currentBlock.getBlockHeader().getRawData().getParentHash().isEmpty());
    Assert.assertTrue(currentBlock.getBlockHeader().getRawData().getWitnessId() >= 0);
    logger.info("test getcurrentblock in soliditynode is " + Long
        .toString(currentBlock.getBlockHeader().getRawData().getNumber()));
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


  public Account queryAccount(String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    byte[] address;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
    if (ecKey == null) {
      String pubKey = loadPubKey(); //04 PubKey[128]
      if (StringUtils.isEmpty(pubKey)) {
        logger.warn("Warning: QueryAccount failed, no wallet address !!");
        return null;
      }
      byte[] pubKeyAsc = pubKey.getBytes();
      byte[] pubKeyHex = Hex.decode(pubKeyAsc);
      ecKey = ECKey.fromPublicOnly(pubKeyHex);
    }
    return grpcQueryAccount(ecKey.getAddress(), blockingStubFull);
  }


  public static String loadPubKey() {
    char[] buf = new char[0x100];
    return String.valueOf(buf, 32, 130);
  }

  public byte[] getAddress(ECKey ecKey) {
    return ecKey.getAddress();
  }

  public Account grpcQueryAccount(byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccount(request);
  }

  public Block getBlock(long blockNum, WalletGrpc.WalletBlockingStub blockingStubFull) {
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(blockNum);
    return blockingStubFull.getBlockByNum(builder.build());

  }
}


