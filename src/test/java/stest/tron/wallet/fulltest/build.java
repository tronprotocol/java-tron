package stest.tron.wallet.fulltest;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.TimePaginatedMessage;
import org.tron.api.GrpcAPI.AccountNetMessage;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class build {

  //testng001、testng002、testng003、testng004
  private final String testKey002 =
      "FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";
  private final String testKey003 =
      "6815B367FDDE637E53E9ADC8E69424E07724333C9A2B973CFA469975E20753FC";

  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress   = PublicMethed.getFinalAddress(testKey003);

  private static final String tooLongDescription =
      "1qazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqa"
          + "zxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvq"
          + "azxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcxswedcv";
  private static final String tooLongUrl =
      "qaswqaswqaswqaswqaswqaswqaswqaswqaswqaswqaswqaswqaswqasw1qazxswedcvqazxswedcv"
          + "qazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedc"
          + "vqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqaz"
          + "xswedcvqazxswedcvqazxswedcwedcv";


  private static final long now = System.currentTimeMillis();
  private static String name = "c_" + Long.toString(now);
  long totalSupply = now;
  private static final long sendAmount = 1025000000L;
  private static final long netCostMeasure = 200L;

  Long freeAssetNetLimit = 30000L;
  Long publicFreeAssetNetLimit = 30000L;
  String description = "f";
  String url = "h";


  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  //get account
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] asset016Address = ecKey1.getAddress();
  String testKeyForAssetIssue016 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] transferAssetAddress = ecKey2.getAddress();
  String transferAssetCreateKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    logger.info(testKeyForAssetIssue016);
    logger.info(transferAssetCreateKey);

    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = false)
  //@Test(enabled = false,threadPoolSize = 10, invocationCount = 10)
  public void createAssetissue() {
    AccountNetMessage.Builder builder = AccountNetMessage.newBuilder();
    builder.setFreeNetLimit(-1).setFreeNetUsed(-1).setNetLimit(-1).setTotalNetLimit(-1).setTotalNetWeight(0).setNetUsed(0);
    builder.getAssetNetLimitCount();
    builder.getFreeNetLimit();
    builder.getFreeNetUsed();
    builder.clearAssetNetLimit();
    builder.clearAssetNetUsed();
    builder.clear();
    builder.build();
    builder.buildPartial();

    TimePaginatedMessage message =TimePaginatedMessage.getDefaultInstance();


  }


  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


