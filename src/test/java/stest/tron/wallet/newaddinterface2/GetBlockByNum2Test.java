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

@Slf4j
public class GetBlockByNum2Test {

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


  @Test(enabled = true)
  public void testGetBlockByNum2() {
    GrpcAPI.BlockExtention currentBlock = blockingStubFull
        .getNowBlock2(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Assert.assertFalse(currentBlockNum < 0);
    if (currentBlockNum == 1) {
      logger.info("Now has very little block, Please test this case by manual");
      Assert.assertTrue(currentBlockNum == 1);
    }

    //The number is large than the currently number, there is no exception when query this number.
    /*    Long outOfCurrentBlockNum = currentBlockNum + 10000L;
    NumberMessage.Builder builder1 = NumberMessage.newBuilder();
    builder1.setNum(outOfCurrentBlockNum);
    Block outOfCurrentBlock = blockingStubFull.getBlockByNum(builder1.build());
    Assert.assertFalse(outOfCurrentBlock.hasBlockHeader());*/

    //Query the first block
    NumberMessage.Builder builder2 = NumberMessage.newBuilder();
    builder2.setNum(1);
    GrpcAPI.BlockExtention firstBlock = blockingStubFull.getBlockByNum2(builder2.build());
    Assert.assertTrue(firstBlock.hasBlockHeader());
    Assert.assertFalse(firstBlock.getBlockHeader().getWitnessSignature().isEmpty());
    Assert.assertTrue(firstBlock.getBlockHeader().getRawData().getTimestamp() > 0);
    Assert.assertFalse(firstBlock.getBlockHeader().getRawData().getWitnessAddress().isEmpty());
    Assert.assertTrue(firstBlock.getBlockHeader().getRawData().getNumber() == 1);
    Assert.assertFalse(firstBlock.getBlockHeader().getRawData().getParentHash().isEmpty());
    Assert.assertTrue(firstBlock.getBlockHeader().getRawData().getWitnessId() >= 0);
    Assert.assertFalse(firstBlock.getBlockid().isEmpty());

    //Query the zero block
    NumberMessage.Builder builder21 = NumberMessage.newBuilder();
    builder2.setNum(0);
    GrpcAPI.BlockExtention zeroBlock = blockingStubFull.getBlockByNum2(builder21.build());
    Assert.assertTrue(zeroBlock.hasBlockHeader());
    Assert.assertTrue(zeroBlock.getBlockHeader().getWitnessSignature().isEmpty());
    Assert.assertFalse(zeroBlock.getBlockHeader().getRawData().getTimestamp() > 0);
    Assert.assertFalse(zeroBlock.getBlockHeader().getRawData().getWitnessAddress().isEmpty());
    Assert.assertFalse(zeroBlock.getBlockHeader().getRawData().getNumber() == 1);
    Assert.assertFalse(zeroBlock.getBlockHeader().getRawData().getParentHash().isEmpty());
    Assert.assertTrue(zeroBlock.getBlockHeader().getRawData().getWitnessId() >= 0);
    Assert.assertFalse(zeroBlock.getBlockid().isEmpty());

    //Query the -1 block
    NumberMessage.Builder builder22 = NumberMessage.newBuilder();
    builder2.setNum(-1);
    GrpcAPI.BlockExtention nagtiveBlock = blockingStubFull.getBlockByNum2(builder22.build());
    Assert.assertTrue(nagtiveBlock.hasBlockHeader());
    Assert.assertTrue(nagtiveBlock.getBlockHeader().getWitnessSignature().isEmpty());
    Assert.assertFalse(nagtiveBlock.getBlockHeader().getRawData().getTimestamp() > 0);
    Assert.assertFalse(nagtiveBlock.getBlockHeader().getRawData().getWitnessAddress().isEmpty());
    Assert.assertFalse(nagtiveBlock.getBlockHeader().getRawData().getNumber() == 1);
    Assert.assertFalse(nagtiveBlock.getBlockHeader().getRawData().getParentHash().isEmpty());
    Assert.assertTrue(nagtiveBlock.getBlockHeader().getRawData().getWitnessId() >= 0);
    Assert.assertFalse(nagtiveBlock.getBlockid().isEmpty());

    //Query the second latest block.
    NumberMessage.Builder builder3 = NumberMessage.newBuilder();
    builder3.setNum(currentBlockNum - 1);
    GrpcAPI.BlockExtention lastSecondBlock = blockingStubFull.getBlockByNum2(builder3.build());
    Assert.assertTrue(lastSecondBlock.hasBlockHeader());
    Assert.assertFalse(lastSecondBlock.getBlockHeader().getWitnessSignature().isEmpty());
    Assert.assertTrue(lastSecondBlock.getBlockHeader().getRawData().getTimestamp() > 0);
    Assert.assertFalse(lastSecondBlock.getBlockHeader().getRawData().getWitnessAddress().isEmpty());
    Assert.assertTrue(
        lastSecondBlock.getBlockHeader().getRawData().getNumber() + 1 == currentBlockNum);
    Assert.assertFalse(lastSecondBlock.getBlockHeader().getRawData().getParentHash().isEmpty());
    Assert.assertTrue(lastSecondBlock.getBlockHeader().getRawData().getWitnessId() >= 0);
    Assert.assertFalse(lastSecondBlock.getBlockid().isEmpty());
  }

  @Test(enabled = true)
  public void testGetBlockByNumFromSolidity2() {
    GrpcAPI.BlockExtention currentBlock = blockingStubSolidity
        .getNowBlock2(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Assert.assertFalse(currentBlockNum < 0);
    if (currentBlockNum == 1) {
      logger.info("Now has very little block, Please test this case by manual");
      Assert.assertTrue(currentBlockNum == 1);
    }

    //Query the first block.
    NumberMessage.Builder builder2 = NumberMessage.newBuilder();
    builder2.setNum(1);
    GrpcAPI.BlockExtention firstBlock = blockingStubSolidity.getBlockByNum2(builder2.build());
    Assert.assertTrue(firstBlock.hasBlockHeader());
    Assert.assertFalse(firstBlock.getBlockHeader().getWitnessSignature().isEmpty());
    Assert.assertTrue(firstBlock.getBlockHeader().getRawData().getTimestamp() > 0);
    Assert.assertFalse(firstBlock.getBlockHeader().getRawData().getWitnessAddress().isEmpty());
    Assert.assertTrue(firstBlock.getBlockHeader().getRawData().getNumber() == 1);
    Assert.assertFalse(firstBlock.getBlockHeader().getRawData().getParentHash().isEmpty());
    Assert.assertTrue(firstBlock.getBlockHeader().getRawData().getWitnessId() >= 0);
    logger.info("firstblock test from solidity succesfully");
    Assert.assertFalse(firstBlock.getBlockid().isEmpty());

    //Query the second latest block.
    NumberMessage.Builder builder3 = NumberMessage.newBuilder();
    builder3.setNum(currentBlockNum - 1);
    GrpcAPI.BlockExtention lastSecondBlock = blockingStubSolidity.getBlockByNum2(builder3.build());
    Assert.assertTrue(lastSecondBlock.hasBlockHeader());
    Assert.assertFalse(lastSecondBlock.getBlockHeader().getWitnessSignature().isEmpty());
    Assert.assertTrue(lastSecondBlock.getBlockHeader().getRawData().getTimestamp() > 0);
    Assert.assertFalse(lastSecondBlock.getBlockHeader().getRawData().getWitnessAddress().isEmpty());
    Assert.assertTrue(
        lastSecondBlock.getBlockHeader().getRawData().getNumber() + 1 == currentBlockNum);
    Assert.assertFalse(lastSecondBlock.getBlockHeader().getRawData().getParentHash().isEmpty());
    Assert.assertTrue(lastSecondBlock.getBlockHeader().getRawData().getWitnessId() >= 0);
    logger.info("Last second test from solidity succesfully");
    Assert.assertFalse(lastSecondBlock.getBlockid().isEmpty());
  }

  @Test(enabled = true)
  public void testGetBlockById2() {
    GrpcAPI.BlockExtention currentBlock = blockingStubFull
        .getNowBlock2(GrpcAPI.EmptyMessage.newBuilder().build());
    ByteString currentHash = currentBlock.getBlockHeader().getRawData().getParentHash();
    GrpcAPI.BytesMessage request = GrpcAPI.BytesMessage.newBuilder().setValue(currentHash).build();
    Block setIdOfBlock = blockingStubFull.getBlockById(request);
    Assert.assertTrue(setIdOfBlock.hasBlockHeader());
    Assert.assertFalse(setIdOfBlock.getBlockHeader().getWitnessSignature().isEmpty());
    Assert.assertTrue(setIdOfBlock.getBlockHeader().getRawData().getTimestamp() > 0);
    Assert.assertFalse(setIdOfBlock.getBlockHeader().getRawData().getWitnessAddress().isEmpty());
    logger.info(Long.toString(setIdOfBlock.getBlockHeader().getRawData().getNumber()));
    logger.info(Long.toString(currentBlock.getBlockHeader().getRawData().getNumber()));
    Assert.assertTrue(
        setIdOfBlock.getBlockHeader().getRawData().getNumber() + 1 == currentBlock.getBlockHeader()
            .getRawData().getNumber());
    Assert.assertFalse(setIdOfBlock.getBlockHeader().getRawData().getParentHash().isEmpty());
    Assert.assertTrue(setIdOfBlock.getBlockHeader().getRawData().getWitnessId() >= 0);
    logger.info("By ID test succesfully");
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

  public GrpcAPI.BlockExtention getBlock2(long blockNum,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(blockNum);
    return blockingStubFull.getBlockByNum2(builder.build());

  }
}


