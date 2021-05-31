package stest.tron.wallet.dailybuild.account;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
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
import org.tron.protos.Protocol.Account;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class WalletTestAccount012 {
  private static final long sendAmount = 10000000000L;
  private static final long frozenAmountForTronPower = 3456789L;
  private static final long frozenAmountForNet = 7000000L;
  private final String foundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] foundationAddress = PublicMethed.getFinalAddress(foundationKey);

  private final String witnessKey = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] witnessAddress = PublicMethed.getFinalAddress(witnessKey);

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] frozenAddress = ecKey1.getAddress();
  String frozenKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(frozenKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

  }

  @Test(enabled = true, description = "Freeze balance to get tron power")
  public void test01FreezeBalanceGetTronPower() {


    final Long beforeFrozenTime = System.currentTimeMillis();
    Assert.assertTrue(PublicMethed.sendcoin(frozenAddress, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);


    AccountResourceMessage accountResource = PublicMethed
        .getAccountResource(frozenAddress, blockingStubFull);
    final Long beforeTotalTronPowerWeight = accountResource.getTotalTronPowerWeight();
    final Long beforeTronPowerLimit = accountResource.getTronPowerLimit();


    Assert.assertTrue(PublicMethed.freezeBalanceGetTronPower(frozenAddress,frozenAmountForTronPower,
        0,2,null,frozenKey,blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceGetTronPower(frozenAddress,frozenAmountForNet,
        0,0,null,frozenKey,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Long afterFrozenTime = System.currentTimeMillis();
    Account account = PublicMethed.queryAccount(frozenAddress,blockingStubFull);
    Assert.assertEquals(account.getTronPower().getFrozenBalance(),frozenAmountForTronPower);
    Assert.assertTrue(account.getTronPower().getExpireTime() > beforeFrozenTime
        && account.getTronPower().getExpireTime() < afterFrozenTime);

    accountResource = PublicMethed
        .getAccountResource(frozenAddress, blockingStubFull);
    Long afterTotalTronPowerWeight = accountResource.getTotalTronPowerWeight();
    Long afterTronPowerLimit = accountResource.getTronPowerLimit();
    Long afterTronPowerUsed = accountResource.getTronPowerUsed();
    Assert.assertEquals(afterTotalTronPowerWeight - beforeTotalTronPowerWeight,
        frozenAmountForTronPower / 1000000L);

    Assert.assertEquals(afterTronPowerLimit - beforeTronPowerLimit,
        frozenAmountForTronPower / 1000000L);



    Assert.assertTrue(PublicMethed.freezeBalanceGetTronPower(frozenAddress,
        6000000 - frozenAmountForTronPower,
        0,2,null,frozenKey,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    accountResource = PublicMethed
        .getAccountResource(frozenAddress, blockingStubFull);
    afterTronPowerLimit = accountResource.getTronPowerLimit();

    Assert.assertEquals(afterTronPowerLimit - beforeTronPowerLimit,
        6);


  }


  @Test(enabled = true,description = "Vote witness by tron power")
  public void test02VotePowerOnlyComeFromTronPower() {
    AccountResourceMessage accountResource = PublicMethed
        .getAccountResource(frozenAddress, blockingStubFull);
    final Long beforeTronPowerUsed = accountResource.getTronPowerUsed();


    HashMap<byte[],Long> witnessMap = new HashMap<>();
    witnessMap.put(witnessAddress,frozenAmountForNet / 1000000L);
    Assert.assertFalse(PublicMethed.voteWitness(frozenAddress,frozenKey,witnessMap,
        blockingStubFull));
    witnessMap.put(witnessAddress,frozenAmountForTronPower / 1000000L);
    Assert.assertTrue(PublicMethed.voteWitness(frozenAddress,frozenKey,witnessMap,
        blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    accountResource = PublicMethed
        .getAccountResource(frozenAddress, blockingStubFull);
    Long afterTronPowerUsed = accountResource.getTronPowerUsed();
    Assert.assertEquals(afterTronPowerUsed - beforeTronPowerUsed,
        frozenAmountForTronPower / 1000000L);

    final Long secondBeforeTronPowerUsed = afterTronPowerUsed;
    witnessMap.put(witnessAddress,(frozenAmountForTronPower / 1000000L) - 1);
    Assert.assertTrue(PublicMethed.voteWitness(frozenAddress,frozenKey,witnessMap,
        blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    accountResource = PublicMethed
        .getAccountResource(frozenAddress, blockingStubFull);
    afterTronPowerUsed = accountResource.getTronPowerUsed();
    Assert.assertEquals(secondBeforeTronPowerUsed - afterTronPowerUsed,
        1);


  }

  @Test(enabled = true,description = "Tron power is not allow to others")
  public void test03TronPowerIsNotAllowToOthers() {
    Assert.assertFalse(PublicMethed.freezeBalanceGetTronPower(frozenAddress,
        frozenAmountForTronPower, 0,2,
        ByteString.copyFrom(foundationAddress),frozenKey,blockingStubFull));
  }


  @Test(enabled = true,description = "Unfreeze balance for tron power")
  public void test04UnfreezeBalanceForTronPower() {
    AccountResourceMessage accountResource = PublicMethed
        .getAccountResource(foundationAddress, blockingStubFull);
    final Long beforeTotalTronPowerWeight = accountResource.getTotalTronPowerWeight();


    Assert.assertTrue(PublicMethed.unFreezeBalance(frozenAddress,frozenKey,2,
        null,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    accountResource = PublicMethed
        .getAccountResource(frozenAddress, blockingStubFull);
    Long afterTotalTronPowerWeight = accountResource.getTotalTronPowerWeight();
    Assert.assertEquals(beforeTotalTronPowerWeight - afterTotalTronPowerWeight,
        6);

    Assert.assertEquals(accountResource.getTronPowerLimit(),0L);
    Assert.assertEquals(accountResource.getTronPowerUsed(),0L);

    Account account = PublicMethed.queryAccount(frozenAddress,blockingStubFull);
    Assert.assertEquals(account.getTronPower().getFrozenBalance(),0);


  }
  

  /**
   * constructor.
   */

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    PublicMethed.unFreezeBalance(frozenAddress, frozenKey, 2, null,
        blockingStubFull);
    PublicMethed.unFreezeBalance(frozenAddress, frozenKey, 0, null,
        blockingStubFull);
    PublicMethed.freedResource(frozenAddress, frozenKey, foundationAddress, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


