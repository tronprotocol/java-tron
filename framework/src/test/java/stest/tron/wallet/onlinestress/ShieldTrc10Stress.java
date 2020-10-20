package stest.tron.wallet.onlinestress;

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
import org.tron.api.GrpcAPI.DecryptNotes;
import org.tron.api.GrpcAPI.Note;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.core.config.args.Args;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.ShieldAddressInfo;

@Slf4j
public class ShieldTrc10Stress {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  List<Note> shieldOutList = new ArrayList<>();
  DecryptNotes notes;
  Note note;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] zenTokenOwnerAddress = ecKey1.getAddress();
  String zenTokenOwnerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] receiverPublicAddress = ecKey2.getAddress();
  String receiverPublicKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  Optional<ShieldAddressInfo> sendShieldAddressInfo;
  String sendshieldAddress;
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
  private Long costTokenAmount = 20000 * zenTokenFee;
  private Long zenTokenWhenCreateNewAddress = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.zenTokenWhenCreateNewAddress");


  /**
   * constructor.
   */
  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    Args.setFullNodeAllowShieldedTransaction(true);
    PublicMethed.printAddress(foundationZenTokenKey);
    PublicMethed.printAddress(zenTokenOwnerKey);
    ManagedChannel channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    WalletGrpc.WalletBlockingStub blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    sendShieldAddressInfo = PublicMethed.generateShieldAddress();
    sendshieldAddress = sendShieldAddressInfo.get().getAddress();

    String memo = "Use to TestZenToken004 shield address";
    List<Note> shieldOutList = new ArrayList<>();
    shieldOutList.clear();

    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, sendshieldAddress,
        "" + costTokenAmount, memo);
    Assert.assertTrue(PublicMethed.sendShieldCoin(
        foundationZenTokenAddress, costTokenAmount + zenTokenFee,
        null, null,
        shieldOutList,
        null, 0,
        foundationZenTokenKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);


  }

  @Test(enabled = true, threadPoolSize = 100, invocationCount = 100)
  public void test1Shield2TwoShieldTransaction() {
    ManagedChannel channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    WalletGrpc.WalletBlockingStub blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    DecryptNotes notes;
    List<Note> shieldOutList = new ArrayList<>();

    Integer times = 100;
    while (times-- > 0) {
      notes = PublicMethed.listShieldNote(sendShieldAddressInfo, blockingStubFull);
      //logger.info("note size:" + notes.getNoteTxsCount());

      String memo1 = "Shield to  shield address1 transaction" + System.currentTimeMillis();
      shieldOutList.clear();
      Long sendToShiledAddress1Amount =
          notes.getNoteTxs(notes.getNoteTxsCount() - 1).getNote().getValue() - zenTokenFee;
      shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, sendshieldAddress,
          "" + sendToShiledAddress1Amount, memo1);

      try {
        PublicMethed.sendShieldCoin(
            null, 0,
            sendShieldAddressInfo.get(), notes.getNoteTxs(notes.getNoteTxsCount() - 1),
            shieldOutList,
            null, 0,
            zenTokenOwnerKey, blockingStubFull);
      } catch (Exception e) {
        throw e;
      }
    }

  }

  @AfterClass(enabled = false)
  public void shutdown() throws InterruptedException {
    PublicMethed.transferAsset(foundationZenTokenAddress, tokenId,
        PublicMethed.getAssetIssueValue(zenTokenOwnerAddress,
            PublicMethed.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
            blockingStubFull), zenTokenOwnerAddress, zenTokenOwnerKey, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}