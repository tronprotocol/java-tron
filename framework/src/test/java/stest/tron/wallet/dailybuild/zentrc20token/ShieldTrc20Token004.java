package stest.tron.wallet.dailybuild.zentrc20token;

import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.Note;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.ShieldedAddressInfo;
import stest.tron.wallet.common.client.utils.ZenTrc20Base;

@Slf4j
public class ShieldTrc20Token004 extends ZenTrc20Base {

  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
  Optional<ShieldedAddressInfo> senderShieldAddressInfo;
  Optional<ShieldedAddressInfo> secondSenderShieldAddressInfo;
  Optional<ShieldedAddressInfo> receiverShieldAddressInfo;
  private BigInteger publicFromAmount;
  List<Note> shieldOutList = new ArrayList<>();
  List<ShieldedAddressInfo> inputShieldAddressList = new ArrayList<>();
  List<GrpcAPI.DecryptNotesTRC20> inputNoteList = new ArrayList<>();
  GrpcAPI.DecryptNotesTRC20 senderNote;
  GrpcAPI.DecryptNotesTRC20 secondSenderNote;
  long senderPosition;

  //get account
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] receiverAddressbyte = ecKey1.getAddress();
  String receiverKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  String receiverAddressString = PublicMethed.getAddressString(receiverKey);


  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() throws Exception {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
    publicFromAmount = getRandomAmount();

    //Generate new shiled account for sender and receiver
    senderShieldAddressInfo = getNewShieldedAddress(blockingStubFull);
    receiverShieldAddressInfo = getNewShieldedAddress(blockingStubFull);
    String memo = "Create a note for burn test " + System.currentTimeMillis();
    String sendShieldAddress = senderShieldAddressInfo.get().getAddress();
    shieldOutList.clear();
    shieldOutList = addShieldTrc20OutputList(shieldOutList, sendShieldAddress,
        "" + publicFromAmount, memo, blockingStubFull);
    //Create mint parameters
    GrpcAPI.ShieldedTRC20Parameters shieldedTrc20Parameters
        = createShieldedTrc20Parameters(publicFromAmount,
        null, null, shieldOutList, "", 0L,
        blockingStubFull, blockingStubSolidity);
    String data = encodeMintParamsToHexString(shieldedTrc20Parameters, publicFromAmount);
    //Do mint transaction type
    String txid = PublicMethed.triggerContract(shieldAddressByte,
        mint, data, true, 0, maxFeeLimit, zenTrc20TokenOwnerAddress,
        zenTrc20TokenOwnerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getReceipt().getResultValue() == 1);

    //Scan sender note
    senderNote = scanShieldedTrc20NoteByIvk(senderShieldAddressInfo.get(),
        blockingStubFull);
    Assert.assertEquals(senderNote.getNoteTxs(0).getIsSpent(), false);
    logger.info("" + senderNote);
    senderPosition = senderNote.getNoteTxs(0).getPosition();
    Assert.assertEquals(senderNote.getNoteTxs(0).getNote().getValue(),
        publicFromAmount.longValue());

    //Generate new shiled account for burn to one public and one shield
    secondSenderShieldAddressInfo = getNewShieldedAddress(blockingStubFull);

    memo = "Create a note for burn to one public and one shield  test "
        + System.currentTimeMillis();
    sendShieldAddress = secondSenderShieldAddressInfo.get().getAddress();
    shieldOutList.clear();
    shieldOutList = addShieldTrc20OutputList(shieldOutList, sendShieldAddress,
        "" + publicFromAmount, memo, blockingStubFull);
    //Create mint parameters
    shieldedTrc20Parameters
        = createShieldedTrc20Parameters(publicFromAmount,
        null, null, shieldOutList, "", 0L,
        blockingStubFull, blockingStubSolidity);
    data = encodeMintParamsToHexString(shieldedTrc20Parameters, publicFromAmount);
    //Do mint transaction type
    txid = PublicMethed.triggerContract(shieldAddressByte,
        mint, data, true, 0, maxFeeLimit, zenTrc20TokenOwnerAddress,
        zenTrc20TokenOwnerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getReceipt().getResultValue() == 1);

    //Scan sender note
    secondSenderNote = scanShieldedTrc20NoteByIvk(secondSenderShieldAddressInfo.get(),
        blockingStubFull);
    Assert.assertEquals(secondSenderNote.getNoteTxs(0).getIsSpent(), false);
    logger.info("" + secondSenderNote);
    senderPosition = secondSenderNote.getNoteTxs(0).getPosition();
    Assert.assertEquals(secondSenderNote.getNoteTxs(0).getNote().getValue(),
        publicFromAmount.longValue());
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Shield TRC20 transaction with type burn")
  public void test01ShieldTrc20TransactionWithTypeBurn() throws Exception {
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity);
    //Query account before mint balance
    final Long beforeBurnAccountBalance = getBalanceOfShieldTrc20(receiverAddressString,
        zenTrc20TokenOwnerAddress, zenTrc20TokenOwnerKey, blockingStubFull);
    //Query contract before mint balance
    final Long beforeBurnShieldAccountBalance = getBalanceOfShieldTrc20(shieldAddress,
        zenTrc20TokenOwnerAddress, zenTrc20TokenOwnerKey, blockingStubFull);

    //String burnMemo = "Burn type test " + System.currentTimeMillis();
    inputShieldAddressList.add(senderShieldAddressInfo.get());
    BigInteger receiveAmount = publicFromAmount;
    //Create transfer parameters
    GrpcAPI.ShieldedTRC20Parameters shieldedTrc20Parameters
        = createShieldedTrc20Parameters(BigInteger.valueOf(0),
        senderNote, inputShieldAddressList, null, receiverAddressString,
        receiveAmount.longValue(), blockingStubFull, blockingStubSolidity);

    String data = shieldedTrc20Parameters.getTriggerContractInput();
    String txid = PublicMethed.triggerContract(shieldAddressByte,
        burn, data, true, 0, maxFeeLimit, zenTrc20TokenOwnerAddress,
        zenTrc20TokenOwnerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getReceipt().getResultValue() == 1);
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() > 180000);

    logger.info("scanShieldedTrc20NoteByIvk + senderNote:" + senderNote);
    senderNote = scanShieldedTrc20NoteByIvk(senderShieldAddressInfo.get(),
        blockingStubFull);
    Assert.assertEquals(senderNote.getNoteTxs(0).getIsSpent(), true);

    final Long afterBurnAccountBalance = getBalanceOfShieldTrc20(receiverAddressString,
        zenTrc20TokenOwnerAddress, zenTrc20TokenOwnerKey, blockingStubFull);
    //Query contract before mint balance
    final Long afterBurnShieldAccountBalance = getBalanceOfShieldTrc20(shieldAddress,
        zenTrc20TokenOwnerAddress, zenTrc20TokenOwnerKey, blockingStubFull);

    logger.info("afterBurnAccountBalance       :" + afterBurnAccountBalance);
    logger.info("beforeBurnAccountBalance      :" + beforeBurnAccountBalance);
    logger.info("beforeBurnShieldAccountBalance:" + beforeBurnShieldAccountBalance);
    logger.info("afterBurnShieldAccountBalance :" + afterBurnShieldAccountBalance);
    Assert.assertEquals(BigInteger.valueOf(afterBurnAccountBalance - beforeBurnAccountBalance),
        receiveAmount);
    Assert.assertEquals(BigInteger.valueOf(beforeBurnShieldAccountBalance
        - afterBurnShieldAccountBalance), receiveAmount);

    senderNote = scanShieldedTrc20NoteByOvk(senderShieldAddressInfo.get(),
        blockingStubFull);
    Assert.assertEquals(ByteArray.toHexString(senderNote.getNoteTxs(0)
        .getTxid().toByteArray()), txid);
    Assert.assertEquals(senderNote.getNoteTxs(0).getToAmount(), publicFromAmount.toString());

    String toAddress = ByteArray.toHexString(senderNote.getNoteTxs(0)
        .getTransparentToAddress().toByteArray());
    String receiverHexString = ByteArray.toHexString(PublicMethed.getFinalAddress(receiverKey));
    Assert.assertEquals(toAddress, receiverHexString);


  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Shield TRC20 transaction to one T and one S with type burn")
  public void test02ShieldTrc20TransactionWithTypeBurnToOnePublicAndOneShield() throws Exception {
    secondSenderNote = scanShieldedTrc20NoteByIvk(secondSenderShieldAddressInfo.get(),
        blockingStubFull);

    //Query account before mint balance
    final Long beforeBurnAccountBalance = getBalanceOfShieldTrc20(receiverAddressString,
        zenTrc20TokenOwnerAddress, zenTrc20TokenOwnerKey, blockingStubFull);
    //Query contract before mint balance
    final Long beforeBurnShieldAccountBalance = getBalanceOfShieldTrc20(shieldAddress,
        zenTrc20TokenOwnerAddress, zenTrc20TokenOwnerKey, blockingStubFull);

    inputShieldAddressList.clear();
    inputShieldAddressList.add(secondSenderShieldAddressInfo.get());
    BigInteger shieldReceiveAmount = BigInteger.valueOf(9L);
    BigInteger receiveAmount = publicFromAmount.subtract(shieldReceiveAmount);

    ShieldedAddressInfo receiverShieldAddressInfo = getNewShieldedAddress(blockingStubFull).get();
    String receiverShieldAddress = receiverShieldAddressInfo.getAddress();
    String memo = "Burn to one shield and one public test " + System.currentTimeMillis();
    shieldOutList.clear();
    shieldOutList = addShieldTrc20OutputList(shieldOutList, receiverShieldAddress,
        "" + shieldReceiveAmount, memo, blockingStubFull);

    //Create transfer parameters
    GrpcAPI.ShieldedTRC20Parameters shieldedTrc20Parameters
        = createShieldedTrc20Parameters(BigInteger.valueOf(0),
        secondSenderNote, inputShieldAddressList, shieldOutList, receiverAddressString,
        receiveAmount.longValue(), blockingStubFull, blockingStubSolidity);

    String data = shieldedTrc20Parameters.getTriggerContractInput();
    String txid = PublicMethed.triggerContract(shieldAddressByte,
        burn, data, true, 0, maxFeeLimit, zenTrc20TokenOwnerAddress,
        zenTrc20TokenOwnerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getReceipt().getResultValue() == 1);
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() > 180000);

    logger.info("scanShieldedTrc20NoteByIvk + senderNote:" + senderNote);
    senderNote = scanShieldedTrc20NoteByIvk(senderShieldAddressInfo.get(),
        blockingStubFull);
    Assert.assertEquals(senderNote.getNoteTxs(0).getIsSpent(), true);

    final Long afterBurnAccountBalance = getBalanceOfShieldTrc20(receiverAddressString,
        zenTrc20TokenOwnerAddress, zenTrc20TokenOwnerKey, blockingStubFull);
    //Query contract before mint balance
    final Long afterBurnShieldAccountBalance = getBalanceOfShieldTrc20(shieldAddress,
        zenTrc20TokenOwnerAddress, zenTrc20TokenOwnerKey, blockingStubFull);

    logger.info("afterBurnAccountBalance       :" + afterBurnAccountBalance);
    logger.info("beforeBurnAccountBalance      :" + beforeBurnAccountBalance);
    logger.info("beforeBurnShieldAccountBalance:" + beforeBurnShieldAccountBalance);
    logger.info("afterBurnShieldAccountBalance :" + afterBurnShieldAccountBalance);
    Assert.assertEquals(BigInteger.valueOf(afterBurnAccountBalance - beforeBurnAccountBalance),
        receiveAmount);
    Assert.assertEquals(BigInteger.valueOf(beforeBurnShieldAccountBalance
        - afterBurnShieldAccountBalance), receiveAmount);

    secondSenderNote = scanShieldedTrc20NoteByOvk(secondSenderShieldAddressInfo.get(),
        blockingStubFull);
    logger.info(secondSenderNote.toString());
    Assert.assertEquals(secondSenderNote.getNoteTxs(0).getNote().getValue(),
        shieldReceiveAmount.longValue());

    Assert.assertEquals(ByteArray.toHexString(secondSenderNote.getNoteTxs(1)
        .getTxid().toByteArray()), txid);
    Assert.assertEquals(secondSenderNote.getNoteTxs(1).getToAmount(), receiveAmount.toString());

    String toAddress = ByteArray.toHexString(secondSenderNote.getNoteTxs(1)
        .getTransparentToAddress().toByteArray());
    String receiverHexString = ByteArray.toHexString(PublicMethed.getFinalAddress(receiverKey));
    Assert.assertEquals(toAddress, receiverHexString);

    GrpcAPI.DecryptNotesTRC20 receiverSenderNote
        = scanShieldedTrc20NoteByIvk(receiverShieldAddressInfo,
        blockingStubFull);
    Assert.assertEquals(receiverSenderNote.getNoteTxs(0)
        .getIsSpent(), false);
    Assert.assertEquals(receiverSenderNote.getNoteTxs(0)
        .getNote().getValue(), shieldReceiveAmount.longValue());
    Assert.assertEquals(ByteArray.toHexString(receiverSenderNote
        .getNoteTxs(0).getTxid().toByteArray()), txid);


  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


