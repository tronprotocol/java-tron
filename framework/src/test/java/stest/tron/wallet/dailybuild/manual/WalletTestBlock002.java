package stest.tron.wallet.dailybuild.manual;

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
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class WalletTestBlock002 {

  private ManagedChannel channelFull = null;
  private ManagedChannel channelSolidity = null;
  private ManagedChannel channelSoliInFull = null;
  private ManagedChannel channelPbft = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSoliInFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubPbft = null;
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
  private String soliInFullnode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(1);
  private String soliInPbft = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(2);

  public static String loadPubKey() {
    char[] buf = new char[0x100];
    return String.valueOf(buf, 32, 130);
  }

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

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

    channelSoliInFull = ManagedChannelBuilder.forTarget(soliInFullnode)
        .usePlaintext(true)
        .build();
    blockingStubSoliInFull = WalletSolidityGrpc.newBlockingStub(channelSoliInFull);

    channelPbft = ManagedChannelBuilder.forTarget(soliInPbft)
        .usePlaintext(true)
        .build();
    blockingStubPbft = WalletSolidityGrpc.newBlockingStub(channelPbft);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetBlockByNum from fullnode")
  public void test01GetBlockByNum() {
    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Assert.assertFalse(currentBlockNum < 0);
    if (currentBlockNum == 1) {
      logger.info("Now has very little block, Please test this case by manual");
      Assert.assertTrue(currentBlockNum == 1);
    }

    //The number is large than the currently number, there is no exception when query this number.
    Long outOfCurrentBlockNum = currentBlockNum + 10000L;
    NumberMessage.Builder builder1 = NumberMessage.newBuilder();
    builder1.setNum(outOfCurrentBlockNum);
    Block outOfCurrentBlock = blockingStubFull.getBlockByNum(builder1.build());
    Assert.assertFalse(outOfCurrentBlock.hasBlockHeader());

    //Query the first block.
    NumberMessage.Builder builder2 = NumberMessage.newBuilder();
    builder2.setNum(1);
    Block firstBlock = blockingStubFull.getBlockByNum(builder2.build());
    Assert.assertTrue(firstBlock.hasBlockHeader());
    Assert.assertFalse(firstBlock.getBlockHeader().getWitnessSignature().isEmpty());
    Assert.assertTrue(firstBlock.getBlockHeader().getRawData().getTimestamp() > 0);
    Assert.assertFalse(firstBlock.getBlockHeader().getRawData().getWitnessAddress().isEmpty());
    Assert.assertTrue(firstBlock.getBlockHeader().getRawData().getNumber() == 1);
    Assert.assertFalse(firstBlock.getBlockHeader().getRawData().getParentHash().isEmpty());
    Assert.assertTrue(firstBlock.getBlockHeader().getRawData().getWitnessId() >= 0);

    //Query the second latest block.
    NumberMessage.Builder builder3 = NumberMessage.newBuilder();
    builder3.setNum(currentBlockNum - 1);
    Block lastSecondBlock = blockingStubFull.getBlockByNum(builder3.build());
    Assert.assertTrue(lastSecondBlock.hasBlockHeader());
    Assert.assertFalse(lastSecondBlock.getBlockHeader().getWitnessSignature().isEmpty());
    Assert.assertTrue(lastSecondBlock.getBlockHeader().getRawData().getTimestamp() > 0);
    Assert.assertFalse(lastSecondBlock.getBlockHeader().getRawData().getWitnessAddress().isEmpty());
    Assert.assertTrue(
        lastSecondBlock.getBlockHeader().getRawData().getNumber() + 1 == currentBlockNum);
    Assert.assertFalse(lastSecondBlock.getBlockHeader().getRawData().getParentHash().isEmpty());
    Assert.assertTrue(lastSecondBlock.getBlockHeader().getRawData().getWitnessId() >= 0);
  }

  @Test(enabled = true, description = "GetBlockByNum from solidity")
  public void test02GetBlockByNumFromSolidity() {
    Block currentBlock = blockingStubSolidity
        .getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Assert.assertFalse(currentBlockNum < 0);
    if (currentBlockNum == 1) {
      logger.info("Now has very little block, Please test this case by manual");
      Assert.assertTrue(currentBlockNum == 1);
    }

    //The number is large than the currently number, there is no exception when query this number.
    Long outOfCurrentBlockNum = currentBlockNum + 10000L;
    NumberMessage.Builder builder1 = NumberMessage.newBuilder();
    builder1.setNum(outOfCurrentBlockNum);
    Block outOfCurrentBlock = blockingStubSolidity.getBlockByNum(builder1.build());
    Assert.assertFalse(outOfCurrentBlock.hasBlockHeader());

    //Query the first block.
    NumberMessage.Builder builder2 = NumberMessage.newBuilder();
    builder2.setNum(1);
    Block firstBlock = blockingStubSolidity.getBlockByNum(builder2.build());
    Assert.assertTrue(firstBlock.hasBlockHeader());
    Assert.assertFalse(firstBlock.getBlockHeader().getWitnessSignature().isEmpty());
    Assert.assertTrue(firstBlock.getBlockHeader().getRawData().getTimestamp() > 0);
    Assert.assertFalse(firstBlock.getBlockHeader().getRawData().getWitnessAddress().isEmpty());
    Assert.assertTrue(firstBlock.getBlockHeader().getRawData().getNumber() == 1);
    Assert.assertFalse(firstBlock.getBlockHeader().getRawData().getParentHash().isEmpty());
    Assert.assertTrue(firstBlock.getBlockHeader().getRawData().getWitnessId() >= 0);
    logger.info("firstblock test from solidity succesfully");

    //Query the second latest block.
    NumberMessage.Builder builder3 = NumberMessage.newBuilder();
    builder3.setNum(currentBlockNum - 1);
    Block lastSecondBlock = blockingStubSolidity.getBlockByNum(builder3.build());
    Assert.assertTrue(lastSecondBlock.hasBlockHeader());
    Assert.assertFalse(lastSecondBlock.getBlockHeader().getWitnessSignature().isEmpty());
    Assert.assertTrue(lastSecondBlock.getBlockHeader().getRawData().getTimestamp() > 0);
    Assert.assertFalse(lastSecondBlock.getBlockHeader().getRawData().getWitnessAddress().isEmpty());
    Assert.assertTrue(
        lastSecondBlock.getBlockHeader().getRawData().getNumber() + 1 == currentBlockNum);
    Assert.assertFalse(lastSecondBlock.getBlockHeader().getRawData().getParentHash().isEmpty());
    Assert.assertTrue(lastSecondBlock.getBlockHeader().getRawData().getWitnessId() >= 0);
    logger.info("Last second test from solidity succesfully");
  }

  @Test(enabled = true, description = "Get block by id")
  public void test03GetBlockById() {

    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
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


  @Test(enabled = true, description = "Get transaction count by block num")
  public void test04GetTransactionCountByBlockNum() {
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(0);

    Assert.assertTrue(blockingStubFull.getTransactionCountByBlockNum(builder.build())
        .getNum() > 3);
  }

  @Test(enabled = true, description = "Get transaction count by block num from solidity")
  public void test05GetTransactionCountByBlockNumFromSolidity() {
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(0);

    Assert.assertTrue(blockingStubSolidity.getTransactionCountByBlockNum(builder.build())
        .getNum() > 3);
    Assert.assertTrue(blockingStubSoliInFull.getTransactionCountByBlockNum(builder.build())
        .getNum() > 3);
  }

  @Test(enabled = true, description = "Get transaction count by block num from PBFT")
  public void test06GetTransactionCountByBlockNumFromPbft() {
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(0);

    Assert.assertTrue(blockingStubPbft.getTransactionCountByBlockNum(builder.build())
        .getNum() > 3);
  }

  @Test(enabled = true, description = "Get now block from PBFT")
  public void test07GetNowBlockFromPbft() {
    Block nowBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long nowBlockNum = nowBlock.getBlockHeader().getRawData().getNumber();
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubPbft);
    Block pbftNowBlock = blockingStubPbft.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long nowPbftBlockNum = pbftNowBlock.getBlockHeader().getRawData().getNumber();
    logger.info("nowBlockNum:" + nowBlockNum + " , nowPbftBlockNum:" + nowPbftBlockNum);
    Assert.assertTrue(nowPbftBlockNum >= nowBlockNum);

    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubPbft);
    GrpcAPI.BlockExtention pbftNowBlock2 = blockingStubPbft.getNowBlock2(GrpcAPI.EmptyMessage
        .newBuilder().build());
    Long nowPbftBlockNum2 = pbftNowBlock2.getBlockHeader().getRawData().getNumber();
    logger.info("nowBlockNum:" + nowBlockNum + " , nowPbftBlockNum2:" + nowPbftBlockNum2);
    Assert.assertTrue(nowPbftBlockNum2 >= nowBlockNum);
  }


  @Test(enabled = true, description = "Get block by num from PBFT")
  public void test08GetBlockByNumFromPbft() {
    Block currentBlock = blockingStubPbft
        .getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Assert.assertFalse(currentBlockNum < 0);
    if (currentBlockNum == 1) {
      logger.info("Now has very little block, Please test this case by manual");
      Assert.assertTrue(currentBlockNum == 1);
    }

    //The number is large than the currently number, there is no exception when query this number.
    Long outOfCurrentBlockNum = currentBlockNum + 10000L;
    NumberMessage.Builder builder1 = NumberMessage.newBuilder();
    builder1.setNum(outOfCurrentBlockNum);
    Block outOfCurrentBlock = blockingStubPbft.getBlockByNum(builder1.build());
    Assert.assertFalse(outOfCurrentBlock.hasBlockHeader());

    //Query the first block.
    NumberMessage.Builder builder2 = NumberMessage.newBuilder();
    builder2.setNum(1);
    Block firstBlock = blockingStubPbft.getBlockByNum(builder2.build());
    Assert.assertTrue(firstBlock.hasBlockHeader());
    Assert.assertFalse(firstBlock.getBlockHeader().getWitnessSignature().isEmpty());
    Assert.assertTrue(firstBlock.getBlockHeader().getRawData().getTimestamp() > 0);
    Assert.assertFalse(firstBlock.getBlockHeader().getRawData().getWitnessAddress().isEmpty());
    Assert.assertTrue(firstBlock.getBlockHeader().getRawData().getNumber() == 1);
    Assert.assertFalse(firstBlock.getBlockHeader().getRawData().getParentHash().isEmpty());
    Assert.assertTrue(firstBlock.getBlockHeader().getRawData().getWitnessId() >= 0);
    logger.info("firstblock test from solidity succesfully");

    //Query the second latest block.
    NumberMessage.Builder builder3 = NumberMessage.newBuilder();
    builder3.setNum(currentBlockNum - 1);
    Block lastSecondBlock = blockingStubPbft.getBlockByNum(builder3.build());
    Assert.assertTrue(lastSecondBlock.hasBlockHeader());
    Assert.assertFalse(lastSecondBlock.getBlockHeader().getWitnessSignature().isEmpty());
    Assert.assertTrue(lastSecondBlock.getBlockHeader().getRawData().getTimestamp() > 0);
    Assert.assertFalse(lastSecondBlock.getBlockHeader().getRawData().getWitnessAddress().isEmpty());
    Assert.assertTrue(
        lastSecondBlock.getBlockHeader().getRawData().getNumber() + 1 == currentBlockNum);
    Assert.assertFalse(lastSecondBlock.getBlockHeader().getRawData().getParentHash().isEmpty());
    Assert.assertTrue(lastSecondBlock.getBlockHeader().getRawData().getWitnessId() >= 0);
    logger.info("Last second test from solidity succesfully");

    //Query the second latest block getBlockByNum2.
    NumberMessage.Builder builder4 = NumberMessage.newBuilder();
    builder4.setNum(currentBlockNum - 1);
    GrpcAPI.BlockExtention lastSecondBlock1 = blockingStubPbft.getBlockByNum2(builder4.build());
    Assert.assertTrue(lastSecondBlock1.hasBlockHeader());
    Assert.assertFalse(lastSecondBlock1.getBlockHeader().getWitnessSignature().isEmpty());
    Assert.assertTrue(lastSecondBlock1.getBlockHeader().getRawData().getTimestamp() > 0);
    Assert.assertFalse(lastSecondBlock1.getBlockHeader().getRawData().getWitnessAddress()
        .isEmpty());
    Assert.assertTrue(
            lastSecondBlock1.getBlockHeader().getRawData().getNumber() + 1 == currentBlockNum);
    Assert.assertFalse(lastSecondBlock1.getBlockHeader().getRawData().getParentHash().isEmpty());
    Assert.assertTrue(lastSecondBlock1.getBlockHeader().getRawData().getWitnessId() >= 0);
    logger.info("Last second test from getBlockByNum2 succesfully");

  }


  /**
   * constructor.
   */

  @AfterClass
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

  /**
   * constructor.
   */

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

  public byte[] getAddress(ECKey ecKey) {
    return ecKey.getAddress();
  }

  /**
   * constructor.
   */

  public Account grpcQueryAccount(byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccount(request);
  }

  /**
   * constructor.
   */

  public Block getBlock(long blockNum, WalletGrpc.WalletBlockingStub blockingStubFull) {
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(blockNum);
    return blockingStubFull.getBlockByNum(builder.build());

  }
}