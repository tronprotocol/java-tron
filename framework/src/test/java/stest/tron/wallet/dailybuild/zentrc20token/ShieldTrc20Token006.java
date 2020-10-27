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
public class ShieldTrc20Token006 extends ZenTrc20Base {

  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
  private String soliInPbft = Configuration.getByPath("testng.conf")
          .getStringList("solidityNode.ip.list").get(2);
  Optional<ShieldedAddressInfo> shieldAddressInfo1;
  Optional<ShieldedAddressInfo> shieldAddressInfo2;
  String shieldAddress1;
  String shieldAddress2;
  private BigInteger publicFromAmount;
  private BigInteger shield1ReceiveAmountFor1to2;
  private BigInteger shield2ReceiveAmountFor1to2;
  private BigInteger shield1ReceiveAmountFor2to2;
  private BigInteger shield2ReceiveAmountFor2to2;
  private BigInteger shield1ReceiveAmountFor2to1;
  List<Note> shieldOutList = new ArrayList<>();
  List<ShieldedAddressInfo> inputShieldAddressList = new ArrayList<>();
  GrpcAPI.DecryptNotesTRC20 shield1Note;
  GrpcAPI.DecryptNotesTRC20 shield2Note;
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

    channelPbft = ManagedChannelBuilder.forTarget(soliInPbft)
            .usePlaintext(true)
            .build();
    blockingStubPbft = WalletSolidityGrpc.newBlockingStub(channelPbft);

    publicFromAmount = getRandomAmount();

    //Generate new shiled account for sender and receiver
    shieldAddressInfo1 = getNewShieldedAddress(blockingStubFull);
    shieldAddressInfo2 = getNewShieldedAddress(blockingStubFull);
    String memo = "Create a note for transfer test " + System.currentTimeMillis();
    shieldAddress1 = shieldAddressInfo1.get().getAddress();
    shieldOutList.clear();
    shieldOutList = addShieldTrc20OutputList(shieldOutList, shieldAddress1,
        "" + publicFromAmount, memo, blockingStubFull);
    //Create mint parameters
    GrpcAPI.ShieldedTRC20Parameters shieldedTrc20Parameters
        = createShieldedTrc20Parameters(publicFromAmount,
        null, null, shieldOutList, "", 0L,
        blockingStubFull, blockingStubSolidity
    );
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
    shield1Note = scanShieldedTrc20NoteByIvk(shieldAddressInfo1.get(),
        blockingStubFull);
    Assert.assertEquals(shield1Note.getNoteTxs(0).getIsSpent(), false);
    logger.info("" + shield1Note);
    senderPosition = shield1Note.getNoteTxs(0).getPosition();
    Assert.assertEquals(shield1Note.getNoteTxs(0).getNote().getValue(),
        publicFromAmount.longValue());


  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Shield TRC20 transfer with type 1 to 2")
  public void test01ShieldTrc20TransferWith1To2() throws Exception {
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity);
    //Prepare parameters
    final String transferMemo1 = "1 to 2 for shieldAddressInfo1 " + System.currentTimeMillis();
    final String transferMemo2 = "1 to 2 for shieldAddressInfo2 " + System.currentTimeMillis();
    shieldAddress1 = shieldAddressInfo1.get().getAddress();
    shieldAddress2 = shieldAddressInfo2.get().getAddress();
    shield1ReceiveAmountFor1to2 = BigInteger.valueOf(30);
    shield2ReceiveAmountFor1to2 = publicFromAmount.subtract(shield1ReceiveAmountFor1to2);
    shieldOutList.clear();
    shieldOutList = addShieldTrc20OutputList(shieldOutList, shieldAddress1,
        "" + shield1ReceiveAmountFor1to2, transferMemo1, blockingStubFull);
    shieldOutList = addShieldTrc20OutputList(shieldOutList, shieldAddress2,
        "" + shield2ReceiveAmountFor1to2, transferMemo2, blockingStubFull);
    inputShieldAddressList.clear();
    inputShieldAddressList.add(shieldAddressInfo1.get());

