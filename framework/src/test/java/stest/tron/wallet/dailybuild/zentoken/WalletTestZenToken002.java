package stest.tron.wallet.dailybuild.zentoken;

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
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.DecryptNotes;
import org.tron.api.GrpcAPI.Note;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.core.config.args.Args;
import org.tron.protos.Protocol;
import org.tron.protos.contract.ShieldContract.IncrementalMerkleVoucherInfo;
import org.tron.protos.contract.ShieldContract.OutputPoint;
import org.tron.protos.contract.ShieldContract.OutputPointInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.ShieldAddressInfo;


@Slf4j
public class WalletTestZenToken002 {

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
  IncrementalMerkleVoucherInfo firstMerkleVoucherInfo;
  IncrementalMerkleVoucherInfo secondMerkleVoucherInfo;
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
  private Long costTokenAmount = 10 * zenTokenFee;
  private Long sendTokenAmount = 8 * zenTokenFee;
  private String scanNoteException = "start_block_index >= 0 && end_block_index > " +
          "start_block_index && end_block_index - start_block_index <= 1000";

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
    Args.getInstance().setFullNodeAllowShieldedTransaction(true);
    sendShieldAddressInfo = PublicMethed.generateShieldAddress();
    sendShieldAddress = sendShieldAddressInfo.get().getAddress();
    logger.info("sendShieldAddressInfo:" + sendShieldAddressInfo);
    memo = "Shield memo in" + System.currentTimeMillis();
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, sendShieldAddress,
        "" + (sendTokenAmount - zenTokenFee), memo);
    Assert.assertTrue(PublicMethed.sendShieldCoin(zenTokenOwnerAddress, sendTokenAmount, null,
        null, shieldOutList, null, 0, zenTokenOwnerKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    notes = PublicMethed.listShieldNote(sendShieldAddressInfo, blockingStubFull);
    sendNote = notes.getNoteTxs(0).getNote();
  }

  @Test(enabled = true, description = "Get merkle tree voucher info")
  public void test01GetMerkleTreeVoucherInfo() {
    notes = PublicMethed.listShieldNote(sendShieldAddressInfo, blockingStubFull);
    sendNote = notes.getNoteTxs(notes.getNoteTxsCount()-1).getNote();
    logger.info("******  test1:"+sendNote.toString());

    OutputPointInfo.Builder request = OutputPointInfo.newBuilder();

    //ShieldNoteInfo noteInfo = shieldWrapper.getUtxoMapNote().get(shieldInputList.get(i));
    OutputPoint.Builder outPointBuild = OutputPoint.newBuilder();
    outPointBuild.setHash(ByteString.copyFrom(notes.getNoteTxs(notes.getNoteTxsCount()-1).getTxid().toByteArray()));
    outPointBuild.setIndex(notes.getNoteTxs(notes.getNoteTxsCount()-1).getIndex());
    request.addOutPoints(outPointBuild.build());
    firstMerkleVoucherInfo = blockingStubFull
        .getMerkleTreeVoucherInfo(request.build());
    Assert.assertTrue(firstMerkleVoucherInfo.toString().contains("tree"));
    Assert.assertTrue(firstMerkleVoucherInfo.toString().contains("rt"));
    Assert.assertTrue(firstMerkleVoucherInfo.toString().contains("path"));
  }


  @Test(enabled = true, description = "Shield to shield transaction")
  public void test02Shield2ShieldTransaction() {
    receiverShieldAddressInfo = PublicMethed.generateShieldAddress();
    receiverShieldAddress = receiverShieldAddressInfo.get().getAddress();

    shieldOutList.clear();
    memo = "Send shield to receiver shield memo in" + System.currentTimeMillis();
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, receiverShieldAddress,
        "" + (sendNote.getValue() - zenTokenFee), memo);
    Assert.assertTrue(PublicMethed.sendShieldCoin(
        null, 0,
        sendShieldAddressInfo.get(), notes.getNoteTxs(0),
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    notes = PublicMethed.listShieldNote(receiverShieldAddressInfo, blockingStubFull);
    receiverNote = notes.getNoteTxs(0).getNote();
    logger.info("Receiver note:" + receiverNote.toString());
    Assert.assertTrue(receiverNote.getValue() == sendNote.getValue() - zenTokenFee);

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Scan note by ivk and scan not by ivk on FullNode")
  public void test03ScanNoteByIvkAndOvk() {
    //Scan sender note by ovk equals scan receiver note by ivk on FullNode
    Note scanNoteByIvk = PublicMethed
        .getShieldNotesByIvk(receiverShieldAddressInfo, blockingStubFull).getNoteTxs(0).getNote();
    Note scanNoteByOvk = PublicMethed
        .getShieldNotesByOvk(sendShieldAddressInfo, blockingStubFull).getNoteTxs(0).getNote();
    Assert.assertEquals(scanNoteByIvk.getValue(), scanNoteByOvk.getValue());
    Assert.assertEquals(scanNoteByIvk.getMemo(), scanNoteByOvk.getMemo());
    Assert.assertEquals(scanNoteByIvk.getRcm(), scanNoteByOvk.getRcm());
    Assert.assertEquals(scanNoteByIvk.getPaymentAddress(), scanNoteByOvk.getPaymentAddress());

    Protocol.Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentNum = currentBlock.getBlockHeader().getRawData().getNumber();
    //endNum - startNum > 1000
    try{
      PublicMethed.getShieldNotesByIvkWithBlockNum(receiverShieldAddressInfo,currentNum-1004,currentNum,
              blockingStubFull);
    }catch (Exception e){
      logger.info(e.toString());
      Assert.assertTrue(e.toString().contains(scanNoteException));
    }
    try{
      PublicMethed.getShieldNotesByOvkWithBlockNum(sendShieldAddressInfo,currentNum-1004,currentNum,
              blockingStubFull);
    }catch (Exception e){
      logger.info("==== endNum - startNum > 1000  ovk==== :"+e.toString());
      Assert.assertTrue(e.toString().contains(scanNoteException));
    }
    //startNum < 0
    try{
      PublicMethed.getShieldNotesByIvkWithBlockNum(receiverShieldAddressInfo,-1L,10L,
              blockingStubFull);
    }catch (Exception e){
      Assert.assertTrue(e.toString().contains(scanNoteException));
    }
    try{
      PublicMethed.getShieldNotesByOvkWithBlockNum(sendShieldAddressInfo,-1L,10L,
              blockingStubFull);
    }catch (Exception e){
      logger.info("==== startNum < 0  ovk==== :"+e.toString());
      Assert.assertTrue(e.toString().contains(scanNoteException));
    }

    //startNum > endNum
    try{
      PublicMethed.getShieldNotesByIvkWithBlockNum(receiverShieldAddressInfo,currentNum,currentNum-1004,
              blockingStubFull);
    }catch (Exception e){
      Assert.assertTrue(e.toString().contains(scanNoteException));
    }
    try{
      PublicMethed.getShieldNotesByOvkWithBlockNum(sendShieldAddressInfo,currentNum,currentNum-1004,
              blockingStubFull);
    }catch (Exception e){
      logger.info("==== startNum > endNum  ovk==== :"+e.toString());
      Assert.assertTrue(e.toString().contains(scanNoteException));
    }

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Scan note by ivk and scan not by ivk on solidity")
  public void test04ScanNoteByIvkAndOvkOnSolidityServer() {

    //Scan sender note by ovk equals scan receiver note by ivk in Solidity
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity);
    Note scanNoteByIvk = PublicMethed
        .getShieldNotesByIvkOnSolidity(receiverShieldAddressInfo, blockingStubSolidity)
        .getNoteTxs(0).getNote();
    Note scanNoteByOvk = PublicMethed
        .getShieldNotesByOvkOnSolidity(sendShieldAddressInfo, blockingStubSolidity)
        .getNoteTxs(0).getNote();
    Assert.assertEquals(scanNoteByIvk.getValue(), scanNoteByOvk.getValue());
    Assert.assertEquals(scanNoteByIvk.getMemo(), scanNoteByOvk.getMemo());
    Assert.assertEquals(scanNoteByIvk.getRcm(), scanNoteByOvk.getRcm());
    Assert.assertEquals(scanNoteByIvk.getPaymentAddress(), scanNoteByOvk.getPaymentAddress());

    Protocol.Block currentBlock = blockingStubSolidity.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentNum = currentBlock.getBlockHeader().getRawData().getNumber();
    //endNum - startNum > 1000
    try{
      PublicMethed.getShieldNotesByIvkOnSolidityWithBlockNum(receiverShieldAddressInfo,currentNum-1004,currentNum,
              blockingStubSolidity);
    }catch (Exception e){
      logger.info(e.toString());
      Assert.assertTrue(e.toString().contains(scanNoteException));
    }
    try{
      PublicMethed.getShieldNotesByOvkOnSolidityWithBlockNum(sendShieldAddressInfo,currentNum-1004,currentNum,
              blockingStubSolidity);
    }catch (Exception e){
      logger.info("==== endNum - startNum > 1000  ovk==== :"+e.toString());
      Assert.assertTrue(e.toString().contains(scanNoteException));
    }
    //startNum < 0
    try{
      PublicMethed.getShieldNotesByIvkOnSolidityWithBlockNum(receiverShieldAddressInfo,-1L,10L,
              blockingStubSolidity);
    }catch (Exception e){
      Assert.assertTrue(e.toString().contains(scanNoteException));
    }
    try{
      PublicMethed.getShieldNotesByOvkOnSolidityWithBlockNum(sendShieldAddressInfo,-1L,10L,
              blockingStubSolidity);
    }catch (Exception e){
      logger.info("==== startNum < 0  ovk==== :"+e.toString());
      Assert.assertTrue(e.toString().contains(scanNoteException));
    }

    //startNum > endNum
    try{
      PublicMethed.getShieldNotesByIvkOnSolidityWithBlockNum(receiverShieldAddressInfo,currentNum,currentNum-1004,
              blockingStubSolidity);
    }catch (Exception e){
      Assert.assertTrue(e.toString().contains(scanNoteException));
    }
    try{
      PublicMethed.getShieldNotesByOvkOnSolidityWithBlockNum(sendShieldAddressInfo,currentNum,currentNum-1004,
              blockingStubSolidity);
    }catch (Exception e){
      logger.info("==== startNum > endNum  ovk==== :"+e.toString());
      Assert.assertTrue(e.toString().contains(scanNoteException));
    }
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Scan note by ivk and scan not by ivk on solidity")
  public void test05ScanNoteByIvkAndOvkOnSolidityServer() {
    //Scan sender note by ovk equals scan receiver note by ivk in Solidity
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity1);
    Note scanNoteByIvk = PublicMethed
        .getShieldNotesByIvkOnSolidity(receiverShieldAddressInfo, blockingStubSolidity1)
        .getNoteTxs(0).getNote();
    Note scanNoteByOvk = PublicMethed
        .getShieldNotesByOvkOnSolidity(sendShieldAddressInfo, blockingStubSolidity1)

        .getNoteTxs(0).getNote();
    Assert.assertEquals(scanNoteByIvk.getValue(), scanNoteByOvk.getValue());
    Assert.assertEquals(scanNoteByIvk.getMemo(), scanNoteByOvk.getMemo());
    Assert.assertEquals(scanNoteByIvk.getRcm(), scanNoteByOvk.getRcm());
    Assert.assertEquals(scanNoteByIvk.getPaymentAddress(), scanNoteByOvk.getPaymentAddress());

    Protocol.Block currentBlock = blockingStubSolidity.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentNum = currentBlock.getBlockHeader().getRawData().getNumber();
    //endNum - startNum > 1000
    try{
      PublicMethed.getShieldNotesByIvkOnSolidityWithBlockNum(receiverShieldAddressInfo,currentNum-1004,currentNum,
              blockingStubSolidity1);
    }catch (Exception e){
      Assert.assertTrue(e.toString().contains(scanNoteException));
    }
    try{
      PublicMethed.getShieldNotesByOvkOnSolidityWithBlockNum(sendShieldAddressInfo,currentNum-1004,currentNum,
              blockingStubSolidity1);
    }catch (Exception e){
      Assert.assertTrue(e.toString().contains(scanNoteException));
    }
    //startNum < 0
    try{
      PublicMethed.getShieldNotesByIvkOnSolidityWithBlockNum(receiverShieldAddressInfo,-1L,10L,
              blockingStubSolidity1);
    }catch (Exception e){
      Assert.assertTrue(e.toString().contains(scanNoteException));
    }
    try{
      PublicMethed.getShieldNotesByOvkOnSolidityWithBlockNum(sendShieldAddressInfo,-1L,10L,
              blockingStubSolidity1);
    }catch (Exception e){
      logger.info("==== startNum < 0  ovk==== :"+e.toString());
      Assert.assertTrue(e.toString().contains(scanNoteException));
    }

    //startNum > endNum
    try{
      PublicMethed.getShieldNotesByIvkOnSolidityWithBlockNum(receiverShieldAddressInfo,currentNum,currentNum-1004,
              blockingStubSolidity1);
    }catch (Exception e){
      Assert.assertTrue(e.toString().contains(scanNoteException));
    }
    try{
      PublicMethed.getShieldNotesByOvkOnSolidityWithBlockNum(sendShieldAddressInfo,currentNum,currentNum-1004,
              blockingStubSolidity1);
    }catch (Exception e){
      logger.info("==== startNum > endNum  ovk==== :"+e.toString());
      Assert.assertTrue(e.toString().contains(scanNoteException));
    }
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Query whether note is spend on solidity")
  public void test06QueryNoteIsSpendOnSolidity() {
    notes = PublicMethed.listShieldNote(sendShieldAddressInfo, blockingStubFull);
    //Scan sender note by ovk equals scan receiver note by ivk in Solidity
    Assert.assertTrue(PublicMethed.getSpendResult(sendShieldAddressInfo.get(),
        notes.getNoteTxs(0), blockingStubFull).getResult());
    Assert.assertTrue(PublicMethed.getSpendResultOnSolidity(sendShieldAddressInfo.get(),
        notes.getNoteTxs(0), blockingStubSolidity).getResult());
    Assert.assertTrue(PublicMethed.getSpendResultOnSolidity(sendShieldAddressInfo.get(),
        notes.getNoteTxs(0), blockingStubSolidity1).getResult());
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Query note and spend status on fullnode and solidity")
  public void test07QueryNoteAndSpendStatusOnFullnode() {
    Assert.assertFalse(
        PublicMethed.getShieldNotesAndMarkByIvk(receiverShieldAddressInfo, blockingStubFull)
            .getNoteTxs(0).getIsSpend());
    Note scanNoteByIvk = PublicMethed
        .getShieldNotesByIvk(receiverShieldAddressInfo, blockingStubFull)
        .getNoteTxs(0).getNote();
    Assert.assertEquals(scanNoteByIvk,
        PublicMethed.getShieldNotesAndMarkByIvk(receiverShieldAddressInfo, blockingStubFull)
            .getNoteTxs(0).getNote());

    Assert.assertFalse(PublicMethed
        .getShieldNotesAndMarkByIvkOnSolidity(receiverShieldAddressInfo, blockingStubSolidity)
        .getNoteTxs(0).getIsSpend());
    scanNoteByIvk = PublicMethed
        .getShieldNotesByIvkOnSolidity(receiverShieldAddressInfo, blockingStubSolidity)
        .getNoteTxs(0).getNote();
    Assert.assertEquals(scanNoteByIvk, PublicMethed
        .getShieldNotesAndMarkByIvkOnSolidity(receiverShieldAddressInfo, blockingStubSolidity)
        .getNoteTxs(0).getNote());

    shieldOutList.clear();
    memo = "Query note and spend status on fullnode " + System.currentTimeMillis();
    notes = PublicMethed.listShieldNote(receiverShieldAddressInfo, blockingStubFull);
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, sendShieldAddress,
        "" + (notes.getNoteTxs(0).getNote().getValue() - zenTokenFee), memo);
    Assert.assertTrue(PublicMethed.sendShieldCoin(
        null, 0,
        receiverShieldAddressInfo.get(), notes.getNoteTxs(0),
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity);

    Assert.assertTrue(
        PublicMethed.getShieldNotesAndMarkByIvk(receiverShieldAddressInfo, blockingStubFull)
            .getNoteTxs(0).getIsSpend());

    Assert.assertTrue(PublicMethed
        .getShieldNotesAndMarkByIvkOnSolidity(receiverShieldAddressInfo, blockingStubSolidity)
        .getNoteTxs(0).getIsSpend());
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Query note and spend status on fullnode and solidity with block num")
  public void test08RQueryNoteAndSpendStatusWithBlockNum() {
    Protocol.Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Protocol.Block currentBlock1 = blockingStubSolidity.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentNum1 = currentBlock1.getBlockHeader().getRawData().getNumber();
    //endNum - startNum > 1000
    try{
      PublicMethed.getShieldNotesAndMarkByIvkWithBlockNum(receiverShieldAddressInfo,currentNum-1004,currentNum,
              blockingStubFull);
    }catch (Exception e){
      Assert.assertTrue(e.toString().contains(scanNoteException));
    }
    try{
      PublicMethed.getShieldNotesAndMarkByIvkOnSolidityWithBlockNum(receiverShieldAddressInfo,currentNum1-1004,currentNum1,
              blockingStubSolidity);
    }catch (Exception e){
      logger.info("==== endNum - startNum > 1000  ovk==== :"+e.toString());
      Assert.assertTrue(e.toString().contains(scanNoteException));
    }
    //startNum < 0
    try{
      PublicMethed.getShieldNotesAndMarkByIvkWithBlockNum(receiverShieldAddressInfo,-1L,10L,
              blockingStubFull);
    }catch (Exception e){
      Assert.assertTrue(e.toString().contains(scanNoteException));
    }
    try{
      PublicMethed.getShieldNotesAndMarkByIvkOnSolidityWithBlockNum(receiverShieldAddressInfo,-1L,10L,
              blockingStubSolidity);
    }catch (Exception e){
      logger.info("==== startNum < 0  ovk==== :"+e.toString());
      Assert.assertTrue(e.toString().contains(scanNoteException));
    }

    //startNum > endNum
    try{
      PublicMethed.getShieldNotesAndMarkByIvkWithBlockNum(receiverShieldAddressInfo,currentNum,currentNum-1004,
              blockingStubFull);
    }catch (Exception e){
      Assert.assertTrue(e.toString().contains(scanNoteException));
    }
    try{
      PublicMethed.getShieldNotesAndMarkByIvkOnSolidityWithBlockNum(receiverShieldAddressInfo,currentNum1,currentNum1-1004,
              blockingStubSolidity);
    }catch (Exception e){
      logger.info("==== startNum > endNum  ovk==== :"+e.toString());
      Assert.assertTrue(e.toString().contains(scanNoteException));
    }
  }

  @Test(enabled = true, description = "Get merkle tree voucher info")
  public void test09GetMerkleTreeVoucherInfo() {
    notes = PublicMethed.listShieldNote(sendShieldAddressInfo, blockingStubFull);
    sendNote = notes.getNoteTxs(notes.getNoteTxsCount()-1).getNote();
    logger.info("******  test8:"+sendNote.toString());
    OutputPointInfo.Builder request = OutputPointInfo.newBuilder();

    OutputPoint.Builder outPointBuild = OutputPoint.newBuilder();
    outPointBuild.setHash(ByteString.copyFrom(notes.getNoteTxs(notes.getNoteTxsCount()-1).getTxid().toByteArray()));
    outPointBuild.setIndex(notes.getNoteTxs(notes.getNoteTxsCount()-1).getIndex());
    request.addOutPoints(outPointBuild.build());
    secondMerkleVoucherInfo = blockingStubFull
        .getMerkleTreeVoucherInfo(request.build());
    logger.info("******  test8-  firstMerkleVoucherInfo:"+firstMerkleVoucherInfo.toString());
    logger.info("******  test8-  secondMerkleVoucherInfo:"+secondMerkleVoucherInfo.toString());
    Assert.assertNotEquals(firstMerkleVoucherInfo, secondMerkleVoucherInfo);
  }

  @Test(enabled = true, description = "Get merkle tree voucher info with blocknum")
  public void test10GetMerkleTreeVoucherInfoWithBlocknum() {
    notes = PublicMethed.listShieldNote(sendShieldAddressInfo, blockingStubFull);

    sendNote = notes.getNoteTxs(notes.getNoteTxsCount()-1).getNote();
    logger.info("******  test9:"+sendNote.toString());
    OutputPointInfo.Builder request = OutputPointInfo.newBuilder();

    OutputPoint.Builder outPointBuild = OutputPoint.newBuilder();
    outPointBuild.setHash(ByteString.copyFrom(notes.getNoteTxs(notes.getNoteTxsCount()-1).getTxid().toByteArray()));
    outPointBuild.setIndex(notes.getNoteTxs(notes.getNoteTxsCount()-1).getIndex());
    request.addOutPoints(outPointBuild.build());
    request.setBlockNum(1);
    secondMerkleVoucherInfo = blockingStubFull
            .getMerkleTreeVoucherInfo(request.build());
    Assert.assertTrue(secondMerkleVoucherInfo.toString().contains("tree"));
    Assert.assertTrue(secondMerkleVoucherInfo.toString().contains("rt"));
    Assert.assertTrue(secondMerkleVoucherInfo.toString().contains("path"));
    Assert.assertEquals(secondMerkleVoucherInfo.getVouchersCount(),1);

    request.clear();
    OutputPoint.Builder outPointBuild1 = OutputPoint.newBuilder();
    outPointBuild1.setHash(ByteString.copyFrom(notes.getNoteTxs(notes.getNoteTxsCount()-1).getTxid().toByteArray()));
    outPointBuild1.setIndex(notes.getNoteTxs(notes.getNoteTxsCount()-1).getIndex());
    request.addOutPoints(outPointBuild1.build());
    request.setBlockNum(300);
    try {
      secondMerkleVoucherInfo = blockingStubFull
              .getMerkleTreeVoucherInfo(request.build());
    }catch (Exception e){
      Assert.assertTrue(e.toString().contains(
              "synBlockNum is too large, cmBlockNum plus synBlockNum must be <= latestBlockNumber"));
    }

  }

  @Test(enabled = true, description = "Get merkle tree voucher info list with multi OutputPoints ")
  public void test11GetMerkleTreeVoucherInfoList() {
    notes = PublicMethed.listShieldNote(sendShieldAddressInfo, blockingStubFull);
    sendNote = notes.getNoteTxs(notes.getNoteTxsCount()-1).getNote();
    logger.info("******  test10:"+sendNote.toString());
    OutputPointInfo.Builder request = OutputPointInfo.newBuilder();

    //outputPoint num is 0   
    try {
      secondMerkleVoucherInfo = blockingStubFull
              .getMerkleTreeVoucherInfo(request.build());
    }catch (Exception e){
      Assert.assertTrue(e.toString().contains(
              "request.OutPointsCount must be speccified with range in【1，10】"));
    }

    //outputPoint num is 5
    OutputPoint.Builder outPointBuild = OutputPoint.newBuilder();
    outPointBuild.setHash(ByteString.copyFrom(notes.getNoteTxs(notes.getNoteTxsCount()-1).getTxid().toByteArray()));
    outPointBuild.setIndex(notes.getNoteTxs(notes.getNoteTxsCount()-1).getIndex());
    request.addOutPoints(outPointBuild.build());
    request.addOutPoints(outPointBuild.build());
    request.addOutPoints(outPointBuild.build());
    request.addOutPoints(outPointBuild.build());
    request.addOutPoints(outPointBuild.build());
    request.setBlockNum(1);
    secondMerkleVoucherInfo = blockingStubFull
            .getMerkleTreeVoucherInfo(request.build());
    Assert.assertEquals(secondMerkleVoucherInfo.getVouchersCount(),5);
    Assert.assertEquals(secondMerkleVoucherInfo.getPathsCount(),5);
    for(int i=0;i<secondMerkleVoucherInfo.getVouchersCount();i++){
      Assert.assertTrue(secondMerkleVoucherInfo.getVouchers(i).toString().contains("tree"));
      Assert.assertTrue(secondMerkleVoucherInfo.getVouchers(i).toString().contains("rt"));
    }
    //outputPoint num is 10
    request.clear();
    OutputPoint.Builder outPointBuild1 = OutputPoint.newBuilder();
    outPointBuild1.setHash(ByteString.copyFrom(notes.getNoteTxs(notes.getNoteTxsCount()-1).getTxid().toByteArray()));
    outPointBuild1.setIndex(notes.getNoteTxs(notes.getNoteTxsCount()-1).getIndex());
    request.addOutPoints(outPointBuild1.build());
    request.addOutPoints(outPointBuild1.build());
    request.addOutPoints(outPointBuild1.build());
    request.addOutPoints(outPointBuild1.build());
    request.addOutPoints(outPointBuild1.build());
    request.addOutPoints(outPointBuild1.build());
    request.addOutPoints(outPointBuild1.build());
    request.addOutPoints(outPointBuild1.build());
    request.addOutPoints(outPointBuild1.build());
    request.addOutPoints(outPointBuild1.build());
    request.setBlockNum(1);
    secondMerkleVoucherInfo = blockingStubFull
            .getMerkleTreeVoucherInfo(request.build());
    Assert.assertEquals(secondMerkleVoucherInfo.getVouchersCount(),10);
    Assert.assertEquals(secondMerkleVoucherInfo.getPathsCount(),10);

    for(int i=0;i<secondMerkleVoucherInfo.getVouchersCount();i++){
      Assert.assertTrue(secondMerkleVoucherInfo.getVouchers(i).toString().contains("tree"));
      Assert.assertTrue(secondMerkleVoucherInfo.getVouchers(i).toString().contains("rt"));
    }
    
    //outputPoint num is 11
    request.clear();
    OutputPoint.Builder outPointBuild2 = OutputPoint.newBuilder();
    outPointBuild2.setHash(ByteString.copyFrom(notes.getNoteTxs(notes.getNoteTxsCount()-1).getTxid().toByteArray()));
    outPointBuild2.setIndex(notes.getNoteTxs(notes.getNoteTxsCount()-1).getIndex());
    request.addOutPoints(outPointBuild2.build());
    request.addOutPoints(outPointBuild2.build());
    request.addOutPoints(outPointBuild2.build());
    request.addOutPoints(outPointBuild2.build());
    request.addOutPoints(outPointBuild2.build());
    request.addOutPoints(outPointBuild2.build());
    request.addOutPoints(outPointBuild2.build());
    request.addOutPoints(outPointBuild2.build());
    request.addOutPoints(outPointBuild2.build());
    request.addOutPoints(outPointBuild2.build());
    request.addOutPoints(outPointBuild2.build());
    request.setBlockNum(1);    
    try {
      secondMerkleVoucherInfo = blockingStubFull
              .getMerkleTreeVoucherInfo(request.build());
    }catch (Exception e){
      Assert.assertTrue(e.toString().contains(
              "request.OutPointsCount must be speccified with range in【1，10】"));
    }

  }

  @Test(enabled = true, description = "if note index not equal ,merkle tree voucher info must not equal ")
  public void test12GetMerkleTreeVoucherInfoWithDiffIndex(){
    Assert.assertTrue(PublicMethed.transferAsset(zenTokenOwnerAddress, tokenId,
            costTokenAmount, foundationZenTokenAddress, foundationZenTokenKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Args.getInstance().setFullNodeAllowShieldedTransaction(true);
    Optional<ShieldAddressInfo> shieldAddressInfo1 = PublicMethed.generateShieldAddress();
    String shieldAddress1 = shieldAddressInfo1.get().getAddress();
    Optional<ShieldAddressInfo> shieldAddressInfo2 = PublicMethed.generateShieldAddress();
    String shieldAddress2 = shieldAddressInfo2.get().getAddress();
    String memo1 = "Diff Index shield memo1 in" + System.currentTimeMillis();
    String memo2 = "Diff Index shield memo2 in" + System.currentTimeMillis();
    shieldOutList.clear();
    Long mount1 = 4 * zenTokenFee;
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, shieldAddress1,
            "" + mount1, memo1);
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, shieldAddress2,
            "" + (sendTokenAmount - mount1-zenTokenFee), memo2);
    Assert.assertTrue(PublicMethed.sendShieldCoin(zenTokenOwnerAddress, sendTokenAmount, null,
            null, shieldOutList, null, 0, zenTokenOwnerKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    DecryptNotes notes1 = PublicMethed.listShieldNote(shieldAddressInfo1, blockingStubFull);
    DecryptNotes notes2 = PublicMethed.listShieldNote(shieldAddressInfo2, blockingStubFull);

    OutputPointInfo.Builder request = OutputPointInfo.newBuilder();
    OutputPoint.Builder outPointBuild = OutputPoint.newBuilder();
    outPointBuild.setHash(ByteString.copyFrom(notes1.getNoteTxs(notes1.getNoteTxsCount()-1).getTxid().toByteArray()));
    outPointBuild.setIndex(notes1.getNoteTxs(notes1.getNoteTxsCount()-1).getIndex());
    request.addOutPoints(outPointBuild.build());
    firstMerkleVoucherInfo = blockingStubFull
            .getMerkleTreeVoucherInfo(request.build());

    request.clear();
    outPointBuild.clear();
    outPointBuild.setHash(ByteString.copyFrom(notes2.getNoteTxs(notes2.getNoteTxsCount()-1).getTxid().toByteArray()));
    outPointBuild.setIndex(notes2.getNoteTxs(notes2.getNoteTxsCount()-1).getIndex());
    request.addOutPoints(outPointBuild.build());
    secondMerkleVoucherInfo = blockingStubFull
            .getMerkleTreeVoucherInfo(request.build());
    Assert.assertEquals(notes1.getNoteTxs(notes1.getNoteTxsCount()-1).getTxid(),
            notes2.getNoteTxs(notes2.getNoteTxsCount()-1).getTxid());
    Assert.assertNotEquals(firstMerkleVoucherInfo,secondMerkleVoucherInfo);

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