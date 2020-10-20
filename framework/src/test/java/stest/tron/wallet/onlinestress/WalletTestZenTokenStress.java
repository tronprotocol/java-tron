package stest.tron.wallet.onlinestress;

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
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.DecryptNotes;
import org.tron.api.GrpcAPI.DiversifierMessage;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.ExpandedSpendingKeyMessage;
import org.tron.api.GrpcAPI.IncomingViewingKeyDiversifierMessage;
import org.tron.api.GrpcAPI.IncomingViewingKeyMessage;
import org.tron.api.GrpcAPI.Note;
import org.tron.api.GrpcAPI.PaymentAddressMessage;
import org.tron.api.GrpcAPI.ViewingKeyMessage;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.core.config.args.Args;
import org.tron.core.zen.address.DiversifierT;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.ShieldAddressInfo;


@Slf4j
public class WalletTestZenTokenStress {

  private static ByteString assetAccountId = null;
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  Optional<ShieldAddressInfo> sendShieldAddressInfo;
  Optional<ShieldAddressInfo> receiverShieldAddressInfo;
  String sendShieldAddress;
  String receiverShieldAddress;
  List<Note> shieldOutList = new ArrayList<>();
  DecryptNotes notes;
  String memo;
  Note sendNote;
  Note receiverNote;
  BytesMessage ak;
  BytesMessage nk;
  BytesMessage sk;
  ExpandedSpendingKeyMessage expandedSpendingKeyMessage;
  DiversifierMessage diversifierMessage1;
  DiversifierMessage diversifierMessage2;
  DiversifierMessage diversifierMessage3;
  IncomingViewingKeyMessage ivk;
  ShieldAddressInfo addressInfo1 = new ShieldAddressInfo();
  ShieldAddressInfo addressInfo2 = new ShieldAddressInfo();
  ShieldAddressInfo addressInfo3 = new ShieldAddressInfo();

