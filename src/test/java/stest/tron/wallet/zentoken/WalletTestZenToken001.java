package stest.tron.wallet.zentoken;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.Note;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.core.config.args.Args;
import org.tron.protos.Contract;
import org.tron.protos.Protocol.Account;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.ShieldAddressInfo;

@Slf4j
public class WalletTestZenToken001 {

  //private final String testKey002 = Configuration.getByPath("testng.conf")
  //    .getString("foundationAccount.key1");
  private final String testKey002 = "7f7f701e94d4f1dd60ee5205e7ea8ee31121427210417b608a6b2e96433549a7";
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  Account shieldOwnerAccount;
  Account shieldReceiverAccount;
  ByteString assetAccountId;

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String foundationZenTokenKey = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenOwnerKey");
  byte[] foundationZenTokenAddress = PublicMethed.getFinalAddress(foundationZenTokenKey);
  private String zenTokenId = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenId");
  private byte[] tokenId = zenTokenId.getBytes();
  private Long zenTokenFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.zenTokenFee");
  private Long costTokenAmount = 5 * zenTokenFee;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] zenTokenOwnerAddress = ecKey1.getAddress();
  String zenTokenOwnerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    String tokenOwnerKey = "2925e186bb1e88988855f11ebf20ea3a6e19ed92328b0ffb576122e769d45b68";
    byte[] tokenOwnerAddress = PublicMethed.getFinalAddress(tokenOwnerKey);
    PublicMethed.printAddress(tokenOwnerKey);
    PublicMethed.sendcoin(tokenOwnerAddress, 20480000000L, fromAddress,
        testKey002, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String name = "shieldToken";
    Long start = System.currentTimeMillis() + 20000;
    Long end = System.currentTimeMillis() + 10000000000L;
    Long totalSupply = 15000000000000001L;
    String description = "This asset issue is use for exchange transaction stress";
    String url = "This asset issue is use for exchange transaction stress";
    PublicMethed.createAssetIssue(tokenOwnerAddress, name, totalSupply, 1, 1,
        start, end, 1, description, url, 1000L, 1000L,
        1L, 1L, tokenOwnerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account getAssetIdFromThisAccount =
        PublicMethed.queryAccount(tokenOwnerAddress, blockingStubFull);
    ByteString assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();
    logger.info("AssetId:" + assetAccountId.toString());
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(foundationZenTokenKey);
    PublicMethed.printAddress(zenTokenOwnerKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    Assert.assertTrue(PublicMethed.transferAsset(zenTokenOwnerAddress, tokenId,
        costTokenAmount, foundationZenTokenAddress, foundationZenTokenKey, blockingStubFull));

  }

  @Test(enabled = true)
  public void test1Public2ShieldTransaction() {

    Args.getInstance().setAllowShieldedTransaction(true);
    logger.info("--------------");
    Optional<ShieldAddressInfo> ShieldAddressInfo = PublicMethed.generateShieldAddress();

    String shieldAddress = ShieldAddressInfo.get().getAddress();
    logger.info("shieldAddress:" + shieldAddress);
    //String shieldAddress = "ztron1msxu0ee6fx6l9g9kau7kawq864sx9r70ag74jpuxya6570j45e7u2rlcj64uhnna0gr9vghfdfp";
    logger.info(shieldAddress);
    List<Note> shieldOutList = new ArrayList<>();

    Long beforeAssetBalance = PublicMethed.getAssetIssueValue(zenTokenOwnerAddress,PublicMethed.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),blockingStubFull);

    List<Long> shieldInputList = new ArrayList<>();
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList,shieldAddress,"10000000","aaa");

    Assert.assertTrue(PublicMethed.sendShieldCoin(zenTokenOwnerAddress,20000000L,shieldInputList,shieldOutList,null,0,zenTokenOwnerKey,blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long afterAssetBalance = PublicMethed.getAssetIssueValue(zenTokenOwnerAddress,PublicMethed.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),blockingStubFull);

    logger.info("beforeAssetBalance:" + beforeAssetBalance);
    logger.info("afterAssetBalance: " + afterAssetBalance);


  }

  /**
   * constructor.
   */

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}