    //Create transfer parameters
    GrpcAPI.ShieldedTRC20Parameters shieldedTrc20Parameters
        = createShieldedTrc20Parameters(BigInteger.valueOf(0),
        shield1Note, inputShieldAddressList, shieldOutList, "", 0L,
        blockingStubFull, blockingStubSolidity
    );

    //Create transfer transaction
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

    //Scan 1 to 2 ivk note
    shield1Note = scanShieldedTrc20NoteByIvk(shieldAddressInfo1.get(),
        blockingStubFull);
    shield2Note = scanShieldedTrc20NoteByIvk(shieldAddressInfo2.get(),
        blockingStubFull);
    logger.info("" + shield1Note);
    logger.info("" + shield2Note);
    Assert.assertEquals(shield1Note.getNoteTxs(1).getTxid(), infoById.get().getId());
    Assert.assertEquals(shield1Note.getNoteTxs(1).getNote().getMemo(),
        ByteString.copyFromUtf8(transferMemo1));
    Assert.assertEquals(shield1Note.getNoteTxs(1).getNote().getValue(),
        shield1ReceiveAmountFor1to2.longValue());
    Assert.assertEquals(shield1Note.getNoteTxs(1).getNote().getPaymentAddress(),
        shieldAddressInfo1.get().getAddress());

    Assert.assertEquals(shield2Note.getNoteTxs(0).getTxid(), infoById.get().getId());
    Assert.assertEquals(shield2Note.getNoteTxs(0).getNote().getMemo(),
        ByteString.copyFromUtf8(transferMemo2));
    Assert.assertEquals(shield2Note.getNoteTxs(0).getNote().getValue(),
        shield2ReceiveAmountFor1to2.longValue());
    Assert.assertEquals(shield2Note.getNoteTxs(0).getNote().getPaymentAddress(),
        shieldAddressInfo2.get().getAddress());

    Assert.assertEquals(shield1Note.getNoteTxs(0).getIsSpent(), true);

    //Scan 1 to 2 ovk note
    shield1Note = scanShieldedTrc20NoteByOvk(shieldAddressInfo1.get(),
        blockingStubFull);
    logger.info("scanShieldedTrc20NoteByOvk + shield1Note:" + shield1Note);
    Assert.assertEquals(shield1Note.getNoteTxsCount(), 2);

    Assert.assertEquals(shield1Note.getNoteTxs(0).getTxid(), infoById.get().getId());
    Assert.assertEquals(shield1Note.getNoteTxs(0).getNote().getMemo(),
        ByteString.copyFromUtf8(transferMemo1));
    Assert.assertEquals(shield1Note.getNoteTxs(0).getNote().getValue(),
        shield1ReceiveAmountFor1to2.longValue());
    Assert.assertEquals(shield1Note.getNoteTxs(0).getNote().getPaymentAddress(),
        shieldAddressInfo1.get().getAddress());

