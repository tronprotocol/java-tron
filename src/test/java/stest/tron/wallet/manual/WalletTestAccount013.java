package stest.tron.wallet.manual;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class WalletTestAccount013 {
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("mainWitness.key25");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("mainWitness.key2");
  private final byte[] testAddress003 = PublicMethed.getFinalAddress(testKey003);

  private final String testKey004 = Configuration.getByPath("testng.conf")
      .getString("mainWitness.key3");
  private final byte[] testAddress004 = PublicMethed.getFinalAddress(testKey004);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  ArrayList<String> txidList = new ArrayList<String>();

  Optional<TransactionInfo> infoById = null;
  Long beforeTime;
  Long afterTime;
  Long beforeBlockNum;
  Long afterBlockNum;
  Block currentBlock;
  Long currentBlockNum;

  //get account




  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);

  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(testKey002);
    PublicMethed.printAddress(testKey003);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
  }

  @Test(enabled = true)
  public void getAllowDelegateResource() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] account013Address = ecKey1.getAddress();
    String testKeyForAccount013 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    PublicMethed.printAddress(testKeyForAccount013);

    ECKey ecKey2 = new ECKey(Utils.getRandom());
    byte[] receiverDelegateAddress = ecKey2.getAddress();
    String receiverDelegateKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
    PublicMethed.printAddress(receiverDelegateKey);

    Assert.assertTrue(
        PublicMethed.sendcoin(account013Address,100000000L,fromAddress,testKey002,blockingStubFull));
    Assert.assertTrue(PublicMethed
        .sendcoin(receiverDelegateAddress,100000000L,fromAddress,testKey002,blockingStubFull));
    AccountResourceMessage account013Resource = PublicMethed
        .getAccountResource(account013Address, blockingStubFull);
    logger.info("013 energy limit is " + account013Resource.getEnergyLimit());
    logger.info("013 net limit is " + account013Resource.getNetLimit());

    AccountResourceMessage receiverResource = PublicMethed
        .getAccountResource(receiverDelegateAddress, blockingStubFull);
    logger.info("receiver energy limit is " + receiverResource.getEnergyLimit());
    logger.info("receiver net limit is " + receiverResource.getNetLimit());

    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(account013Address,10000000L,3,0,
        ByteString.copyFrom(receiverDelegateAddress),testKeyForAccount013,blockingStubFull));

    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(account013Address,10000000L,3,1,
        ByteString.copyFrom(receiverDelegateAddress),testKeyForAccount013,blockingStubFull));

    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(receiverDelegateAddress,10000000L,3,0,
        ByteString.copyFrom(account013Address),receiverDelegateKey,blockingStubFull));

    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(receiverDelegateAddress,10000000L,3,1,
        ByteString.copyFrom(account013Address),receiverDelegateKey,blockingStubFull));

    account013Resource = PublicMethed.getAccountResource(account013Address, blockingStubFull);
    logger.info("After 013 energy limit is " + account013Resource.getEnergyLimit());
    logger.info("After 013 net limit is " + account013Resource.getNetLimit());

    receiverResource = PublicMethed.getAccountResource(receiverDelegateAddress, blockingStubFull);
    logger.info("After receiver energy limit is " + receiverResource.getEnergyLimit());
    logger.info("After receiver net limit is " + receiverResource.getNetLimit());
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}