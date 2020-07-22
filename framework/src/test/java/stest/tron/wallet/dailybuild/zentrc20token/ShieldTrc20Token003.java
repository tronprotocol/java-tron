package stest.tron.wallet.dailybuild.zentrc20token;

import com.google.protobuf.ByteString;
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
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.ShieldedAddressInfo;
import stest.tron.wallet.common.client.utils.ZenTrc20Base;

@Slf4j
public class ShieldTrc20Token003 extends ZenTrc20Base {

  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
  Optional<ShieldedAddressInfo> senderShieldAddressInfo;
  Optional<ShieldedAddressInfo> receiverShieldAddressInfo;
  private BigInteger publicFromAmount;
  List<Note> shieldOutList = new ArrayList<>();
  List<ShieldedAddressInfo> inputShieldAddressList = new ArrayList<>();
  GrpcAPI.DecryptNotesTRC20 senderNote;
  GrpcAPI.DecryptNotesTRC20 receiverNote;
  long senderPosition;

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
    String memo = "Create a note for transfer test " + System.currentTimeMillis();
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
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity);
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


  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Shield TRC20 transaction with type transfer")
  public void test01ShieldTrc20TransactionWithTypeTransfer() throws Exception {
    final Long beforeMintShieldContractBalance = getBalanceOfShieldTrc20(shieldAddress,
        zenTrc20TokenOwnerAddress, zenTrc20TokenOwnerKey, blockingStubFull);

    String transferMemo = "Transfer type test " + System.currentTimeMillis();
    String receiverShieldAddress = receiverShieldAddressInfo.get().getAddress();
    shieldOutList.clear();
    shieldOutList = addShieldTrc20OutputList(shieldOutList, receiverShieldAddress,
        "" + publicFromAmount, transferMemo, blockingStubFull);
    inputShieldAddressList.add(senderShieldAddressInfo.get());
    //inputNoteList.add(senderNote);
    //Create transfer parameters
    GrpcAPI.ShieldedTRC20Parameters shieldedTrc20Parameters
        = createShieldedTrc20Parameters(BigInteger.valueOf(0),
        senderNote, inputShieldAddressList, shieldOutList, "", 0L,
        blockingStubFull, blockingStubSolidity);

    String data = encodeTransferParamsToHexString(shieldedTrc20Parameters);
    //String data = shieldedTrc20Parameters.getTriggerContractInput();
    String txid = PublicMethed.triggerContract(shieldAddressByte,
        transfer, data, true, 0, maxFeeLimit, zenTrc20TokenOwnerAddress,
        zenTrc20TokenOwnerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getReceipt().getResultValue() == 1);
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() > 300000);

    //Scan sender note
    receiverNote = scanShieldedTrc20NoteByIvk(receiverShieldAddressInfo.get(),
        blockingStubFull);

    logger.info("" + receiverNote);
    Assert.assertEquals(receiverNote.getNoteTxs(0).getTxid(), infoById.get().getId());
    Assert.assertEquals(receiverNote.getNoteTxs(0).getNote().getMemo(),
        ByteString.copyFromUtf8(transferMemo));
    Assert.assertEquals(receiverNote.getNoteTxs(0).getNote().getValue(),
        publicFromAmount.longValue());
    Assert.assertEquals(receiverNote.getNoteTxs(0).getNote().getPaymentAddress(),
        receiverShieldAddressInfo.get().getAddress());

    logger.info("scanShieldedTrc20NoteByIvk + senderNote:" + senderNote);
    senderNote = scanShieldedTrc20NoteByIvk(senderShieldAddressInfo.get(),
        blockingStubFull);
    Assert.assertEquals(senderNote.getNoteTxs(0).getIsSpent(), true);

    senderNote = scanShieldedTrc20NoteByOvk(senderShieldAddressInfo.get(),
        blockingStubFull);
    logger.info("scanShieldedTrc20NoteByOvk + senderNote:" + senderNote);

    final Long afterMintShieldContractBalance = getBalanceOfShieldTrc20(shieldAddress,
        zenTrc20TokenOwnerAddress, zenTrc20TokenOwnerKey, blockingStubFull);
    Assert.assertEquals(beforeMintShieldContractBalance, afterMintShieldContractBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Shield TRC20 transaction with type transfer without ask")
  public void test02ShieldTrc20TransactionWithTypeTransferWithoutAsk() throws Exception {
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity);
    //Scan receiver note prepare for without type of transfer
    receiverNote = scanShieldedTrc20NoteByIvk(receiverShieldAddressInfo.get(),
        blockingStubFull);
    String transferMemo = "Transfer type without ask test " + System.currentTimeMillis();
    shieldOutList.clear();
    shieldOutList = addShieldTrc20OutputList(shieldOutList, senderShieldAddressInfo.get()
            .getAddress(),
        "" + publicFromAmount, transferMemo, blockingStubFull);
    inputShieldAddressList.clear();
    inputShieldAddressList.add(receiverShieldAddressInfo.get());

    //Create transfer parameters
    GrpcAPI.ShieldedTRC20Parameters shieldedTrc20Parameters
        = createShieldedTrc20ParametersWithoutAsk(BigInteger.valueOf(0),
        receiverNote, inputShieldAddressList, shieldOutList, "", null, 0L,
        blockingStubFull, blockingStubSolidity);

    String data = encodeTransferParamsToHexString(shieldedTrc20Parameters);
    String txid = PublicMethed.triggerContract(shieldAddressByte,
        transfer, data, true, 0, maxFeeLimit, zenTrc20TokenOwnerAddress,
        zenTrc20TokenOwnerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getReceipt().getResultValue() == 1);
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() > 300000);

    senderNote = scanShieldedTrc20NoteByIvk(senderShieldAddressInfo.get(),
        blockingStubFull);

    logger.info("" + senderNote);
    Assert.assertEquals(senderNote.getNoteTxs(1).getTxid(), infoById.get().getId());
    Assert.assertEquals(senderNote.getNoteTxs(1).getNote().getMemo(),
        ByteString.copyFromUtf8(transferMemo));
    Assert.assertEquals(senderNote.getNoteTxs(1).getNote().getValue(),
        publicFromAmount.longValue());
    Assert.assertEquals(senderNote.getNoteTxs(1).getNote().getPaymentAddress(),
        senderShieldAddressInfo.get().getAddress());

    //logger.info("scanShieldedTrc20NoteByIvk + senderNote:" + senderNote);
    receiverNote = scanShieldedTrc20NoteByIvk(receiverShieldAddressInfo.get(),
        blockingStubFull);
    Assert.assertEquals(receiverNote.getNoteTxs(0).getIsSpent(), true);
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