    Assert.assertEquals(shield1Note.getNoteTxs(1).getTxid(), infoById.get().getId());
    Assert.assertEquals(shield1Note.getNoteTxs(1).getNote().getMemo(),
        ByteString.copyFromUtf8(transferMemo2));
    Assert.assertEquals(shield1Note.getNoteTxs(1).getNote().getValue(),
        shield2ReceiveAmountFor1to2.longValue());
    Assert.assertEquals(shield1Note.getNoteTxs(1).getNote().getPaymentAddress(),
        shieldAddressInfo2.get().getAddress());
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Shield TRC20 transfer with type 2 to 2")
  public void test02ShieldTrc20TransferWith2To2() throws Exception {
    //Create a new note for 2 to 2
    String memo = "Create a new note for transfer test " + System.currentTimeMillis();
    shieldAddress1 = shieldAddressInfo1.get().getAddress();
    shieldOutList.clear();
    shieldOutList = addShieldTrc20OutputList(shieldOutList, shieldAddress1,
        "" + publicFromAmount, memo, blockingStubFull);
    //Create mint parameters
    GrpcAPI.ShieldedTRC20Parameters shieldedTrc20Parameters
        = createShieldedTrc20Parameters(publicFromAmount,
        null, null, shieldOutList, "", 0L,
        blockingStubFull, blockingStubSolidity
    );
    String data = encodeMintParamsToHexString(shieldedTrc20Parameters, publicFromAmount);
    //Do mint transaction type
    String txid = PublicMethed.triggerContract(shieldAddressByte,
        mint, data, true, 0, maxFeeLimit, zenTrc20TokenOwnerAddress,
        zenTrc20TokenOwnerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getReceipt().getResultValue() == 1);
    shield1Note = scanShieldedTrc20NoteByIvk(shieldAddressInfo1.get(),
        blockingStubFull);

    final GrpcAPI.DecryptNotesTRC20 inputNoteFor2to2 = GrpcAPI.DecryptNotesTRC20.newBuilder()
        .addNoteTxs(shield1Note.getNoteTxs(1))
        .addNoteTxs(shield1Note.getNoteTxs(2)).build();

    //Prepare parameters
    final String transferMemo1 = "2 to 2 for shieldAddressInfo1 " + System.currentTimeMillis();
    final String transferMemo2 = "2 to 2 for shieldAddressInfo2 " + System.currentTimeMillis();
    shieldAddress1 = shieldAddressInfo1.get().getAddress();
    shieldAddress2 = shieldAddressInfo2.get().getAddress();
    shield1ReceiveAmountFor2to2 = BigInteger.valueOf(5);
    shield2ReceiveAmountFor2to2 = publicFromAmount.add(shield1ReceiveAmountFor1to2)
        .subtract(shield1ReceiveAmountFor2to2);
    shieldOutList.clear();
    shieldOutList = addShieldTrc20OutputList(shieldOutList, shieldAddress1,
        "" + shield1ReceiveAmountFor2to2, transferMemo1, blockingStubFull);
    shieldOutList = addShieldTrc20OutputList(shieldOutList, shieldAddress2,
        "" + shield2ReceiveAmountFor2to2, transferMemo2, blockingStubFull);
    inputShieldAddressList.clear();
    inputShieldAddressList.add(shieldAddressInfo1.get());
    inputShieldAddressList.add(shieldAddressInfo1.get());

    //Create transfer parameters
    shieldedTrc20Parameters
        = createShieldedTrc20Parameters(BigInteger.valueOf(0),
        inputNoteFor2to2, inputShieldAddressList, shieldOutList, "", 0L,
        blockingStubFull, blockingStubSolidity
    );

    //Create transfer transaction
    data = encodeTransferParamsToHexString(shieldedTrc20Parameters);
    txid = PublicMethed.triggerContract(shieldAddressByte,
        transfer, data, true, 0, maxFeeLimit, zenTrc20TokenOwnerAddress,
        zenTrc20TokenOwnerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getReceipt().getResultValue() == 1);
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() > 300000);

    //Scan 2 to 2 ivk note
    shield1Note = scanShieldedTrc20NoteByIvk(shieldAddressInfo1.get(),
        blockingStubFull);
    shield2Note = scanShieldedTrc20NoteByIvk(shieldAddressInfo2.get(),
        blockingStubFull);
    logger.info("" + shield1Note);
    logger.info("" + shield2Note);
    Assert.assertEquals(shield1Note.getNoteTxs(3).getTxid(), infoById.get().getId());
    Assert.assertEquals(shield1Note.getNoteTxs(3).getNote().getMemo(),
        ByteString.copyFromUtf8(transferMemo1));
    Assert.assertEquals(shield1Note.getNoteTxs(3).getNote().getValue(),
        shield1ReceiveAmountFor2to2.longValue());
    Assert.assertEquals(shield1Note.getNoteTxs(3).getNote().getPaymentAddress(),
        shieldAddressInfo1.get().getAddress());

