package stest.tron.wallet.newaddinterface2;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AccountNetMessage;
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
public class CreateAccount2Test {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private static final long now = System.currentTimeMillis();
  private static String name = "AssetIssue012_" + Long.toString(now);
  private static final long totalSupply = now;
  private static final long sendAmount = 10000000000L;
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  private static final long FREENETLIMIT = 5000L;
  private static final long BASELINE = 4800L;
  //owner account
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] account007Address = ecKey1.getAddress();
  String account007Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  //Wait to be create account
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] newAccountAddress = ecKey2.getAddress();
  String newAccountKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    logger.info(account007Key);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    Assert.assertTrue(PublicMethed.sendcoin(account007Address, 10000000,
        fromAddress, testKey002, blockingStubFull));
  }

  @Test(enabled = true)
  public void testCreateAccount2() {
    Account accountInfo = PublicMethed.queryAccount(account007Key, blockingStubFull);
    final Long beforeBalance = accountInfo.getBalance();
    AccountNetMessage accountNetInfo = PublicMethed.getAccountNet(account007Address,
        blockingStubFull);
    final Long beforeFreeNet = accountNetInfo.getFreeNetUsed();
    GrpcAPI.Return ret1 = PublicMethed.createAccount2(account007Address, newAccountAddress,
        account007Key, blockingStubFull);
    Assert.assertEquals(ret1.getCode(), GrpcAPI.Return.response_code.SUCCESS);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(), "");
    accountInfo = PublicMethed.queryAccount(account007Key, blockingStubFull);
    Long afterBalance = accountInfo.getBalance();
    accountNetInfo = PublicMethed.getAccountNet(account007Address,
        blockingStubFull);
    Long afterFreeNet = accountNetInfo.getFreeNetUsed();
    logger.info(Long.toString(beforeBalance));
    logger.info(Long.toString(afterBalance));
    //When creator has no bandwidth, he can't use the free net.
    Assert.assertTrue(afterFreeNet == beforeFreeNet);
    //When the creator has no bandwidth, create a new account should spend 0.1TRX.
    Assert.assertTrue(beforeBalance - afterBalance == 100000);
  }

  @Test(enabled = true)
  public void testExceptionCreateAccount2() {
    //Try to create an exist account
    GrpcAPI.Return ret1 = PublicMethed
        .createAccount2(account007Address, account007Address, account007Key,
            blockingStubFull);
    Assert.assertEquals(ret1.getCode(), GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(),
        "contract validate error : Account has existed");
    //Try to create an invalid account
    byte[] wrongAddress = "wrongAddress".getBytes();
    ret1 = PublicMethed.createAccount2(account007Address, wrongAddress, account007Key,
        blockingStubFull);
    Assert.assertEquals(ret1.getCode(), GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(),
        "contract validate error : Invalid account address");
  }

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


