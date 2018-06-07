package stest.tron.wallet.witness;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.core.Wallet;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.TransactionUtils;

//import stest.tron.wallet.common.client.WitnessComparator;

//import stest.tron.wallet.common.client.WitnessComparator;

@Slf4j
public class WalletTestWitness002 {


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
    WalletClient.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
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
  public void testQueryAllWitness() {
    GrpcAPI.WitnessList witnesslist = blockingStubFull
        .listWitnesses(GrpcAPI.EmptyMessage.newBuilder().build());
    Optional<GrpcAPI.WitnessList> result = Optional.ofNullable(witnesslist);
    if (result.isPresent()) {
      GrpcAPI.WitnessList witnessList = result.get();
      List<Protocol.Witness> list = witnessList.getWitnessesList();
      List<Protocol.Witness> newList = new ArrayList();
      newList.addAll(list);
      newList.sort(new WitnessComparator());
      GrpcAPI.WitnessList.Builder builder = GrpcAPI.WitnessList.newBuilder();
      newList.forEach(witness -> builder.addWitnesses(witness));
      result = Optional.of(builder.build());
    }
    logger.info(Integer.toString(result.get().getWitnessesCount()));
    Assert.assertTrue(result.get().getWitnessesCount() > 0);
    for (int j = 0; j < result.get().getWitnessesCount(); j++) {
      Assert.assertFalse(result.get().getWitnesses(j).getAddress().isEmpty());
      Assert.assertFalse(result.get().getWitnesses(j).getUrl().isEmpty());
      //Assert.assertTrue(result.get().getWitnesses(j).getLatestSlotNum() > 0);
      result.get().getWitnesses(j).getUrlBytes();
      result.get().getWitnesses(j).getLatestBlockNum();
      result.get().getWitnesses(j).getLatestSlotNum();
      result.get().getWitnesses(j).getTotalMissed();
      result.get().getWitnesses(j).getTotalProduced();
    }

    //Improve coverage.
    witnesslist.equals(result.get());
    witnesslist.hashCode();
    witnesslist.getSerializedSize();
    witnesslist.equals(null);
  }

  @Test(enabled = true)
  public void testSolidityQueryAllWitness() {
    GrpcAPI.WitnessList solidityWitnessList = blockingStubSolidity
        .listWitnesses(GrpcAPI.EmptyMessage.newBuilder().build());
    Optional<GrpcAPI.WitnessList> result = Optional.ofNullable(solidityWitnessList);
    if (result.isPresent()) {
      GrpcAPI.WitnessList witnessList = result.get();
      List<Protocol.Witness> list = witnessList.getWitnessesList();
      List<Protocol.Witness> newList = new ArrayList();
      newList.addAll(list);
      newList.sort(new WitnessComparator());
      GrpcAPI.WitnessList.Builder builder = GrpcAPI.WitnessList.newBuilder();
      newList.forEach(witness -> builder.addWitnesses(witness));
      result = Optional.of(builder.build());
    }
    logger.info(Integer.toString(result.get().getWitnessesCount()));
    Assert.assertTrue(result.get().getWitnessesCount() > 0);
    for (int j = 0; j < result.get().getWitnessesCount(); j++) {
      Assert.assertFalse(result.get().getWitnesses(j).getAddress().isEmpty());
      Assert.assertFalse(result.get().getWitnesses(j).getUrl().isEmpty());
    }
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

  class WitnessComparator implements Comparator {

    public int compare(Object o1, Object o2) {
      return Long
          .compare(((Protocol.Witness) o2).getVoteCount(), ((Protocol.Witness) o1).getVoteCount());
    }
  }

  public Account queryAccount(ECKey ecKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    byte[] address;
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

  private Transaction signTransaction(ECKey ecKey, Transaction transaction) {
    if (ecKey == null || ecKey.getPrivKey() == null) {
      logger.warn("Warning: Can't sign,there is no private key !!");
      return null;
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    return TransactionUtils.sign(transaction, ecKey);
  }
}