    Assert.assertEquals(shield2Note.getNoteTxs(1).getTxid(), infoById.get().getId());
    Assert.assertEquals(shield2Note.getNoteTxs(1).getNote().getMemo(),
        ByteString.copyFromUtf8(transferMemo2));
    Assert.assertEquals(shield2Note.getNoteTxs(1).getNote().getValue(),
        shield2ReceiveAmountFor2to2.longValue());
    Assert.assertEquals(shield2Note.getNoteTxs(1).getNote().getPaymentAddress(),
        shieldAddressInfo2.get().getAddress());

    Assert.assertEquals(shield1Note.getNoteTxs(1).getIsSpent(), true);
    Assert.assertEquals(shield1Note.getNoteTxs(2).getIsSpent(), true);

    //Scan 2 to 2 ovk note
    shield1Note = scanShieldedTrc20NoteByOvk(shieldAddressInfo1.get(),
        blockingStubFull);
    logger.info("scanShieldedTrc20NoteByOvk + shield1Note:" + shield1Note);
    Assert.assertEquals(shield1Note.getNoteTxsCount(), 4);

    Assert.assertEquals(shield1Note.getNoteTxs(2).getTxid(), infoById.get().getId());
    Assert.assertEquals(shield1Note.getNoteTxs(2).getNote().getMemo(),
        ByteString.copyFromUtf8(transferMemo1));
    Assert.assertEquals(shield1Note.getNoteTxs(2).getNote().getValue(),
        shield1ReceiveAmountFor2to2.longValue());
    Assert.assertEquals(shield1Note.getNoteTxs(2).getNote().getPaymentAddress(),
        shieldAddressInfo1.get().getAddress());

    Assert.assertEquals(shield1Note.getNoteTxs(3).getTxid(), infoById.get().getId());
    Assert.assertEquals(shield1Note.getNoteTxs(3).getNote().getMemo(),
        ByteString.copyFromUtf8(transferMemo2));
    Assert.assertEquals(shield1Note.getNoteTxs(3).getNote().getValue(),
        shield2ReceiveAmountFor2to2.longValue());
    Assert.assertEquals(shield1Note.getNoteTxs(3).getNote().getPaymentAddress(),
        shieldAddressInfo2.get().getAddress());
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Shield TRC20 transfer with type 2 to 1")
  public void test03ShieldTrc20TransferWith2To1() throws Exception {
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity);
    shield2Note = scanShieldedTrc20NoteByIvk(shieldAddressInfo2.get(),
        blockingStubFull);

    //Prepare parameters
    final String transferMemo1 = "2 to 1 for shieldAddressInfo1 " + System.currentTimeMillis();

    shieldAddress1 = shieldAddressInfo1.get().getAddress();
    shield1ReceiveAmountFor2to1 = BigInteger.valueOf(shield2Note.getNoteTxs(0)
        .getNote().getValue() + shield2Note.getNoteTxs(1).getNote().getValue());
    shieldOutList.clear();
    shieldOutList = addShieldTrc20OutputList(shieldOutList, shieldAddress1,
        "" + shield1ReceiveAmountFor2to1, transferMemo1, blockingStubFull);
    inputShieldAddressList.clear();
    inputShieldAddressList.add(shieldAddressInfo2.get());
    inputShieldAddressList.add(shieldAddressInfo2.get());

    //Create transfer parameters
    GrpcAPI.ShieldedTRC20Parameters shieldedTrc20Parameters
        = createShieldedTrc20Parameters(BigInteger.valueOf(0),
        shield2Note, inputShieldAddressList, shieldOutList, "", 0L,
        blockingStubFull, blockingStubSolidity
    );

    //Create transfer transaction
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