  Optional<ShieldAddressInfo> receiverAddressInfo1;
  Optional<ShieldAddressInfo> receiverAddressInfo2;
  Optional<ShieldAddressInfo> receiverAddressInfo3;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] zenTokenOwnerAddress = ecKey1.getAddress();
  String zenTokenOwnerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private ManagedChannel channelSolidity = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private ManagedChannel channelSolidity1 = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity1 = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
  private String soliditynode1 = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(1);
  private String foundationZenTokenKey = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenOwnerKey");
  byte[] foundationZenTokenAddress = PublicMethed.getFinalAddress(foundationZenTokenKey);
  private String zenTokenId = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenId");
  private byte[] tokenId = zenTokenId.getBytes();
  private Long zenTokenFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.zenTokenFee");
  private Long costTokenAmount = 1 * zenTokenFee + 1;
  private Long sendTokenAmount = 1 * zenTokenFee;

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
    PublicMethed.printAddress(foundationZenTokenKey);
    PublicMethed.printAddress(zenTokenOwnerKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);

    channelSolidity1 = ManagedChannelBuilder.forTarget(soliditynode1)
        .usePlaintext(true)
        .build();
    blockingStubSolidity1 = WalletSolidityGrpc.newBlockingStub(channelSolidity1);

    Assert.assertTrue(PublicMethed.transferAsset(zenTokenOwnerAddress, tokenId,
        costTokenAmount, foundationZenTokenAddress, foundationZenTokenKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Args.setFullNodeAllowShieldedTransaction(true);


  }

  @Test(enabled = true, threadPoolSize = 3, invocationCount = 3)
  public void test1Shield2ShieldTransaction() throws InterruptedException {
    List<Note> shieldOutList = new ArrayList<>();
    Integer times = 0;
    Optional<ShieldAddressInfo> sendShieldAddressInfo = PublicMethed.generateShieldAddress();
    String sendShieldAddress = sendShieldAddressInfo.get().getAddress();
    String memo = "7";
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, sendShieldAddress,
        "" + zenTokenFee, memo);
    while (times++ < 10000) {
      ECKey ecKey1 = new ECKey(Utils.getRandom());
      byte[] zenTokenOwnerAddress = ecKey1.getAddress();

      PublicMethed.transferAsset(zenTokenOwnerAddress, tokenId,
          zenTokenFee * 2, foundationZenTokenAddress, foundationZenTokenKey, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);

      memo = times + ":shield note number";
      shieldOutList.clear();
      shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, sendShieldAddress,
          "" + zenTokenFee, memo);
      String zenTokenOwnerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
      PublicMethed.sendShieldCoin(zenTokenOwnerAddress, 2 * zenTokenFee, null,
          null, shieldOutList, null, 0, zenTokenOwnerKey, blockingStubFull);
      /*      logger.info("Note number:"
          + PublicMethed.getShieldNotesCount(sendShieldAddressInfo,blockingStubFull));*/
    }


  }

  @Test(enabled = true, threadPoolSize = 30, invocationCount = 30)
  public void test2Shield2ShieldTransaction() throws InterruptedException {
    BytesMessage ak;
    BytesMessage nk;
    BytesMessage sk;
    ExpandedSpendingKeyMessage expandedSpendingKeyMessage;
    DiversifierMessage diversifierMessage1;
    DiversifierMessage diversifierMessage2;
    DiversifierMessage diversifierMessage3;
    IncomingViewingKeyMessage ivk;
    ShieldAddressInfo addressInfo1 = new ShieldAddressInfo();
    ShieldAddressInfo addressInfo2 = new ShieldAddressInfo();
    ShieldAddressInfo addressInfo3 = new ShieldAddressInfo();

    Optional<ShieldAddressInfo> receiverAddressInfo1;
    Optional<ShieldAddressInfo> receiverAddressInfo2;
    Optional<ShieldAddressInfo> receiverAddressInfo3;

    Integer times = 0;
    while (times++ < 10000) {
      sk = blockingStubFull.getSpendingKey(EmptyMessage.newBuilder().build());
      //logger.info("sk: " + ByteArray.toHexString(sk.getValue().toByteArray()));

      diversifierMessage1 = blockingStubFull.getDiversifier(EmptyMessage.newBuilder().build());
      //logger.info("d1: " + ByteArray.toHexString(diversifierMessage1.getD().toByteArray()));
      diversifierMessage2 = blockingStubFull.getDiversifier(EmptyMessage.newBuilder().build());
      //logger.info("d2: " + ByteArray.toHexString(diversifierMessage2.getD().toByteArray()));
      diversifierMessage3 = blockingStubFull.getDiversifier(EmptyMessage.newBuilder().build());
      //logger.info("d3: " + ByteArray.toHexString(diversifierMessage3.getD().toByteArray()));

      expandedSpendingKeyMessage = blockingStubFull.getExpandedSpendingKey(sk);

      BytesMessage.Builder askBuilder = BytesMessage.newBuilder();
      askBuilder.setValue(expandedSpendingKeyMessage.getAsk());
      ak = blockingStubFull.getAkFromAsk(askBuilder.build());
      //logger.info("ak: " + ByteArray.toHexString(ak.getValue().toByteArray()));

      BytesMessage.Builder nskBuilder = BytesMessage.newBuilder();
      nskBuilder.setValue(expandedSpendingKeyMessage.getNsk());
      nk = blockingStubFull.getNkFromNsk(nskBuilder.build());
      //logger.info("nk: " + ByteArray.toHexString(nk.getValue().toByteArray()));

      ViewingKeyMessage.Builder viewBuilder = ViewingKeyMessage.newBuilder();
      viewBuilder.setAk(ak.getValue());
      viewBuilder.setNk(nk.getValue());
      ivk = blockingStubFull.getIncomingViewingKey(viewBuilder.build());
      //logger.info("ivk: " + ByteArray.toHexString(ivk.getIvk().toByteArray()));

      IncomingViewingKeyDiversifierMessage.Builder builder =
          IncomingViewingKeyDiversifierMessage.newBuilder();
      builder.setD(diversifierMessage1);
      builder.setIvk(ivk);
      PaymentAddressMessage addressMessage = blockingStubFull.getZenPaymentAddress(builder.build());
      //System.out.println("address1: " + addressMessage.getPaymentAddress());
      addressInfo1.setSk(sk.getValue().toByteArray());
      addressInfo1.setD(new DiversifierT(diversifierMessage1.getD().toByteArray()));
      addressInfo1.setIvk(ivk.getIvk().toByteArray());
      addressInfo1.setOvk(expandedSpendingKeyMessage.getOvk().toByteArray());
      addressInfo1.setPkD(addressMessage.getPkD().toByteArray());
      receiverAddressInfo1 = Optional.of(addressInfo1);

      builder.clear();
      builder = IncomingViewingKeyDiversifierMessage.newBuilder();
      builder.setD(diversifierMessage2);
      builder.setIvk(ivk);
      addressMessage = blockingStubFull.getZenPaymentAddress(builder.build());
      //System.out.println("address2: " + addressMessage.getPaymentAddress());
      addressInfo2.setSk(sk.getValue().toByteArray());
      addressInfo2.setD(new DiversifierT(diversifierMessage2.getD().toByteArray()));
      addressInfo2.setIvk(ivk.getIvk().toByteArray());
      addressInfo2.setOvk(expandedSpendingKeyMessage.getOvk().toByteArray());
      addressInfo2.setPkD(addressMessage.getPkD().toByteArray());
      receiverAddressInfo2 = Optional.of(addressInfo2);

      builder.clear();
      builder = IncomingViewingKeyDiversifierMessage.newBuilder();
      builder.setD(diversifierMessage3);
      builder.setIvk(ivk);
      addressMessage = blockingStubFull.getZenPaymentAddress(builder.build());
      //System.out.println("address3: " + addressMessage.getPaymentAddress());
      addressInfo3.setSk(sk.getValue().toByteArray());
      addressInfo3.setD(new DiversifierT(diversifierMessage3.getD().toByteArray()));
      addressInfo3.setIvk(ivk.getIvk().toByteArray());
      addressInfo3.setOvk(expandedSpendingKeyMessage.getOvk().toByteArray());
      addressInfo3.setPkD(addressMessage.getPkD().toByteArray());
      receiverAddressInfo3 = Optional.of(addressInfo3);
    }

  }

  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    PublicMethed.transferAsset(foundationZenTokenAddress, tokenId,
        PublicMethed.getAssetIssueValue(zenTokenOwnerAddress,
            PublicMethed.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
            blockingStubFull), zenTokenOwnerAddress, zenTokenOwnerKey, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity1 != null) {
      channelSolidity1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}