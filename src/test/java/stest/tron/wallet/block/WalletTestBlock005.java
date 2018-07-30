package stest.tron.wallet.block;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.Optional;
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
import org.tron.common.crypto.ECKey;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;

//import com.sun.tools.internal.xjc.reader.xmlschema.bindinfo.BIConversion;

//import stest.tron.wallet.common.client.AccountComparator;

@Slf4j
public class WalletTestBlock005 {

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

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

  @Test(enabled = true)
  public void testGetBlockByLatestNum() {
    //
    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Assert.assertFalse(currentBlockNum < 0);
    while (currentBlockNum <= 5) {
      logger.info("Now the block num is " + Long.toString(currentBlockNum) + " Please wait");
      currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
      currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    }

    NumberMessage numberMessage = NumberMessage.newBuilder().setNum(3).build();
    GrpcAPI.BlockList blockList = blockingStubFull.getBlockByLatestNum(numberMessage);
    Optional<GrpcAPI.BlockList> getBlockByLatestNum = Optional.ofNullable(blockList);
    Assert.assertTrue(getBlockByLatestNum.isPresent());
    Assert.assertTrue(getBlockByLatestNum.get().getBlockCount() == 3);
    Assert.assertTrue(getBlockByLatestNum.get().getBlock(0).hasBlockHeader());
    Assert.assertTrue(
        getBlockByLatestNum.get().getBlock(1).getBlockHeader().getRawData().getNumber() > 0);
    Assert.assertFalse(
        getBlockByLatestNum.get().getBlock(2).getBlockHeader().getRawData().getParentHash()
            .isEmpty());
    logger.info("TestGetBlockByLatestNum ok!!!");

  }

  @Test(enabled = true)
  public void testGetBlockByExceptionNum() {
    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Assert.assertFalse(currentBlockNum < 0);
    while (currentBlockNum <= 5) {
      logger.info("Now the block num is " + Long.toString(currentBlockNum) + " Please wait");
      currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
      currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    }
    NumberMessage numberMessage = NumberMessage.newBuilder().setNum(-1).build();
    GrpcAPI.BlockList blockList = blockingStubFull.getBlockByLatestNum(numberMessage);
    Optional<GrpcAPI.BlockList> getBlockByLatestNum = Optional.ofNullable(blockList);
    Assert.assertTrue(getBlockByLatestNum.get().getBlockCount() == 0);

    numberMessage = NumberMessage.newBuilder().setNum(0).build();
    blockList = blockingStubFull.getBlockByLatestNum(numberMessage);
    getBlockByLatestNum = Optional.ofNullable(blockList);
    Assert.assertTrue(getBlockByLatestNum.get().getBlockCount() == 0);

    numberMessage = NumberMessage.newBuilder().setNum(100).build();
    blockList = blockingStubFull.getBlockByLatestNum(numberMessage);
    getBlockByLatestNum = Optional.ofNullable(blockList);
    Assert.assertTrue(getBlockByLatestNum.get().getBlockCount() == 0);


  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
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