    //Scan 2 to 1 ivk note
    shield1Note = scanShieldedTrc20NoteByIvk(shieldAddressInfo1.get(),
        blockingStubFull);
    shield2Note = scanShieldedTrc20NoteByIvk(shieldAddressInfo2.get(),
        blockingStubFull);
    logger.info("" + shield1Note);
    logger.info("" + shield2Note);
    Assert.assertEquals(shield1Note.getNoteTxs(4).getTxid(), infoById.get().getId());
    Assert.assertEquals(shield1Note.getNoteTxs(4).getNote().getMemo(),
        ByteString.copyFromUtf8(transferMemo1));
    Assert.assertEquals(shield1Note.getNoteTxs(4).getNote().getValue(),
        shield1ReceiveAmountFor2to1.longValue());
    Assert.assertEquals(shield1Note.getNoteTxs(4).getNote().getPaymentAddress(),
        shieldAddressInfo1.get().getAddress());

    Assert.assertEquals(shield2Note.getNoteTxs(0).getIsSpent(), true);
    Assert.assertEquals(shield2Note.getNoteTxs(1).getIsSpent(), true);

    //Scan 2 to 1 ovk note
    shield2Note = scanShieldedTrc20NoteByOvk(shieldAddressInfo2.get(),
        blockingStubFull);
    logger.info("scanShieldedTrc20NoteByOvk + shield1Note:" + shield2Note);
    Assert.assertEquals(shield2Note.getNoteTxsCount(), 1);

    Assert.assertEquals(shield2Note.getNoteTxs(0).getTxid(), infoById.get().getId());
    Assert.assertEquals(shield2Note.getNoteTxs(0).getNote().getValue(),
        shield1ReceiveAmountFor2to1.longValue());
    Assert.assertEquals(shield2Note.getNoteTxs(0).getNote().getPaymentAddress(),
        shieldAddressInfo1.get().getAddress());

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Scan shield trc20 note by ivk and ovk on solidity")
  public void test04ScanShieldTrc20NoteByIvkAndOvkOnSolidity() throws Exception {
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity);
    shield1Note = scanShieldedTrc20NoteByIvk(shieldAddressInfo1.get(),
        blockingStubFull);
    GrpcAPI.DecryptNotesTRC20 shield1NoteOnSolidity
        = scanShieldedTrc20NoteByIvk(shieldAddressInfo1.get(),
        blockingStubFull, blockingStubSolidity);
    Assert.assertEquals(shield1Note, shield1NoteOnSolidity);

    shield2Note = scanShieldedTrc20NoteByIvk(shieldAddressInfo2.get(),
        blockingStubFull);
    GrpcAPI.DecryptNotesTRC20 shield2NoteOnSolidity
        = scanShieldedTrc20NoteByIvk(shieldAddressInfo2.get(),
        blockingStubFull, blockingStubSolidity);
    Assert.assertEquals(shield2Note, shield2NoteOnSolidity);

    shield1Note = scanShieldedTrc20NoteByOvk(shieldAddressInfo1.get(),
        blockingStubFull);
    shield1NoteOnSolidity
        = scanShieldedTrc20NoteByOvk(shieldAddressInfo1.get(),
        blockingStubFull, blockingStubSolidity);
    Assert.assertEquals(shield1Note, shield1NoteOnSolidity);

    shield2Note = scanShieldedTrc20NoteByOvk(shieldAddressInfo2.get(),
        blockingStubFull);
    shield2NoteOnSolidity
        = scanShieldedTrc20NoteByOvk(shieldAddressInfo2.get(),
        blockingStubFull, blockingStubSolidity);
    Assert.assertEquals(shield2Note, shield2NoteOnSolidity);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Scan shield trc20 note by ivk and ovk on pbft")
  public void test04ScanShieldTrc20NoteByIvkAndOvkOnPbft() throws Exception {
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity);
    shield1Note = scanShieldedTrc20NoteByIvk(shieldAddressInfo1.get(),
            blockingStubFull);
    GrpcAPI.DecryptNotesTRC20 shield1NoteOnPbft
            = scanShieldedTrc20NoteByIvk(shieldAddressInfo1.get(),
            blockingStubFull, blockingStubPbft);
    Assert.assertEquals(shield1Note, shield1NoteOnPbft);

    shield2Note = scanShieldedTrc20NoteByIvk(shieldAddressInfo2.get(),
            blockingStubFull);
    GrpcAPI.DecryptNotesTRC20 shield2NoteOnPbft
            = scanShieldedTrc20NoteByIvk(shieldAddressInfo2.get(),
            blockingStubFull, blockingStubPbft);
    Assert.assertEquals(shield2Note, shield2NoteOnPbft);

    shield1Note = scanShieldedTrc20NoteByOvk(shieldAddressInfo1.get(),
            blockingStubFull);
    shield1NoteOnPbft
            = scanShieldedTrc20NoteByOvk(shieldAddressInfo1.get(),
            blockingStubFull, blockingStubPbft);
    Assert.assertEquals(shield1Note, shield1NoteOnPbft);

    shield2Note = scanShieldedTrc20NoteByOvk(shieldAddressInfo2.get(),
            blockingStubFull);
    shield2NoteOnPbft
            = scanShieldedTrc20NoteByOvk(shieldAddressInfo2.get(),
            blockingStubFull, blockingStubPbft);
    Assert.assertEquals(shield2Note, shield2NoteOnPbft);
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Query is shield trc20 note spend on solidity and pbft")
  public void test05IsShieldTrc20NoteSpendOnSolidityAndPbft() throws Exception {
    shield1Note = scanShieldedTrc20NoteByIvk(shieldAddressInfo1.get(),
        blockingStubFull);
    shield2Note = scanShieldedTrc20NoteByIvk(shieldAddressInfo2.get(),
        blockingStubFull);

    Assert.assertEquals(getTrc20SpendResult(shieldAddressInfo1.get(),
        shield1Note.getNoteTxs(0), blockingStubFull), true);

    Assert.assertEquals(getTrc20SpendResult(shieldAddressInfo1.get(),
        shield1Note.getNoteTxs(0), blockingStubFull),
        getTrc20SpendResult(shieldAddressInfo1.get(),
            shield1Note.getNoteTxs(0), blockingStubFull, blockingStubSolidity));

    Assert.assertTrue(getTrc20SpendResult(shieldAddressInfo1.get(),
            shield1Note.getNoteTxs(0), blockingStubFull, blockingStubPbft));

    boolean spend = getTrc20SpendResult(shieldAddressInfo1.get(),shield1Note.getNoteTxs(1),
        blockingStubFull);

    Assert.assertEquals(spend,
        getTrc20SpendResult(shieldAddressInfo1.get(), shield1Note.getNoteTxs(1),
            blockingStubFull, blockingStubSolidity));
    Assert.assertEquals(spend,
        getTrc20SpendResult(shieldAddressInfo1.get(), shield1Note.getNoteTxs(1),
            blockingStubFull, blockingStubPbft));

    spend = getTrc20SpendResult(shieldAddressInfo2.get(),shield2Note.getNoteTxs(0),
        blockingStubFull);
    Assert.assertEquals(spend,
        getTrc20SpendResult(shieldAddressInfo2.get(), shield2Note.getNoteTxs(0),
            blockingStubFull, blockingStubSolidity));
    Assert.assertEquals(spend,
        getTrc20SpendResult(shieldAddressInfo2.get(), shield2Note.getNoteTxs(0),
            blockingStubFull, blockingStubPbft));

    spend = getTrc20SpendResult(shieldAddressInfo2.get(),shield2Note.getNoteTxs(1),
        blockingStubFull);
    Assert.assertEquals(spend,
        getTrc20SpendResult(shieldAddressInfo2.get(), shield2Note.getNoteTxs(1),
            blockingStubFull, blockingStubSolidity));
    Assert.assertEquals(spend,
        getTrc20SpendResult(shieldAddressInfo2.get(), shield2Note.getNoteTxs(1),
            blockingStubFull, blockingStubPbft));

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


