package org.tron.core;

import static org.tron.core.zksnark.LibrustzcashTest.librustzcashInitZksnarkParams;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.PrivateShieldedTRC20ParametersWithoutAsk;
import org.tron.api.GrpcAPI.ShieldedTRC20TriggerContractParameters;
import org.tron.api.GrpcAPI.SpendAuthSigParameters;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.Hash;
import org.tron.common.zksnark.JLibrustzcash;
import org.tron.common.zksnark.LibrustzcashParam;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.zen.address.DiversifierT;
import org.tron.core.zen.address.ExpandedSpendingKey;
import org.tron.core.zen.address.FullViewingKey;
import org.tron.core.zen.address.IncomingViewingKey;
import org.tron.core.zen.address.KeyIo;
import org.tron.core.zen.address.PaymentAddress;
import org.tron.core.zen.address.SpendingKey;
import org.tron.core.zen.note.Note;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.ShieldContract;
import org.tron.protos.contract.SmartContractOuterClass;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ShieldedTRC20ContractTest {

  private static ManagedChannel channelFull = null;
  private static WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private static ManagedChannel channelSolidity = null;
  private static WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private static String fullnode = "127.0.0.1:50051";
  private static String soliditynode = "127.0.0.1:50061";
  private String trc20ContractAddress = "TX89RWqH3gX3m5wvZvSDaedyLBF4SzZqy5";
  private String shieldedTRC20ContractAddress = "TB8MuSWh979b4donqWUZtFJ4aYAemZ1R6U";
  private String privateKey = "650950B193DDDDB35B6E48912DD28F7AB0E7140C1BFDEFD493348F02295BD812";
  private String pubAddress = "TFsrP7YcSSRwHzLPwaCnXyTKagHs8rXKNJ";

  @BeforeClass
  public static void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
    Args.getInstance().setFullNodeAllowShieldedTRC20TransactionArgs(true);
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @AfterClass
  public static void afterClass() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  @Ignore
  @Test
  public void printTRC20Address() {
    logger.info(Hex.toHexString(WalletClient.decodeFromBase58Check(trc20ContractAddress)));
  }

  @Ignore
  @Test
  public void getTrc20AccountBalance() {
    byte[] contractAddress = WalletClient
        .decodeFromBase58Check(trc20ContractAddress);
    logger.info("trc20 contract address: " + ByteArray.toHexString(contractAddress));
    byte[] userAccountAddress = new byte[32];
    byte[] shieldedContractAddress = WalletClient
        .decodeFromBase58Check(shieldedTRC20ContractAddress);
    System.arraycopy(shieldedContractAddress, 0, userAccountAddress, 11, 21);
    String methodSign = "balanceOf(address)";
    byte[] selector = new byte[4];
    System.arraycopy(Hash.sha3(methodSign.getBytes()), 0, selector, 0, 4);
    byte[] input = ByteUtil.merge(selector, userAccountAddress);
    SmartContractOuterClass.TriggerSmartContract.Builder triggerBuilder = SmartContractOuterClass
        .TriggerSmartContract.newBuilder();
    triggerBuilder.setContractAddress(ByteString.copyFrom(contractAddress));
    triggerBuilder.setData(ByteString.copyFrom(input));
    GrpcAPI.TransactionExtention trxExt2 = blockingStubFull.triggerConstantContract(
        triggerBuilder.build());

    List<ByteString> list = trxExt2.getConstantResultList();
    byte[] listBytes = new byte[0];
    for (ByteString bs : list) {
      listBytes = ByteUtil.merge(listBytes, bs.toByteArray());
    }
    logger.info("balance " + Hex.toHexString(listBytes));
  }

  @Ignore
  @Test
  public void setAllowance() {
    byte[] contractAddress = WalletClient
        .decodeFromBase58Check(trc20ContractAddress);
    byte[] shieldedContractAddress = WalletClient
        .decodeFromBase58Check(shieldedTRC20ContractAddress);
    byte[] shieldedContractAddressPadding = new byte[32];
    System.arraycopy(shieldedContractAddress, 0, shieldedContractAddressPadding, 11, 21);
    logger.info("shielded contract addr " + ByteArray.toHexString(shieldedContractAddressPadding));
    byte[] valueBytes = longTo32Bytes(100_000L);
    String input = Hex.toHexString(ByteUtil.merge(shieldedContractAddressPadding, valueBytes));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    byte[] callerAddress = WalletClient.decodeFromBase58Check(pubAddress);
    String txid = PublicMethed.triggerContract(contractAddress,
        "approve(address,uint256)",
        input,
        true,
        0L,
        1000000000L,
        callerAddress,
        privateKey,
        blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(
        Transaction.Result.contractResult.SUCCESS, infoById.get().getReceipt().getResult());
  }

  @Ignore
  @Test
  public void testCreateShieldedContractParametersForMint() throws ZksnarkException {
    librustzcashInitZksnarkParams();
    long fromAmount = 50;
    SpendingKey sk = SpendingKey.random();
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    byte[] ovk = expsk.getOvk();

    //ReceiveNote
    GrpcAPI.ReceiveNote.Builder revNoteBuilder = GrpcAPI.ReceiveNote.newBuilder();
    SpendingKey spendingKey = SpendingKey.decode(privateKey);
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
    long revValue = 50;
    byte[] memo = new byte[512];
    byte[] rcm = Note.generateR();
    String paymentAddressStr = KeyIo.encodePaymentAddress(paymentAddress);
    GrpcAPI.Note revNote = getNote(revValue, paymentAddressStr, rcm, memo);
    revNoteBuilder.setNote(revNote);

    byte[] contractAddress = WalletClient.decodeFromBase58Check(shieldedTRC20ContractAddress);
    GrpcAPI.PrivateShieldedTRC20Parameters.Builder paramBuilder = GrpcAPI
        .PrivateShieldedTRC20Parameters.newBuilder();
    paramBuilder.setOvk(ByteString.copyFrom(ovk));
    paramBuilder.setFromAmount(fromAmount);
    paramBuilder.addShieldedReceives(revNoteBuilder.build());
    paramBuilder.setShieldedTRC20ContractAddress(ByteString.copyFrom(contractAddress));

    GrpcAPI.ShieldedTRC20Parameters trc20MintParams = blockingStubFull
        .createShieldedContractParameters(paramBuilder.build());
    GrpcAPI.PrivateShieldedTRC20Parameters trc20Params = paramBuilder.build();
    logger.info(Hex.toHexString(trc20Params.getOvk().toByteArray()));
    logger.info(String.valueOf(trc20Params.getFromAmount()));
    logger.info(String.valueOf(trc20Params.getShieldedReceives(0).getNote().getValue()));
    logger.info(trc20Params.getShieldedReceives(0).getNote().getPaymentAddress());
    logger.info(Hex.toHexString(
        trc20Params.getShieldedReceives(0).getNote().getRcm().toByteArray()));
    logger.info(Hex.toHexString(trc20Params.getShieldedTRC20ContractAddress().toByteArray()));

    //verify receiveProof && bindingSignature
    boolean result;
    long ctx = JLibrustzcash.librustzcashSaplingVerificationCtxInit();
    ShieldContract.ReceiveDescription revDesc = trc20MintParams.getReceiveDescription(0);
    try {
      result = JLibrustzcash.librustzcashSaplingCheckOutput(
          new LibrustzcashParam.CheckOutputParams(
              ctx,
              revDesc.getValueCommitment().toByteArray(),
              revDesc.getNoteCommitment().toByteArray(),
              revDesc.getEpk().toByteArray(),
              revDesc.getZkproof().toByteArray()));
      long valueBalance = -revValue;
      result &= JLibrustzcash.librustzcashSaplingFinalCheck(
          new LibrustzcashParam.FinalCheckParams(
              ctx,
              valueBalance,
              trc20MintParams.getBindingSignature().toByteArray(),
              trc20MintParams.getMessageHash().toByteArray()));
    } catch (Throwable any) {
      result = false;
    } finally {
      JLibrustzcash.librustzcashSaplingVerificationCtxFree(ctx);
    }
    Assert.assertTrue(result);
    byte[] callerAddress = WalletClient.decodeFromBase58Check(pubAddress);
    String txid1 = triggerMint(blockingStubFull, contractAddress, callerAddress, privateKey,
        trc20MintParams.getTriggerContractInput());
    logger.info("..............result...........");
    logger.info(txid1);
    logger.info("..............end..............");
  }

  @Ignore
  @Test
  public void testCreateShieldedContractParametersForTransfer1v1()
      throws ZksnarkException, ContractValidateException {
    byte[] contractAddress = WalletClient
        .decodeFromBase58Check(shieldedTRC20ContractAddress);
    byte[] callerAddress = WalletClient.decodeFromBase58Check(pubAddress);
    SpendingKey sk = SpendingKey.decode(privateKey);
    GrpcAPI.PrivateShieldedTRC20Parameters mintPrivateParam1 = mintParams(
        privateKey, 60, shieldedTRC20ContractAddress);
    GrpcAPI.ShieldedTRC20Parameters mintParam1 = blockingStubFull.createShieldedContractParameters(
        mintPrivateParam1);
    String mintInput1 = mintParamsToHexString(mintParam1, 60);
    String txid1 = triggerMint(
        blockingStubFull, contractAddress, callerAddress, privateKey, mintInput1);
    logger.info("..............mint result...........");
    logger.info(txid1);
    logger.info("..............end..............");

    // SpendNoteTRC20 1
    Optional<TransactionInfo> infoById1 = PublicMethed
        .getTransactionInfoById(txid1, blockingStubFull);
    byte[] tx1Data = infoById1.get().getLog(0).getData().toByteArray();
    long pos1 = bytes32Tolong(ByteArray.subArray(tx1Data, 0, 32));
    byte[] contractResult1 = triggerGetPath(
        blockingStubFull, contractAddress, callerAddress, privateKey, pos1);
    byte[] path1 = ByteArray.subArray(contractResult1, 32, 1056);
    byte[] root1 = ByteArray.subArray(contractResult1, 0, 32);
    logger.info(Hex.toHexString(contractResult1));
    GrpcAPI.SpendNoteTRC20.Builder note1Builder = GrpcAPI.SpendNoteTRC20.newBuilder();
    note1Builder.setAlpha(ByteString.copyFrom(Note.generateR()));
    note1Builder.setPos(pos1);
    note1Builder.setPath(ByteString.copyFrom(path1));
    note1Builder.setRoot(ByteString.copyFrom(root1));
    note1Builder.setNote(mintPrivateParam1.getShieldedReceives(0).getNote());
    GrpcAPI.PrivateShieldedTRC20Parameters.Builder privateTRC20Builder = GrpcAPI
        .PrivateShieldedTRC20Parameters.newBuilder();
    privateTRC20Builder.addShieldedSpends(note1Builder.build());

    //ReceiveNote 1
    GrpcAPI.ReceiveNote.Builder revNoteBuilder = GrpcAPI.ReceiveNote.newBuilder();
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
    long revValue = 60;
    byte[] memo = new byte[512];
    byte[] rcm = Note.generateR();
    String paymentAddressStr = KeyIo.encodePaymentAddress(paymentAddress);
    GrpcAPI.Note revNote = getNote(revValue, paymentAddressStr, rcm, memo);
    revNoteBuilder.setNote(revNote);
    privateTRC20Builder.addShieldedReceives(revNoteBuilder.build());

    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    privateTRC20Builder.setAsk(ByteString.copyFrom(expsk.getAsk()));
    privateTRC20Builder.setNsk(ByteString.copyFrom(expsk.getNsk()));
    privateTRC20Builder.setOvk(ByteString.copyFrom(expsk.getOvk()));
    privateTRC20Builder.setShieldedTRC20ContractAddress(ByteString.copyFrom(contractAddress));
    GrpcAPI.ShieldedTRC20Parameters transferParam = blockingStubFull
        .createShieldedContractParameters(privateTRC20Builder.build());
    String txid = triggerTransfer(
        blockingStubFull, contractAddress, callerAddress, privateKey,
        transferParam.getTriggerContractInput());
    logger.info("..............transfer result...........");
    logger.info(txid);
    logger.info("..............end..............");
  }

  @Ignore
  @Test
  public void testCreateShieldedContractParametersForTransfer1v2()
      throws ZksnarkException, ContractValidateException {
    byte[] contractAddress = WalletClient
        .decodeFromBase58Check(shieldedTRC20ContractAddress);
    byte[] callerAddress = WalletClient.decodeFromBase58Check(pubAddress);
    SpendingKey sk = SpendingKey.decode(privateKey);
    GrpcAPI.PrivateShieldedTRC20Parameters mintPrivateParam1 = mintParams(
        privateKey, 100, shieldedTRC20ContractAddress);
    GrpcAPI.ShieldedTRC20Parameters mintParam1 = blockingStubFull.createShieldedContractParameters(
        mintPrivateParam1);

    String mintInput1 = mintParamsToHexString(mintParam1, 100);
    String txid1 = triggerMint(
        blockingStubFull, contractAddress, callerAddress, privateKey, mintInput1);
    logger.info("..............mint result...........");
    logger.info(txid1);
    logger.info("..............end..............");

    // SpendNoteTRC20 1
    Optional<TransactionInfo> infoById1 = PublicMethed
        .getTransactionInfoById(txid1, blockingStubFull);
    byte[] tx1Data = infoById1.get().getLog(0).getData().toByteArray();
    long pos1 = bytes32Tolong(ByteArray.subArray(tx1Data, 0, 32));
    byte[] contractResult1 = triggerGetPath(
        blockingStubFull, contractAddress, callerAddress, privateKey, pos1);
    byte[] path1 = ByteArray.subArray(contractResult1, 32, 1056);
    byte[] root1 = ByteArray.subArray(contractResult1, 0, 32);
    logger.info(Hex.toHexString(contractResult1));
    GrpcAPI.SpendNoteTRC20.Builder note1Builder = GrpcAPI.SpendNoteTRC20.newBuilder();
    note1Builder.setAlpha(ByteString.copyFrom(Note.generateR()));
    note1Builder.setPos(pos1);
    note1Builder.setPath(ByteString.copyFrom(path1));
    note1Builder.setRoot(ByteString.copyFrom(root1));
    note1Builder.setNote(mintPrivateParam1.getShieldedReceives(0).getNote());
    GrpcAPI.PrivateShieldedTRC20Parameters.Builder privateTRC20Builder = GrpcAPI
        .PrivateShieldedTRC20Parameters.newBuilder();
    privateTRC20Builder.addShieldedSpends(note1Builder.build());

    //ReceiveNote 1
    GrpcAPI.ReceiveNote.Builder revNoteBuilder = GrpcAPI.ReceiveNote.newBuilder();
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
    long revValue = 30;
    byte[] memo = new byte[512];
    byte[] rcm = Note.generateR();
    String paymentAddressStr = KeyIo.encodePaymentAddress(paymentAddress);
    GrpcAPI.Note revNote = getNote(revValue, paymentAddressStr, rcm, memo);
    revNoteBuilder.setNote(revNote);

    //ReceiveNote 2
    GrpcAPI.ReceiveNote.Builder revNoteBuilder2 = GrpcAPI.ReceiveNote.newBuilder();
    FullViewingKey fullViewingKey2 = sk.fullViewingKey();
    IncomingViewingKey incomingViewingKey2 = fullViewingKey2.inViewingKey();
    PaymentAddress paymentAddress2 = incomingViewingKey2.address(DiversifierT.random()).get();
    long revValue2 = 70;
    byte[] memo2 = new byte[512];
    byte[] rcm2 = Note.generateR();
    String paymentAddressStr2 = KeyIo.encodePaymentAddress(paymentAddress2);
    GrpcAPI.Note revNote2 = getNote(revValue2, paymentAddressStr2, rcm2, memo2);
    revNoteBuilder2.setNote(revNote2);
    privateTRC20Builder.addShieldedReceives(revNoteBuilder.build());
    privateTRC20Builder.addShieldedReceives(revNoteBuilder2.build());
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();

    privateTRC20Builder.setAsk(ByteString.copyFrom(expsk.getAsk()));
    privateTRC20Builder.setNsk(ByteString.copyFrom(expsk.getNsk()));
    privateTRC20Builder.setOvk(ByteString.copyFrom(expsk.getOvk()));
    privateTRC20Builder.setShieldedTRC20ContractAddress(ByteString.copyFrom(contractAddress));
    GrpcAPI.ShieldedTRC20Parameters transferParam = blockingStubFull
        .createShieldedContractParameters(privateTRC20Builder.build());
    String txid = triggerTransfer(
        blockingStubFull, contractAddress, callerAddress, privateKey,
        transferParam.getTriggerContractInput());
    logger.info("..............transfer result...........");
    logger.info(txid);
    logger.info("..............end..............");
  }

  @Ignore
  @Test
  public void testCreateShieldedContractParametersForTransfer2v1()
      throws ZksnarkException, ContractValidateException {
    byte[] contractAddress = WalletClient
        .decodeFromBase58Check(shieldedTRC20ContractAddress);
    byte[] callerAddress = WalletClient.decodeFromBase58Check(pubAddress);
    SpendingKey sk = SpendingKey.decode(privateKey);
    GrpcAPI.PrivateShieldedTRC20Parameters mintPrivateParam1 = mintParams(
        privateKey, 60, shieldedTRC20ContractAddress);
    GrpcAPI.ShieldedTRC20Parameters mintParam1 = blockingStubFull.createShieldedContractParameters(
        mintPrivateParam1);
    GrpcAPI.PrivateShieldedTRC20Parameters mintPrivateParam2 = mintParams(
        privateKey, 40, shieldedTRC20ContractAddress);
    GrpcAPI.ShieldedTRC20Parameters mintParam2 = blockingStubFull.createShieldedContractParameters(
        mintPrivateParam2);

    String mintInput1 = mintParamsToHexString(mintParam1, 60);
    String mintInput2 = mintParamsToHexString(mintParam2, 40);
    String txid1 = triggerMint(blockingStubFull, contractAddress, callerAddress, privateKey,
        mintInput1);
    String txid2 = triggerMint(blockingStubFull, contractAddress, callerAddress, privateKey,
        mintInput2);
    logger.info("..............mint result...........");
    logger.info(txid1);
    logger.info(txid2);
    logger.info("..............end..............");

    // SpendNoteTRC20 1
    Optional<TransactionInfo> infoById1 = PublicMethed
        .getTransactionInfoById(txid1, blockingStubFull);
    byte[] tx1Data = infoById1.get().getLog(0).getData().toByteArray();
    long pos1 = bytes32Tolong(ByteArray.subArray(tx1Data, 0, 32));
    byte[] contractResult1 = triggerGetPath(
        blockingStubFull, contractAddress, callerAddress, privateKey, pos1);
    byte[] path1 = ByteArray.subArray(contractResult1, 32, 1056);
    byte[] root1 = ByteArray.subArray(contractResult1, 0, 32);
    logger.info(Hex.toHexString(contractResult1));
    GrpcAPI.SpendNoteTRC20.Builder note1Builder = GrpcAPI.SpendNoteTRC20.newBuilder();
    note1Builder.setAlpha(ByteString.copyFrom(Note.generateR()));
    note1Builder.setPos(pos1);
    note1Builder.setPath(ByteString.copyFrom(path1));
    note1Builder.setRoot(ByteString.copyFrom(root1));
    note1Builder.setNote(mintPrivateParam1.getShieldedReceives(0).getNote());

    // SpendNoteTRC20 2
    Optional<TransactionInfo> infoById2 = PublicMethed
        .getTransactionInfoById(txid2, blockingStubFull);
    byte[] tx2Data = infoById2.get().getLog(0).getData().toByteArray();
    long pos2 = bytes32Tolong(ByteArray.subArray(tx2Data, 0, 32));
    byte[] contractResult2 = triggerGetPath(
        blockingStubFull, contractAddress, callerAddress, privateKey, pos2);
    byte[] path2 = ByteArray.subArray(contractResult2, 32, 1056);
    byte[] root2 = ByteArray.subArray(contractResult2, 0, 32);
    logger.info(Hex.toHexString(contractResult2));
    GrpcAPI.SpendNoteTRC20.Builder note2Builder = GrpcAPI.SpendNoteTRC20.newBuilder();
    note2Builder.setAlpha(ByteString.copyFrom(Note.generateR()));
    note2Builder.setPos(pos2);
    note2Builder.setPath(ByteString.copyFrom(path2));
    note2Builder.setRoot(ByteString.copyFrom(root2));
    note2Builder.setNote(mintPrivateParam2.getShieldedReceives(0).getNote());
    GrpcAPI.PrivateShieldedTRC20Parameters.Builder privateTRC20Builder = GrpcAPI
        .PrivateShieldedTRC20Parameters.newBuilder();
    privateTRC20Builder.addShieldedSpends(note1Builder.build());
    privateTRC20Builder.addShieldedSpends(note2Builder.build());

    //ReceiveNote 1
    GrpcAPI.ReceiveNote.Builder revNoteBuilder = GrpcAPI.ReceiveNote.newBuilder();
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
    long revValue = 100;
    byte[] memo = new byte[512];
    byte[] rcm = Note.generateR();
    String paymentAddressStr = KeyIo.encodePaymentAddress(paymentAddress);
    GrpcAPI.Note revNote = getNote(revValue, paymentAddressStr, rcm, memo);
    revNoteBuilder.setNote(revNote);

    privateTRC20Builder.addShieldedReceives(revNoteBuilder.build());
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    privateTRC20Builder.setAsk(ByteString.copyFrom(expsk.getAsk()));
    privateTRC20Builder.setNsk(ByteString.copyFrom(expsk.getNsk()));
    privateTRC20Builder.setOvk(ByteString.copyFrom(expsk.getOvk()));
    privateTRC20Builder.setShieldedTRC20ContractAddress(ByteString.copyFrom(contractAddress));
    GrpcAPI.ShieldedTRC20Parameters transferParam = blockingStubFull
        .createShieldedContractParameters(privateTRC20Builder.build());
    String txid = triggerTransfer(
        blockingStubFull, contractAddress, callerAddress, privateKey,
        transferParam.getTriggerContractInput());
    logger.info("..............transfer result...........");
    logger.info(txid);
    logger.info("..............end..............");
  }

  @Ignore
  @Test
  public void testCreateShieldedContractParametersForTransfer2v2()
      throws ZksnarkException, ContractValidateException {
    byte[] contractAddress = WalletClient
        .decodeFromBase58Check(shieldedTRC20ContractAddress);
    byte[] callerAddress = WalletClient.decodeFromBase58Check(pubAddress);
    SpendingKey sk = SpendingKey.decode(privateKey);
    GrpcAPI.PrivateShieldedTRC20Parameters mintPrivateParam1 = mintParams(
        privateKey, 60, shieldedTRC20ContractAddress);
    GrpcAPI.ShieldedTRC20Parameters mintParam1 = blockingStubFull.createShieldedContractParameters(
        mintPrivateParam1);
    GrpcAPI.PrivateShieldedTRC20Parameters mintPrivateParam2 = mintParams(
        privateKey, 40, shieldedTRC20ContractAddress);
    GrpcAPI.ShieldedTRC20Parameters mintParam2 = blockingStubFull.createShieldedContractParameters(
        mintPrivateParam2);

    String mintInput1 = mintParamsToHexString(mintParam1, 60);
    String mintInput2 = mintParamsToHexString(mintParam2, 40);
    String txid1 = triggerMint(
        blockingStubFull, contractAddress, callerAddress, privateKey, mintInput1);
    String txid2 = triggerMint(
        blockingStubFull, contractAddress, callerAddress, privateKey, mintInput2);
    logger.info("..............mint result...........");
    logger.info(txid1);
    logger.info(txid2);
    logger.info("..............end..............");

    // SpendNoteTRC20 1
    Optional<TransactionInfo> infoById1 = PublicMethed
        .getTransactionInfoById(txid1, blockingStubFull);
    byte[] tx1Data = infoById1.get().getLog(0).getData().toByteArray();
    long pos1 = bytes32Tolong(ByteArray.subArray(tx1Data, 0, 32));
    byte[] contractResult1 = triggerGetPath(
        blockingStubFull, contractAddress, callerAddress, privateKey, pos1);
    byte[] path1 = ByteArray.subArray(contractResult1, 32, 1056);
    byte[] root1 = ByteArray.subArray(contractResult1, 0, 32);
    logger.info(Hex.toHexString(contractResult1));
    GrpcAPI.SpendNoteTRC20.Builder note1Builder = GrpcAPI.SpendNoteTRC20.newBuilder();
    note1Builder.setAlpha(ByteString.copyFrom(Note.generateR()));
    note1Builder.setPos(pos1);
    note1Builder.setPath(ByteString.copyFrom(path1));
    note1Builder.setRoot(ByteString.copyFrom(root1));
    note1Builder.setNote(mintPrivateParam1.getShieldedReceives(0).getNote());

    // SpendNoteTRC20 2
    Optional<TransactionInfo> infoById2 = PublicMethed
        .getTransactionInfoById(txid2, blockingStubFull);
    byte[] tx2Data = infoById2.get().getLog(0).getData().toByteArray();
    long pos2 = bytes32Tolong(ByteArray.subArray(tx2Data, 0, 32));
    byte[] contractResult2 = triggerGetPath(
        blockingStubFull, contractAddress, callerAddress, privateKey, pos2);
    byte[] path2 = ByteArray.subArray(contractResult2, 32, 1056);
    byte[] root2 = ByteArray.subArray(contractResult2, 0, 32);
    logger.info(Hex.toHexString(contractResult2));
    GrpcAPI.SpendNoteTRC20.Builder note2Builder = GrpcAPI.SpendNoteTRC20.newBuilder();
    note2Builder.setAlpha(ByteString.copyFrom(Note.generateR()));
    note2Builder.setPos(pos2);
    note2Builder.setPath(ByteString.copyFrom(path2));
    note2Builder.setRoot(ByteString.copyFrom(root2));
    note2Builder.setNote(mintPrivateParam2.getShieldedReceives(0).getNote());
    GrpcAPI.PrivateShieldedTRC20Parameters.Builder privateTRC20Builder = GrpcAPI
        .PrivateShieldedTRC20Parameters.newBuilder();
    privateTRC20Builder.addShieldedSpends(note1Builder.build());
    privateTRC20Builder.addShieldedSpends(note2Builder.build());

    //ReceiveNote 1
    GrpcAPI.ReceiveNote.Builder revNoteBuilder = GrpcAPI.ReceiveNote.newBuilder();
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
    long revValue = 31;
    byte[] memo = new byte[512];
    byte[] rcm = Note.generateR();
    String paymentAddressStr = KeyIo.encodePaymentAddress(paymentAddress);
    GrpcAPI.Note revNote = getNote(revValue, paymentAddressStr, rcm, memo);
    revNoteBuilder.setNote(revNote);

    //ReceiveNote 2
    GrpcAPI.ReceiveNote.Builder revNoteBuilder2 = GrpcAPI.ReceiveNote.newBuilder();
    FullViewingKey fullViewingKey2 = sk.fullViewingKey();
    IncomingViewingKey incomingViewingKey2 = fullViewingKey2.inViewingKey();
    PaymentAddress paymentAddress2 = incomingViewingKey2.address(DiversifierT.random()).get();
    long revValue2 = 69;
    byte[] memo2 = new byte[512];
    byte[] rcm2 = Note.generateR();
    String paymentAddressStr2 = KeyIo.encodePaymentAddress(paymentAddress2);
    GrpcAPI.Note revNote2 = getNote(revValue2, paymentAddressStr2, rcm2, memo2);
    revNoteBuilder2.setNote(revNote2);
    privateTRC20Builder.addShieldedReceives(revNoteBuilder.build());
    privateTRC20Builder.addShieldedReceives(revNoteBuilder2.build());
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();

    privateTRC20Builder.setAsk(ByteString.copyFrom(expsk.getAsk()));
    privateTRC20Builder.setNsk(ByteString.copyFrom(expsk.getNsk()));
    privateTRC20Builder.setOvk(ByteString.copyFrom(expsk.getOvk()));
    privateTRC20Builder.setShieldedTRC20ContractAddress(ByteString.copyFrom(contractAddress));
    GrpcAPI.ShieldedTRC20Parameters transferParam = blockingStubFull
        .createShieldedContractParameters(
            privateTRC20Builder.build());
    String txid = triggerTransfer(
        blockingStubFull, contractAddress, callerAddress, privateKey,
        transferParam.getTriggerContractInput());
    logger.info("..............transfer result...........");
    logger.info(txid);
    logger.info("..............end..............");
  }

  @Ignore
  @Test
  public void testCreateShieldedContractParametersForBurn()
      throws ZksnarkException, ContractValidateException {
    librustzcashInitZksnarkParams();
    byte[] contractAddress = WalletClient
        .decodeFromBase58Check(shieldedTRC20ContractAddress);
    byte[] callerAddress = WalletClient.decodeFromBase58Check(pubAddress);
    SpendingKey sk = SpendingKey.decode(privateKey);
    GrpcAPI.PrivateShieldedTRC20Parameters mintPrivateParam1 = mintParams(
        privateKey, 60, shieldedTRC20ContractAddress);
    GrpcAPI.ShieldedTRC20Parameters mintParam1 = blockingStubFull.createShieldedContractParameters(
        mintPrivateParam1);
    long value = 60;
    String mintInput1 = mintParamsToHexString(mintParam1, value);
    String txid1 = triggerMint(
        blockingStubFull, contractAddress, callerAddress, privateKey, mintInput1);
    logger.info("..............result...........");
    logger.info(txid1);
    logger.info("..............end..............");

    // SpendNoteTRC20 1
    Optional<TransactionInfo> infoById1 = PublicMethed
        .getTransactionInfoById(txid1, blockingStubFull);
    byte[] tx1Data = infoById1.get().getLog(0).getData().toByteArray();
    long pos1 = bytes32Tolong(ByteArray.subArray(tx1Data, 0, 32));
    byte[] contractResult1 = triggerGetPath(
        blockingStubFull, contractAddress, callerAddress, privateKey, pos1);
    byte[] path1 = ByteArray.subArray(contractResult1, 32, 1056);
    byte[] root1 = ByteArray.subArray(contractResult1, 0, 32);
    logger.info(Hex.toHexString(contractResult1));
    GrpcAPI.SpendNoteTRC20.Builder note1Builder = GrpcAPI.SpendNoteTRC20.newBuilder();
    note1Builder.setAlpha(ByteString.copyFrom(Note.generateR()));
    note1Builder.setPos(pos1);
    note1Builder.setPath(ByteString.copyFrom(path1));
    note1Builder.setRoot(ByteString.copyFrom(root1));
    note1Builder.setNote(mintPrivateParam1.getShieldedReceives(0).getNote());
    GrpcAPI.PrivateShieldedTRC20Parameters.Builder privateTRC20Builder = GrpcAPI
        .PrivateShieldedTRC20Parameters.newBuilder();
    privateTRC20Builder.addShieldedSpends(note1Builder.build());

    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    privateTRC20Builder.setAsk(ByteString.copyFrom(expsk.getAsk()));
    privateTRC20Builder.setNsk(ByteString.copyFrom(expsk.getNsk()));
    privateTRC20Builder.setToAmount(60);
    privateTRC20Builder.setTransparentToAddress(ByteString.copyFrom(callerAddress));
    privateTRC20Builder.setShieldedTRC20ContractAddress(ByteString.copyFrom(contractAddress));
    GrpcAPI.ShieldedTRC20Parameters burnParam = blockingStubFull
        .createShieldedContractParameters(privateTRC20Builder.build());

    GrpcAPI.PrivateShieldedTRC20Parameters privateTrc20Params = privateTRC20Builder.build();
    logger.info("input parameters:");
    logger.info(Hex.toHexString(expsk.getAsk()));
    logger.info(Hex.toHexString(expsk.getNsk()));
    for (GrpcAPI.SpendNoteTRC20 spend : privateTrc20Params.getShieldedSpendsList()) {
      GrpcAPI.Note note = spend.getNote();
      logger.info(String.valueOf(note.getValue()));
      logger.info(note.getPaymentAddress());
      logger.info(Hex.toHexString(note.getRcm().toByteArray()));
      logger.info(Hex.toHexString(spend.getAlpha().toByteArray()));
      logger.info(Hex.toHexString(spend.getRoot().toByteArray()));
      logger.info(Hex.toHexString(spend.getPath().toByteArray()));
      logger.info(String.valueOf(spend.getPos()));
    }
    for (GrpcAPI.ReceiveNote rNote : privateTrc20Params.getShieldedReceivesList()) {
      GrpcAPI.Note note = rNote.getNote();
      logger.info(String.valueOf(note.getValue()));
      logger.info(note.getPaymentAddress());
      logger.info(Hex.toHexString(note.getRcm().toByteArray()));
    }
    logger.info(String.valueOf(privateTrc20Params.getToAmount()));
    logger.info(Hex.toHexString(privateTrc20Params.getTransparentToAddress().toByteArray()));
    logger.info(Hex.toHexString(privateTrc20Params.getShieldedTRC20ContractAddress()
        .toByteArray()));

    //check the proof
    boolean result;
    //verify spendProof && bindingSignature
    long ctx = JLibrustzcash.librustzcashSaplingVerificationCtxInit();
    ShieldContract.SpendDescription spend = burnParam.getSpendDescription(0);
    try {
      result = JLibrustzcash.librustzcashSaplingCheckSpend(
          new LibrustzcashParam.CheckSpendParams(ctx,
              spend.getValueCommitment().toByteArray(),
              spend.getAnchor().toByteArray(),
              spend.getNullifier().toByteArray(),
              spend.getRk().toByteArray(),
              spend.getZkproof().toByteArray(),
              spend.getSpendAuthoritySignature().toByteArray(),
              burnParam.getMessageHash().toByteArray()));
      long valueBalance = value;
      result &= JLibrustzcash.librustzcashSaplingFinalCheck(
          new LibrustzcashParam.FinalCheckParams(ctx, valueBalance,
              burnParam.getBindingSignature().toByteArray(),
              burnParam.getMessageHash().toByteArray()));
    } catch (Throwable any) {
      result = false;
    } finally {
      JLibrustzcash.librustzcashSaplingVerificationCtxFree(ctx);
    }
    Assert.assertTrue(result);
    String txid2 = triggerBurn(
        blockingStubFull, contractAddress, callerAddress, privateKey,
        burnParam.getTriggerContractInput());
    byte[] nf = burnParam.getSpendDescription(0).getNullifier().toByteArray();
    logger.info("..............burn result...........");
    logger.info(txid2);
    logger.info(Hex.toHexString(nf));
    logger.info("..............end..............");
  }

  @Ignore
  @Test
  public void testCreateShieldedContractParametersForMintWithoutAsk() throws Exception {
    librustzcashInitZksnarkParams();
    byte[] callerAddress = WalletClient.decodeFromBase58Check(pubAddress);
    SpendingKey sk = SpendingKey.random();
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    byte[] ovk = expsk.getOvk();
    //ReceiveNote
    GrpcAPI.ReceiveNote.Builder revNoteBuilder = GrpcAPI.ReceiveNote.newBuilder();
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
    long revValue = 50;
    byte[] memo = new byte[512];
    byte[] rcm = Note.generateR();
    String paymentAddressStr = KeyIo.encodePaymentAddress(paymentAddress);
    GrpcAPI.Note revNote = getNote(revValue, paymentAddressStr, rcm, memo);
    revNoteBuilder.setNote(revNote);

    byte[] contractAddress = WalletClient.decodeFromBase58Check(shieldedTRC20ContractAddress);
    GrpcAPI.PrivateShieldedTRC20ParametersWithoutAsk.Builder paramBuilder = GrpcAPI
        .PrivateShieldedTRC20ParametersWithoutAsk.newBuilder();
    paramBuilder.setOvk(ByteString.copyFrom(ovk));
    paramBuilder.setFromAmount(revValue);
    paramBuilder.addShieldedReceives(revNoteBuilder.build());
    paramBuilder.setShieldedTRC20ContractAddress(ByteString.copyFrom(contractAddress));
    GrpcAPI.ShieldedTRC20Parameters trc20MintParams = blockingStubFull
        .createShieldedContractParametersWithoutAsk(paramBuilder.build());

    GrpcAPI.PrivateShieldedTRC20ParametersWithoutAsk privateParams = paramBuilder.build();
    logger.info("input parameters:");
    logger.info(Hex.toHexString(expsk.getOvk()));
    logger.info(String.valueOf(privateParams.getFromAmount()));
    for (GrpcAPI.SpendNoteTRC20 spend : privateParams.getShieldedSpendsList()) {
      GrpcAPI.Note note = spend.getNote();
      logger.info(String.valueOf(note.getValue()));
      logger.info(note.getPaymentAddress());
      logger.info(Hex.toHexString(note.getRcm().toByteArray()));
      logger.info(Hex.toHexString(spend.getAlpha().toByteArray()));
      logger.info(Hex.toHexString(spend.getRoot().toByteArray()));
      logger.info(Hex.toHexString(spend.getPath().toByteArray()));
      logger.info(String.valueOf(spend.getPos()));
    }
    for (GrpcAPI.ReceiveNote rNote : privateParams.getShieldedReceivesList()) {
      GrpcAPI.Note note = rNote.getNote();
      logger.info(String.valueOf(note.getValue()));
      logger.info(note.getPaymentAddress());
      logger.info(Hex.toHexString(note.getRcm().toByteArray()));
    }
    logger.info(Hex.toHexString(privateParams.getShieldedTRC20ContractAddress().toByteArray()));

    //verify receiveProof && bindingSignature
    boolean result;
    long ctx = JLibrustzcash.librustzcashSaplingVerificationCtxInit();
    ShieldContract.ReceiveDescription revDesc = trc20MintParams.getReceiveDescription(0);
    try {
      result = JLibrustzcash.librustzcashSaplingCheckOutput(
          new LibrustzcashParam.CheckOutputParams(
              ctx,
              revDesc.getValueCommitment().toByteArray(),
              revDesc.getNoteCommitment().toByteArray(),
              revDesc.getEpk().toByteArray(),
              revDesc.getZkproof().toByteArray()));
      long valueBalance = -revValue;
      result &= JLibrustzcash.librustzcashSaplingFinalCheck(
          new LibrustzcashParam.FinalCheckParams(
              ctx,
              valueBalance,
              trc20MintParams.getBindingSignature().toByteArray(),
              trc20MintParams.getMessageHash().toByteArray()));
    } catch (Throwable any) {
      result = false;
    } finally {
      JLibrustzcash.librustzcashSaplingVerificationCtxFree(ctx);
    }
    Assert.assertTrue(result);

    ShieldedTRC20TriggerContractParameters.Builder triggerParam =
        ShieldedTRC20TriggerContractParameters
            .newBuilder();
    triggerParam.setShieldedTRC20Parameters(trc20MintParams);
    triggerParam.setAmount(revValue);
    BytesMessage triggerInput = blockingStubFull
        .getTriggerInputForShieldedTRC20Contract(triggerParam.build());
    Assert.assertArrayEquals(triggerInput.getValue().toByteArray(),
        Hex.decode(trc20MintParams.getTriggerContractInput()));
    String txid = triggerMint(
        blockingStubFull, contractAddress, callerAddress, privateKey,
        Hex.toHexString(triggerInput.getValue().toByteArray()));
    logger.info("..............transfer result...........");
    logger.info(txid);
    logger.info("..............end..............");
  }

  @Ignore
  @Test
  public void testCreateShieldedContractParametersForTransferWithoutAsk1v1() throws Exception {
    byte[] contractAddress = WalletClient
        .decodeFromBase58Check(shieldedTRC20ContractAddress);
    byte[] callerAddress = WalletClient.decodeFromBase58Check(pubAddress);
    SpendingKey sk = SpendingKey.decode(privateKey);
    GrpcAPI.PrivateShieldedTRC20Parameters mintPrivateParam1 = mintParams(
        privateKey, 60, shieldedTRC20ContractAddress);
    GrpcAPI.ShieldedTRC20Parameters mintParam1 = blockingStubFull
        .createShieldedContractParameters(mintPrivateParam1);
    String mintInput1 = mintParamsToHexString(mintParam1, 60);
    String txid1 = triggerMint(
        blockingStubFull, contractAddress, callerAddress, privateKey, mintInput1);
    logger.info("..............result...........");
    logger.info(txid1);
    logger.info("..............end..............");

    // SpendNoteTRC20 1
    Optional<TransactionInfo> infoById1 = PublicMethed
        .getTransactionInfoById(txid1, blockingStubFull);
    byte[] tx1Data = infoById1.get().getLog(0).getData().toByteArray();
    long pos1 = bytes32Tolong(ByteArray.subArray(tx1Data, 0, 32));
    byte[] contractResult1 = triggerGetPath(
        blockingStubFull, contractAddress, callerAddress, privateKey, pos1);
    byte[] path1 = ByteArray.subArray(contractResult1, 32, 1056);
    byte[] root1 = ByteArray.subArray(contractResult1, 0, 32);
    logger.info(Hex.toHexString(contractResult1));
    GrpcAPI.SpendNoteTRC20.Builder note1Builder = GrpcAPI.SpendNoteTRC20.newBuilder();
    note1Builder.setAlpha(ByteString.copyFrom(Note.generateR()));
    note1Builder.setPos(pos1);
    note1Builder.setPath(ByteString.copyFrom(path1));
    note1Builder.setRoot(ByteString.copyFrom(root1));
    note1Builder.setNote(mintPrivateParam1.getShieldedReceives(0).getNote());
    GrpcAPI.PrivateShieldedTRC20ParametersWithoutAsk.Builder privateTRC20Builder = GrpcAPI
        .PrivateShieldedTRC20ParametersWithoutAsk.newBuilder();
    privateTRC20Builder.addShieldedSpends(note1Builder.build());

    //ReceiveNote 1
    GrpcAPI.ReceiveNote.Builder revNoteBuilder = GrpcAPI.ReceiveNote.newBuilder();
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
    long revValue = 60;
    byte[] memo = new byte[512];
    byte[] rcm = Note.generateR();
    String paymentAddressStr = KeyIo.encodePaymentAddress(paymentAddress);
    GrpcAPI.Note revNote = getNote(revValue, paymentAddressStr, rcm, memo);
    revNoteBuilder.setNote(revNote);

    privateTRC20Builder.addShieldedReceives(revNoteBuilder.build());
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    FullViewingKey fvk = sk.fullViewingKey();
    privateTRC20Builder.setAk(ByteString.copyFrom(fvk.getAk()));
    privateTRC20Builder.setNsk(ByteString.copyFrom(expsk.getNsk()));
    privateTRC20Builder.setOvk(ByteString.copyFrom(expsk.getOvk()));
    privateTRC20Builder.setShieldedTRC20ContractAddress(ByteString.copyFrom(contractAddress));
    PrivateShieldedTRC20ParametersWithoutAsk shieldedTRC20ParametersWithoutAsk = privateTRC20Builder
        .build();
    GrpcAPI.ShieldedTRC20Parameters transferParam = blockingStubFull
        .createShieldedContractParametersWithoutAsk(shieldedTRC20ParametersWithoutAsk);

    logger.info(transferParam.toString());

    SpendAuthSigParameters.Builder signParamerters = SpendAuthSigParameters.newBuilder();
    signParamerters.setAlpha(shieldedTRC20ParametersWithoutAsk.getShieldedSpends(0).getAlpha());
    signParamerters.setAsk(ByteString.copyFrom(expsk.getAsk()));
    signParamerters.setTxHash(transferParam.getMessageHash());
    BytesMessage signMsg = blockingStubFull.createSpendAuthSig(signParamerters.build());
    logger.info("...........spend authority signature...........");
    logger.info(String.valueOf(signMsg.getValue().size()));
    logger.info(Hex.toHexString(signMsg.getValue().toByteArray()));
    ShieldedTRC20TriggerContractParameters.Builder triggerParam =
        ShieldedTRC20TriggerContractParameters
            .newBuilder();
    triggerParam.setShieldedTRC20Parameters(transferParam);
    triggerParam.addSpendAuthoritySignature(signMsg);
    BytesMessage triggerInput = blockingStubFull
        .getTriggerInputForShieldedTRC20Contract(triggerParam.build());
    String txid = triggerTransfer(
        blockingStubFull, contractAddress, callerAddress, privateKey,
        Hex.toHexString(triggerInput.getValue().toByteArray()));
    logger.info("..............transfer result...........");
    logger.info(txid);
    logger.info("..............end..............");

  }

  @Ignore
  @Test
  public void testCreateShieldedContractParametersForTransferWithoutAsk1v2() throws Exception {
    byte[] contractAddress = WalletClient
        .decodeFromBase58Check(shieldedTRC20ContractAddress);
    byte[] callerAddress = WalletClient.decodeFromBase58Check(pubAddress);
    SpendingKey sk = SpendingKey.decode(privateKey);
    GrpcAPI.PrivateShieldedTRC20Parameters mintPrivateParam1 = mintParams(
        privateKey, 100, shieldedTRC20ContractAddress);
    GrpcAPI.ShieldedTRC20Parameters mintParam1 = blockingStubFull.createShieldedContractParameters(
        mintPrivateParam1);
    String mintInput1 = mintParamsToHexString(mintParam1, 100);
    String txid1 = triggerMint(blockingStubFull, contractAddress, callerAddress, privateKey,
        mintInput1);
    logger.info("..............result...........");
    logger.info(txid1);
    logger.info("..............end..............");

    // SpendNoteTRC20 1
    Optional<TransactionInfo> infoById1 = PublicMethed
        .getTransactionInfoById(txid1, blockingStubFull);
    byte[] tx1Data = infoById1.get().getLog(0).getData().toByteArray();
    long pos1 = bytes32Tolong(ByteArray.subArray(tx1Data, 0, 32));
    byte[] contractResult1 = triggerGetPath(blockingStubFull, contractAddress, callerAddress,
        privateKey,
        pos1);
    byte[] path1 = ByteArray.subArray(contractResult1, 32, 1056);
    byte[] root1 = ByteArray.subArray(contractResult1, 0, 32);
    logger.info(Hex.toHexString(contractResult1));
    GrpcAPI.SpendNoteTRC20.Builder note1Builder = GrpcAPI.SpendNoteTRC20.newBuilder();
    note1Builder.setAlpha(ByteString.copyFrom(Note.generateR()));
    note1Builder.setPos(pos1);
    note1Builder.setPath(ByteString.copyFrom(path1));
    note1Builder.setRoot(ByteString.copyFrom(root1));
    note1Builder.setNote(mintPrivateParam1.getShieldedReceives(0).getNote());
    GrpcAPI.PrivateShieldedTRC20ParametersWithoutAsk.Builder privateTRC20Builder = GrpcAPI
        .PrivateShieldedTRC20ParametersWithoutAsk.newBuilder();
    privateTRC20Builder.addShieldedSpends(note1Builder.build());

    //ReceiveNote 1
    GrpcAPI.ReceiveNote.Builder revNoteBuilder = GrpcAPI.ReceiveNote.newBuilder();
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
    long revValue = 30;
    byte[] memo = new byte[512];
    byte[] rcm = Note.generateR();
    String paymentAddressStr = KeyIo.encodePaymentAddress(paymentAddress);
    GrpcAPI.Note revNote = getNote(revValue, paymentAddressStr, rcm, memo);
    revNoteBuilder.setNote(revNote);

    //ReceiveNote 2
    GrpcAPI.ReceiveNote.Builder revNoteBuilder2 = GrpcAPI.ReceiveNote.newBuilder();
    FullViewingKey fullViewingKey2 = sk.fullViewingKey();
    IncomingViewingKey incomingViewingKey2 = fullViewingKey2.inViewingKey();
    PaymentAddress paymentAddress2 = incomingViewingKey2.address(DiversifierT.random()).get();
    long revValue2 = 70;
    byte[] memo2 = new byte[512];
    byte[] rcm2 = Note.generateR();
    String paymentAddressStr2 = KeyIo.encodePaymentAddress(paymentAddress2);
    GrpcAPI.Note revNote2 = getNote(revValue2, paymentAddressStr2, rcm2, memo2);
    revNoteBuilder2.setNote(revNote2);

    privateTRC20Builder.addShieldedReceives(revNoteBuilder.build());
    privateTRC20Builder.addShieldedReceives(revNoteBuilder2.build());
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    FullViewingKey fvk = sk.fullViewingKey();
    privateTRC20Builder.setAk(ByteString.copyFrom(fvk.getAk()));
    privateTRC20Builder.setNsk(ByteString.copyFrom(expsk.getNsk()));
    privateTRC20Builder.setOvk(ByteString.copyFrom(expsk.getOvk()));
    privateTRC20Builder.setShieldedTRC20ContractAddress(ByteString.copyFrom(contractAddress));
    GrpcAPI.ShieldedTRC20Parameters transferParam = blockingStubFull
        .createShieldedContractParametersWithoutAsk(privateTRC20Builder.build());
    logger.info(transferParam.toString());

    //trigger the contract
    PrivateShieldedTRC20ParametersWithoutAsk shieldedTRC20ParametersWithoutAsk = privateTRC20Builder
        .build();
    SpendAuthSigParameters.Builder signParamerters = SpendAuthSigParameters.newBuilder();
    signParamerters.setAlpha(shieldedTRC20ParametersWithoutAsk.getShieldedSpends(0).getAlpha());
    signParamerters.setAsk(ByteString.copyFrom(expsk.getAsk()));
    signParamerters.setTxHash(transferParam.getMessageHash());
    BytesMessage signMsg = blockingStubFull.createSpendAuthSig(signParamerters.build());

    ShieldedTRC20TriggerContractParameters.Builder triggerParam =
        ShieldedTRC20TriggerContractParameters
            .newBuilder();
    triggerParam.setShieldedTRC20Parameters(transferParam);
    triggerParam.addSpendAuthoritySignature(signMsg);
    BytesMessage triggerInput = blockingStubFull
        .getTriggerInputForShieldedTRC20Contract(triggerParam.build());
    String txid = triggerTransfer(
        blockingStubFull, contractAddress, callerAddress, privateKey,
        Hex.toHexString(triggerInput.getValue().toByteArray()));
    logger.info("..............transfer result...........");
    logger.info(txid);
    logger.info("..............end..............");
  }

  @Ignore
  @Test
  public void testCreateShieldedContractParametersForTransferWithoutAsk2v1() throws Exception {
    byte[] contractAddress = WalletClient
        .decodeFromBase58Check(shieldedTRC20ContractAddress);
    byte[] callerAddress = WalletClient.decodeFromBase58Check(pubAddress);
    SpendingKey sk = SpendingKey.decode(privateKey);
    GrpcAPI.PrivateShieldedTRC20Parameters mintPrivateParam1 = mintParams(
        privateKey, 60, shieldedTRC20ContractAddress);
    GrpcAPI.ShieldedTRC20Parameters mintParam1 = blockingStubFull.createShieldedContractParameters(
        mintPrivateParam1);
    GrpcAPI.PrivateShieldedTRC20Parameters mintPrivateParam2 = mintParams(
        privateKey, 40, shieldedTRC20ContractAddress);
    GrpcAPI.ShieldedTRC20Parameters mintParam2 = blockingStubFull.createShieldedContractParameters(
        mintPrivateParam2);

    String mintInput1 = mintParamsToHexString(mintParam1, 60);
    String mintInput2 = mintParamsToHexString(mintParam2, 40);
    String txid1 = triggerMint(
        blockingStubFull, contractAddress, callerAddress, privateKey, mintInput1);
    String txid2 = triggerMint(
        blockingStubFull, contractAddress, callerAddress, privateKey, mintInput2);
    logger.info("..............result...........");
    logger.info(txid1);
    logger.info(txid2);
    logger.info("..............end..............");

    // SpendNoteTRC20 1
    Optional<TransactionInfo> infoById1 = PublicMethed
        .getTransactionInfoById(txid1, blockingStubFull);
    byte[] tx1Data = infoById1.get().getLog(0).getData().toByteArray();
    long pos1 = bytes32Tolong(ByteArray.subArray(tx1Data, 0, 32));
    byte[] contractResult1 = triggerGetPath(
        blockingStubFull, contractAddress, callerAddress, privateKey, pos1);
    byte[] path1 = ByteArray.subArray(contractResult1, 32, 1056);
    byte[] root1 = ByteArray.subArray(contractResult1, 0, 32);
    logger.info(Hex.toHexString(contractResult1));
    GrpcAPI.SpendNoteTRC20.Builder note1Builder = GrpcAPI.SpendNoteTRC20.newBuilder();
    note1Builder.setAlpha(ByteString.copyFrom(Note.generateR()));
    note1Builder.setPos(pos1);
    note1Builder.setPath(ByteString.copyFrom(path1));
    note1Builder.setRoot(ByteString.copyFrom(root1));
    note1Builder.setNote(mintPrivateParam1.getShieldedReceives(0).getNote());

    // SpendNoteTRC20 2
    Optional<TransactionInfo> infoById2 = PublicMethed
        .getTransactionInfoById(txid2, blockingStubFull);
    byte[] tx2Data = infoById2.get().getLog(0).getData().toByteArray();
    long pos2 = bytes32Tolong(ByteArray.subArray(tx2Data, 0, 32));
    byte[] contractResult2 = triggerGetPath(
        blockingStubFull, contractAddress, callerAddress, privateKey, pos2);
    byte[] path2 = ByteArray.subArray(contractResult2, 32, 1056);
    byte[] root2 = ByteArray.subArray(contractResult2, 0, 32);
    logger.info(Hex.toHexString(contractResult2));
    GrpcAPI.SpendNoteTRC20.Builder note2Builder = GrpcAPI.SpendNoteTRC20.newBuilder();
    note2Builder.setAlpha(ByteString.copyFrom(Note.generateR()));
    note2Builder.setPos(pos2);
    note2Builder.setPath(ByteString.copyFrom(path2));
    note2Builder.setRoot(ByteString.copyFrom(root2));
    note2Builder.setNote(mintPrivateParam2.getShieldedReceives(0).getNote());
    GrpcAPI.PrivateShieldedTRC20ParametersWithoutAsk.Builder privateTRC20Builder = GrpcAPI
        .PrivateShieldedTRC20ParametersWithoutAsk.newBuilder();
    privateTRC20Builder.addShieldedSpends(note1Builder.build());
    privateTRC20Builder.addShieldedSpends(note2Builder.build());

    //ReceiveNote 1
    GrpcAPI.ReceiveNote.Builder revNoteBuilder = GrpcAPI.ReceiveNote.newBuilder();
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
    long revValue = 100;
    byte[] memo = new byte[512];
    byte[] rcm = Note.generateR();
    String paymentAddressStr = KeyIo.encodePaymentAddress(paymentAddress);
    GrpcAPI.Note revNote = getNote(revValue, paymentAddressStr, rcm, memo);
    revNoteBuilder.setNote(revNote);

    privateTRC20Builder.addShieldedReceives(revNoteBuilder.build());
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    FullViewingKey fvk = sk.fullViewingKey();
    privateTRC20Builder.setAk(ByteString.copyFrom(fvk.getAk()));
    privateTRC20Builder.setNsk(ByteString.copyFrom(expsk.getNsk()));
    privateTRC20Builder.setOvk(ByteString.copyFrom(expsk.getOvk()));
    privateTRC20Builder.setShieldedTRC20ContractAddress(ByteString.copyFrom(contractAddress));
    GrpcAPI.ShieldedTRC20Parameters transferParam = blockingStubFull
        .createShieldedContractParametersWithoutAsk(privateTRC20Builder.build());
    logger.info(transferParam.toString());

    //trigger the contract
    PrivateShieldedTRC20ParametersWithoutAsk shieldedTRC20ParametersWithoutAsk = privateTRC20Builder
        .build();
    SpendAuthSigParameters.Builder signParamerters1 = SpendAuthSigParameters.newBuilder();
    signParamerters1.setAlpha(shieldedTRC20ParametersWithoutAsk.getShieldedSpends(0).getAlpha());
    signParamerters1.setAsk(ByteString.copyFrom(expsk.getAsk()));
    signParamerters1.setTxHash(transferParam.getMessageHash());
    BytesMessage signMsg1 = blockingStubFull.createSpendAuthSig(signParamerters1.build());

    SpendAuthSigParameters.Builder signParamerters2 = SpendAuthSigParameters.newBuilder();
    signParamerters2.setAlpha(shieldedTRC20ParametersWithoutAsk.getShieldedSpends(1).getAlpha());
    signParamerters2.setAsk(ByteString.copyFrom(expsk.getAsk()));
    signParamerters2.setTxHash(transferParam.getMessageHash());
    BytesMessage signMsg2 = blockingStubFull.createSpendAuthSig(signParamerters2.build());

    ShieldedTRC20TriggerContractParameters.Builder triggerParam =
        ShieldedTRC20TriggerContractParameters
            .newBuilder();
    triggerParam.setShieldedTRC20Parameters(transferParam);
    triggerParam.addSpendAuthoritySignature(signMsg1);
    triggerParam.addSpendAuthoritySignature(signMsg2);
    BytesMessage triggerInput = blockingStubFull
        .getTriggerInputForShieldedTRC20Contract(triggerParam.build());
    String txid = triggerTransfer(
        blockingStubFull, contractAddress, callerAddress, privateKey,
        Hex.toHexString(triggerInput.getValue().toByteArray()));
    logger.info("..............transfer result...........");
    logger.info(txid);
    logger.info("..............end..............");
  }

  @Ignore
  @Test
  public void testCreateShieldedContractParametersForTransferWithoutAsk2v2() throws Exception {
    byte[] contractAddress = WalletClient
        .decodeFromBase58Check(shieldedTRC20ContractAddress);
    byte[] callerAddress = WalletClient.decodeFromBase58Check(pubAddress);
    SpendingKey sk = SpendingKey.decode(privateKey);
    //String  privateKey2 = "03d51abbd89cb8196f0efb6892f94d68fccc2c35f0b84609e5f12c55dd85aba8";
    GrpcAPI.PrivateShieldedTRC20Parameters mintPrivateParam1 = mintParams(
        privateKey, 60, shieldedTRC20ContractAddress);
    GrpcAPI.ShieldedTRC20Parameters mintParam1 = blockingStubFull.createShieldedContractParameters(
        mintPrivateParam1);
    GrpcAPI.PrivateShieldedTRC20Parameters mintPrivateParam2 = mintParams(
        privateKey, 40, shieldedTRC20ContractAddress);
    GrpcAPI.ShieldedTRC20Parameters mintParam2 = blockingStubFull.createShieldedContractParameters(
        mintPrivateParam2);

    String mintInput1 = mintParamsToHexString(mintParam1, 60);
    String mintInput2 = mintParamsToHexString(mintParam2, 40);
    String txid1 = triggerMint(
        blockingStubFull, contractAddress, callerAddress, privateKey, mintInput1);
    String txid2 = triggerMint(
        blockingStubFull, contractAddress, callerAddress, privateKey, mintInput2);
    logger.info("..............result...........");
    logger.info(txid1);
    logger.info(txid2);
    logger.info("..............end..............");

    // SpendNoteTRC20 1
    Optional<TransactionInfo> infoById1 = PublicMethed
        .getTransactionInfoById(txid1, blockingStubFull);
    byte[] tx1Data = infoById1.get().getLog(0).getData().toByteArray();
    long pos1 = bytes32Tolong(ByteArray.subArray(tx1Data, 0, 32));
    byte[] contractResult1 = triggerGetPath(
        blockingStubFull, contractAddress, callerAddress, privateKey, pos1);
    byte[] path1 = ByteArray.subArray(contractResult1, 32, 1056);
    byte[] root1 = ByteArray.subArray(contractResult1, 0, 32);
    logger.info(Hex.toHexString(contractResult1));
    GrpcAPI.SpendNoteTRC20.Builder note1Builder = GrpcAPI.SpendNoteTRC20.newBuilder();
    note1Builder.setAlpha(ByteString.copyFrom(Note.generateR()));
    note1Builder.setPos(pos1);
    note1Builder.setPath(ByteString.copyFrom(path1));
    note1Builder.setRoot(ByteString.copyFrom(root1));
    note1Builder.setNote(mintPrivateParam1.getShieldedReceives(0).getNote());

    // SpendNoteTRC20 2
    Optional<TransactionInfo> infoById2 = PublicMethed
        .getTransactionInfoById(txid2, blockingStubFull);
    byte[] tx2Data = infoById2.get().getLog(0).getData().toByteArray();
    long pos2 = bytes32Tolong(ByteArray.subArray(tx2Data, 0, 32));
    byte[] contractResult2 = triggerGetPath(
        blockingStubFull, contractAddress, callerAddress, privateKey, pos2);
    byte[] path2 = ByteArray.subArray(contractResult2, 32, 1056);
    byte[] root2 = ByteArray.subArray(contractResult2, 0, 32);
    logger.info(Hex.toHexString(contractResult2));
    GrpcAPI.SpendNoteTRC20.Builder note2Builder = GrpcAPI.SpendNoteTRC20.newBuilder();
    note2Builder.setAlpha(ByteString.copyFrom(Note.generateR()));
    note2Builder.setPos(pos2);
    note2Builder.setPath(ByteString.copyFrom(path2));
    note2Builder.setRoot(ByteString.copyFrom(root2));
    note2Builder.setNote(mintPrivateParam2.getShieldedReceives(0).getNote());
    GrpcAPI.PrivateShieldedTRC20ParametersWithoutAsk.Builder privateTRC20Builder = GrpcAPI
        .PrivateShieldedTRC20ParametersWithoutAsk.newBuilder();
    privateTRC20Builder.addShieldedSpends(note1Builder.build());
    privateTRC20Builder.addShieldedSpends(note2Builder.build());

    //ReceiveNote 1
    GrpcAPI.ReceiveNote.Builder revNoteBuilder = GrpcAPI.ReceiveNote.newBuilder();
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
    long revValue = 30;
    byte[] memo = new byte[512];
    byte[] rcm = Note.generateR();
    String paymentAddressStr = KeyIo.encodePaymentAddress(paymentAddress);
    GrpcAPI.Note revNote = getNote(revValue, paymentAddressStr, rcm, memo);
    revNoteBuilder.setNote(revNote);

    //ReceiveNote 2
    GrpcAPI.ReceiveNote.Builder revNoteBuilder2 = GrpcAPI.ReceiveNote.newBuilder();
    FullViewingKey fullViewingKey2 = sk.fullViewingKey();
    IncomingViewingKey incomingViewingKey2 = fullViewingKey2.inViewingKey();
    PaymentAddress paymentAddress2 = incomingViewingKey2.address(DiversifierT.random()).get();
    long revValue2 = 70;
    byte[] memo2 = new byte[512];
    byte[] rcm2 = Note.generateR();
    String paymentAddressStr2 = KeyIo.encodePaymentAddress(paymentAddress2);
    GrpcAPI.Note revNote2 = getNote(revValue2, paymentAddressStr2, rcm2, memo2);
    revNoteBuilder2.setNote(revNote2);

    privateTRC20Builder.addShieldedReceives(revNoteBuilder.build());
    privateTRC20Builder.addShieldedReceives(revNoteBuilder2.build());
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    FullViewingKey fvk = sk.fullViewingKey();
    privateTRC20Builder.setAk(ByteString.copyFrom(fvk.getAk()));
    privateTRC20Builder.setNsk(ByteString.copyFrom(expsk.getNsk()));
    privateTRC20Builder.setOvk(ByteString.copyFrom(expsk.getOvk()));
    privateTRC20Builder.setShieldedTRC20ContractAddress(ByteString.copyFrom(contractAddress));

    GrpcAPI.ShieldedTRC20Parameters transferParam = blockingStubFull
        .createShieldedContractParametersWithoutAsk(privateTRC20Builder.build());
    logger.info(transferParam.toString());
    // checkTransferParams(transferParam);
    GrpcAPI.PrivateShieldedTRC20ParametersWithoutAsk privateParams = privateTRC20Builder.build();
    logger.info("input parameters:");
    logger.info(Hex.toHexString(expsk.getNsk()));
    logger.info(Hex.toHexString(fvk.getAk()));
    logger.info(Hex.toHexString(expsk.getOvk()));
    //logger.info(String.valueOf(privateParams.getFromAmount()));
    for (GrpcAPI.SpendNoteTRC20 spend : privateParams.getShieldedSpendsList()) {
      GrpcAPI.Note note = spend.getNote();
      logger.info(String.valueOf(note.getValue()));
      logger.info(note.getPaymentAddress());
      logger.info(Hex.toHexString(note.getRcm().toByteArray()));
      logger.info(Hex.toHexString(spend.getAlpha().toByteArray()));
      logger.info(Hex.toHexString(spend.getRoot().toByteArray()));
      logger.info(Hex.toHexString(spend.getPath().toByteArray()));
      logger.info(String.valueOf(spend.getPos()));
    }
    for (GrpcAPI.ReceiveNote rNote : privateParams.getShieldedReceivesList()) {
      GrpcAPI.Note note = rNote.getNote();
      logger.info(String.valueOf(note.getValue()));
      logger.info(note.getPaymentAddress());
      logger.info(Hex.toHexString(note.getRcm().toByteArray()));
    }
    logger.info(Hex.toHexString(privateParams.getShieldedTRC20ContractAddress().toByteArray()));

    //trigger the contract
    PrivateShieldedTRC20ParametersWithoutAsk shieldedTRC20ParametersWithoutAsk = privateTRC20Builder
        .build();
    SpendAuthSigParameters.Builder signParamerters1 = SpendAuthSigParameters.newBuilder();
    signParamerters1.setAlpha(shieldedTRC20ParametersWithoutAsk.getShieldedSpends(0).getAlpha());
    signParamerters1.setAsk(ByteString.copyFrom(expsk.getAsk()));
    signParamerters1.setTxHash(transferParam.getMessageHash());
    BytesMessage signMsg1 = blockingStubFull.createSpendAuthSig(signParamerters1.build());

    SpendAuthSigParameters.Builder signParamerters2 = SpendAuthSigParameters.newBuilder();
    signParamerters2.setAlpha(shieldedTRC20ParametersWithoutAsk.getShieldedSpends(1).getAlpha());
    signParamerters2.setAsk(ByteString.copyFrom(expsk.getAsk()));
    signParamerters2.setTxHash(transferParam.getMessageHash());
    BytesMessage signMsg2 = blockingStubFull.createSpendAuthSig(signParamerters2.build());

    ShieldedTRC20TriggerContractParameters.Builder triggerParam =
        ShieldedTRC20TriggerContractParameters
            .newBuilder();
    triggerParam.setShieldedTRC20Parameters(transferParam);
    triggerParam.addSpendAuthoritySignature(signMsg1);
    triggerParam.addSpendAuthoritySignature(signMsg2);
    logger.info("print the spend authority signature");
    logger.info(Hex.toHexString(signMsg1.getValue().toByteArray()));
    logger.info(Hex.toHexString(signMsg2.getValue().toByteArray()));

    BytesMessage triggerInput = blockingStubFull
        .getTriggerInputForShieldedTRC20Contract(triggerParam.build());
    String txid = triggerTransfer(
        blockingStubFull, contractAddress, callerAddress, privateKey,
        Hex.toHexString(triggerInput.getValue().toByteArray()));
    logger.info("..............transfer result...........");
    logger.info(txid);
    logger.info("..............end..............");
  }

  @Ignore
  @Test
  public void testCreateShieldedContractParametersForBurnWithoutAsk() throws Exception {
    byte[] contractAddress = WalletClient
        .decodeFromBase58Check(shieldedTRC20ContractAddress);
    byte[] callerAddress = WalletClient.decodeFromBase58Check(pubAddress);
    SpendingKey sk = SpendingKey.decode(privateKey);
    GrpcAPI.PrivateShieldedTRC20Parameters mintPrivateParam1 = mintParams(
        privateKey, 60, shieldedTRC20ContractAddress);
    GrpcAPI.ShieldedTRC20Parameters mintParam1 = blockingStubFull.createShieldedContractParameters(
        mintPrivateParam1);
    long value = 60;
    String mintInput1 = mintParamsToHexString(mintParam1, value);
    String txid1 = triggerMint(
        blockingStubFull, contractAddress, callerAddress, privateKey, mintInput1);
    logger.info("..............result...........");
    logger.info(txid1);
    logger.info("..............end..............");

    Optional<TransactionInfo> infoById1 = PublicMethed
        .getTransactionInfoById(txid1, blockingStubFull);
    byte[] tx1Data = infoById1.get().getLog(0).getData().toByteArray();
    long pos1 = bytes32Tolong(ByteArray.subArray(tx1Data, 0, 32));
    byte[] contractResult1 = triggerGetPath(blockingStubFull, contractAddress, callerAddress,
        privateKey, pos1);
    byte[] path1 = ByteArray.subArray(contractResult1, 32, 1056);
    byte[] root1 = ByteArray.subArray(contractResult1, 0, 32);
    logger.info(Hex.toHexString(contractResult1));
    GrpcAPI.SpendNoteTRC20.Builder note1Builder = GrpcAPI.SpendNoteTRC20.newBuilder();
    note1Builder.setAlpha(ByteString.copyFrom(Note.generateR()));
    note1Builder.setPos(pos1);
    note1Builder.setPath(ByteString.copyFrom(path1));
    note1Builder.setRoot(ByteString.copyFrom(root1));
    note1Builder.setNote(mintPrivateParam1.getShieldedReceives(0).getNote());

    GrpcAPI.PrivateShieldedTRC20ParametersWithoutAsk.Builder privateTRC20Builder = GrpcAPI
        .PrivateShieldedTRC20ParametersWithoutAsk.newBuilder();
    privateTRC20Builder.addShieldedSpends(note1Builder.build());
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    FullViewingKey fvk = expsk.fullViewingKey();
    privateTRC20Builder.setAk(ByteString.copyFrom(fvk.getAk()));
    privateTRC20Builder.setNsk(ByteString.copyFrom(expsk.getNsk()));
    privateTRC20Builder.setToAmount(60);
    privateTRC20Builder.setTransparentToAddress(ByteString.copyFrom(callerAddress));
    privateTRC20Builder.setShieldedTRC20ContractAddress(ByteString.copyFrom(contractAddress));
    GrpcAPI.ShieldedTRC20Parameters transferParam = blockingStubFull
        .createShieldedContractParametersWithoutAsk(privateTRC20Builder.build());
    logger.info(transferParam.toString());

    GrpcAPI.PrivateShieldedTRC20ParametersWithoutAsk privateParams = privateTRC20Builder.build();
    logger.info("input parameters:");
    logger.info(Hex.toHexString(expsk.getNsk()));
    logger.info(Hex.toHexString(fvk.getAk()));
    for (GrpcAPI.SpendNoteTRC20 spend : privateParams.getShieldedSpendsList()) {
      GrpcAPI.Note note = spend.getNote();
      logger.info(String.valueOf(note.getValue()));
      logger.info(note.getPaymentAddress());
      logger.info(Hex.toHexString(note.getRcm().toByteArray()));
      logger.info(Hex.toHexString(spend.getAlpha().toByteArray()));
      logger.info(Hex.toHexString(spend.getRoot().toByteArray()));
      logger.info(Hex.toHexString(spend.getPath().toByteArray()));
      logger.info(String.valueOf(spend.getPos()));
    }
    for (GrpcAPI.ReceiveNote rNote : privateParams.getShieldedReceivesList()) {
      GrpcAPI.Note note = rNote.getNote();
      logger.info(String.valueOf(note.getValue()));
      logger.info(note.getPaymentAddress());
      logger.info(Hex.toHexString(note.getRcm().toByteArray()));
    }
    logger.info(String.valueOf(privateParams.getToAmount()));
    logger.info(Hex.toHexString(privateParams.getTransparentToAddress().toByteArray()));
    logger.info(Hex.toHexString(privateParams.getShieldedTRC20ContractAddress().toByteArray()));

    //trigger the contract
    PrivateShieldedTRC20ParametersWithoutAsk shieldedTRC20ParametersWithoutAsk = privateTRC20Builder
        .build();
    SpendAuthSigParameters.Builder signParamerters = SpendAuthSigParameters.newBuilder();
    signParamerters.setAlpha(shieldedTRC20ParametersWithoutAsk.getShieldedSpends(0).getAlpha());
    signParamerters.setAsk(ByteString.copyFrom(expsk.getAsk()));
    signParamerters.setTxHash(transferParam.getMessageHash());
    BytesMessage signMsg = blockingStubFull.createSpendAuthSig(signParamerters.build());

    ShieldedTRC20TriggerContractParameters.Builder triggerParam =
        ShieldedTRC20TriggerContractParameters
            .newBuilder();
    triggerParam.setShieldedTRC20Parameters(transferParam);
    triggerParam.addSpendAuthoritySignature(signMsg);
    triggerParam.setAmount(value);
    triggerParam.setTransparentToAddress(ByteString.copyFrom(callerAddress));
    BytesMessage triggerInput = blockingStubFull
        .getTriggerInputForShieldedTRC20Contract(triggerParam.build());
    logger.info("print the trigger params");
    logger.info(Hex.toHexString(signMsg.getValue().toByteArray()));
    logger.info(String.valueOf(value));
    logger.info(Hex.toHexString(callerAddress));

    String txid = triggerBurn(
        blockingStubFull, contractAddress, callerAddress, privateKey,
        Hex.toHexString(triggerInput.getValue().toByteArray()));
    logger.info("..............transfer result...........");
    logger.info(txid);
    logger.info("..............end..............");
  }

  @Ignore
  @Test
  public void testScanShieldedTRC20NotesbyIvk() throws ZksnarkException {
    int statNum = 9200;
    int endNum = 9240;
    librustzcashInitZksnarkParams();
    byte[] contractAddress = WalletClient
        .decodeFromBase58Check(shieldedTRC20ContractAddress);
    SpendingKey sk = SpendingKey.decode(privateKey);
    FullViewingKey fvk = sk.fullViewingKey();
    byte[] ivk = fvk.inViewingKey().value;
    GrpcAPI.IvkDecryptTRC20Parameters.Builder paramBuilder = GrpcAPI
        .IvkDecryptTRC20Parameters.newBuilder();
    logger.info(Hex.toHexString(contractAddress));
    logger.info(Hex.toHexString(ivk));
    logger.info(Hex.toHexString(fvk.getAk()));
    logger.info(Hex.toHexString(fvk.getNk()));

    paramBuilder.setAk(ByteString.copyFrom(fvk.getAk()));
    paramBuilder.setNk(ByteString.copyFrom(fvk.getNk()));
    paramBuilder.setIvk(ByteString.copyFrom(ivk));
    paramBuilder.setStartBlockIndex(statNum);
    paramBuilder.setEndBlockIndex(endNum);
    paramBuilder.setShieldedTRC20ContractAddress(ByteString.copyFrom(contractAddress));
    GrpcAPI.DecryptNotesTRC20 scannedNotes = blockingStubFull.scanShieldedTRC20NotesbyIvk(
        paramBuilder.build());
    for (GrpcAPI.DecryptNotesTRC20.NoteTx noteTx : scannedNotes.getNoteTxsList()) {
      logger.info(noteTx.toString());
    }
  }

  @Ignore
  @Test
  public void testscanShieldedTRC20NotesbyOvk() throws ZksnarkException {
    int statNum = 9200;
    int endNum = 9240;
    byte[] contractAddress = WalletClient
        .decodeFromBase58Check(shieldedTRC20ContractAddress);
    SpendingKey sk = SpendingKey.decode(privateKey);
    FullViewingKey fvk = sk.fullViewingKey();
    GrpcAPI.OvkDecryptTRC20Parameters.Builder paramBuilder = GrpcAPI.OvkDecryptTRC20Parameters
        .newBuilder();
    logger.info(Hex.toHexString(contractAddress));
    logger.info(Hex.toHexString(fvk.getOvk()));

    paramBuilder.setOvk(ByteString.copyFrom(fvk.getOvk()));
    paramBuilder.setStartBlockIndex(statNum);
    paramBuilder.setEndBlockIndex(endNum);
    paramBuilder.setShieldedTRC20ContractAddress(ByteString.copyFrom(contractAddress));
    GrpcAPI.DecryptNotesTRC20 scannedNotes = blockingStubFull.scanShieldedTRC20NotesbyOvk(
        paramBuilder.build());
    for (GrpcAPI.DecryptNotesTRC20.NoteTx noteTx : scannedNotes.getNoteTxsList()) {
      logger.info(noteTx.toString());
    }
  }

  @Ignore
  @Test
  public void isShieldedTRC20ContractNoteSpent() throws ZksnarkException {
    int statNum = 9200;
    int endNum = 9240;
    librustzcashInitZksnarkParams();
    byte[] contractAddress = WalletClient
        .decodeFromBase58Check(shieldedTRC20ContractAddress);
    SpendingKey sk = SpendingKey.decode(privateKey);
    FullViewingKey fvk = sk.fullViewingKey();
    byte[] ivk = fvk.inViewingKey().value;
    GrpcAPI.IvkDecryptTRC20Parameters.Builder paramBuilder = GrpcAPI.IvkDecryptTRC20Parameters
        .newBuilder();

    paramBuilder.setAk(ByteString.copyFrom(fvk.getAk()));
    paramBuilder.setNk(ByteString.copyFrom(fvk.getNk()));
    paramBuilder.setIvk(ByteString.copyFrom(ivk));
    paramBuilder.setStartBlockIndex(statNum);
    paramBuilder.setEndBlockIndex(endNum);
    paramBuilder.setShieldedTRC20ContractAddress(ByteString.copyFrom(contractAddress));
    GrpcAPI.DecryptNotesTRC20 scannedNotes = blockingStubFull.scanShieldedTRC20NotesbyIvk(
        paramBuilder.build());
    for (GrpcAPI.DecryptNotesTRC20.NoteTx noteTx : scannedNotes.getNoteTxsList()) {
      logger.info(noteTx.toString());
    }

    GrpcAPI.NfTRC20Parameters.Builder NfBuilfer;
    NfBuilfer = GrpcAPI.NfTRC20Parameters.newBuilder();
    NfBuilfer.setAk(ByteString.copyFrom(fvk.getAk()));
    NfBuilfer.setNk(ByteString.copyFrom(fvk.getNk()));
    NfBuilfer.setPosition(271);
    NfBuilfer.setShieldedTRC20ContractAddress(ByteString.copyFrom(contractAddress));
    NfBuilfer.setNote(scannedNotes.getNoteTxs(0).getNote());
    GrpcAPI.NullifierResult result = blockingStubFull.isShieldedTRC20ContractNoteSpent(
        NfBuilfer.build());
    Assert.assertTrue(result.getIsSpent());

    NfBuilfer = GrpcAPI.NfTRC20Parameters.newBuilder();
    NfBuilfer.setAk(ByteString.copyFrom(fvk.getAk()));
    NfBuilfer.setNk(ByteString.copyFrom(fvk.getNk()));
    NfBuilfer.setPosition(272);
    NfBuilfer.setShieldedTRC20ContractAddress(ByteString.copyFrom(contractAddress));
    NfBuilfer.setNote(scannedNotes.getNoteTxs(1).getNote());
    GrpcAPI.NullifierResult result1 = blockingStubFull.isShieldedTRC20ContractNoteSpent(
        NfBuilfer.build());
    Assert.assertTrue(result1.getIsSpent());
    GrpcAPI.NfTRC20Parameters nfParma = NfBuilfer.build();
    logger.info(String.valueOf(nfParma.getNote().getValue()));
    logger.info(nfParma.getNote().getPaymentAddress());
    logger.info(Hex.toHexString(nfParma.getNote().getRcm().toByteArray()));
    logger.info(Hex.toHexString(nfParma.getNote().getMemo().toByteArray()));
    logger.info(Hex.toHexString(nfParma.getAk().toByteArray()));
    logger.info(Hex.toHexString(nfParma.getNk().toByteArray()));
    logger.info(String.valueOf(nfParma.getPosition()));
    logger.info(Hex.toHexString(nfParma.getShieldedTRC20ContractAddress().toByteArray()));

    NfBuilfer = GrpcAPI.NfTRC20Parameters.newBuilder();
    NfBuilfer.setAk(ByteString.copyFrom(fvk.getAk()));
    NfBuilfer.setNk(ByteString.copyFrom(fvk.getNk()));
    NfBuilfer.setPosition(274);
    NfBuilfer.setShieldedTRC20ContractAddress(ByteString.copyFrom(contractAddress));
    NfBuilfer.setNote(scannedNotes.getNoteTxs(2).getNote());
    GrpcAPI.NullifierResult result2 = blockingStubFull.isShieldedTRC20ContractNoteSpent(
        NfBuilfer.build());
    Assert.assertFalse(result2.getIsSpent());
  }

  @Ignore
  @Test
  public void testTriggerNullifer() throws ZksnarkException, ContractValidateException {
    byte[] contractAddress = WalletClient
        .decodeFromBase58Check(shieldedTRC20ContractAddress);
    byte[] callerAddress = WalletClient.decodeFromBase58Check(pubAddress);
    SpendingKey sk = SpendingKey.decode(privateKey);
    long value = 60;
    GrpcAPI.PrivateShieldedTRC20Parameters mintPrivateParam1 = mintParams(
        privateKey, value, shieldedTRC20ContractAddress);
    GrpcAPI.ShieldedTRC20Parameters mintParam1 = blockingStubFull.createShieldedContractParameters(
        mintPrivateParam1);
    String mintInput1 = mintParamsToHexString(mintParam1, value);
    String txid1 = triggerMint(
        blockingStubFull, contractAddress, callerAddress, privateKey, mintInput1);
    logger.info("..............min result...........");
    logger.info(txid1);
    logger.info("..............end..............");

    // SpendNoteTRC20 1
    Optional<TransactionInfo> infoById1 = PublicMethed
        .getTransactionInfoById(txid1, blockingStubFull);
    byte[] tx1Data = infoById1.get().getLog(0).getData().toByteArray();
    long pos1 = bytes32Tolong(ByteArray.subArray(tx1Data, 0, 32));
    byte[] contractResult1 = triggerGetPath(blockingStubFull, contractAddress, callerAddress,
        privateKey, pos1);
    byte[] path1 = ByteArray.subArray(contractResult1, 32, 1056);
    byte[] root1 = ByteArray.subArray(contractResult1, 0, 32);
    logger.info(Hex.toHexString(contractResult1));
    GrpcAPI.SpendNoteTRC20.Builder note1Builder = GrpcAPI.SpendNoteTRC20.newBuilder();
    note1Builder.setAlpha(ByteString.copyFrom(Note.generateR()));
    note1Builder.setPos(pos1);
    note1Builder.setPath(ByteString.copyFrom(path1));
    note1Builder.setRoot(ByteString.copyFrom(root1));
    note1Builder.setNote(mintPrivateParam1.getShieldedReceives(0).getNote());

    GrpcAPI.PrivateShieldedTRC20Parameters.Builder privateTRC20Builder = GrpcAPI
        .PrivateShieldedTRC20Parameters.newBuilder();
    privateTRC20Builder.addShieldedSpends(note1Builder.build());
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    privateTRC20Builder.setAsk(ByteString.copyFrom(expsk.getAsk()));
    privateTRC20Builder.setNsk(ByteString.copyFrom(expsk.getNsk()));
    privateTRC20Builder.setToAmount(60);
    privateTRC20Builder.setTransparentToAddress(ByteString.copyFrom(callerAddress));
    privateTRC20Builder.setShieldedTRC20ContractAddress(ByteString.copyFrom(contractAddress));
    GrpcAPI.ShieldedTRC20Parameters burnParam = blockingStubFull
        .createShieldedContractParameters(privateTRC20Builder.build());
    String burnInput = burnParamsToHexString(burnParam, value, callerAddress);
    String txid2 = triggerBurn(blockingStubFull, contractAddress, callerAddress, privateKey,
        burnInput);
    byte[] nf = burnParam.getSpendDescription(0).getNullifier().toByteArray();
    logger.info("..............burn result...........");
    logger.info(txid2);
    logger.info(Hex.toHexString(nf));
    logger.info("..............end..............");

    //test nullifer
    String methodSign = "nullifiers(bytes32)";
    byte[] selector = new byte[4];
    System.arraycopy(Hash.sha3(methodSign.getBytes()), 0, selector, 0, 4);
    byte[] input = ByteUtil.merge(selector, nf);
    SmartContractOuterClass.TriggerSmartContract.Builder triggerBuilder = SmartContractOuterClass
        .TriggerSmartContract.newBuilder();
    triggerBuilder.setContractAddress(ByteString.copyFrom(contractAddress));
    triggerBuilder.setData(ByteString.copyFrom(input));
    GrpcAPI.TransactionExtention trxExt2 = blockingStubFull.triggerConstantContract(
        triggerBuilder.build());
    String code = trxExt2.getResult().getCode().toString();
    boolean bool = trxExt2.getResult().getResult();
    List<ByteString> list = trxExt2.getConstantResultList();
    byte[] listBytes = new byte[0];
    for (ByteString bs : list) {
      listBytes = ByteUtil.merge(listBytes, bs.toByteArray());
    }
    logger.info("..............nullifier result...........");
    logger.info(code);
    logger.info(String.valueOf(bool));
    logger.info(Hex.toHexString(listBytes));
    logger.info("..............end..............");
    Assert.assertArrayEquals(nf, listBytes);
  }

  @Ignore
  @Test
  public void testSolidityScanShieldedTRC20NotesbyIvk() throws ZksnarkException {
    int statNum = 9200;
    int endNum = 9240;
    librustzcashInitZksnarkParams();
    byte[] contractAddress = WalletClient
        .decodeFromBase58Check(shieldedTRC20ContractAddress);
    SpendingKey sk = SpendingKey.decode(privateKey);
    FullViewingKey fvk = sk.fullViewingKey();
    byte[] ivk = fvk.inViewingKey().value;
    GrpcAPI.IvkDecryptTRC20Parameters.Builder paramBuilder = GrpcAPI
        .IvkDecryptTRC20Parameters.newBuilder();
    logger.info(Hex.toHexString(contractAddress));
    logger.info(Hex.toHexString(ivk));
    logger.info(Hex.toHexString(fvk.getAk()));
    logger.info(Hex.toHexString(fvk.getNk()));

    paramBuilder.setAk(ByteString.copyFrom(fvk.getAk()));
    paramBuilder.setNk(ByteString.copyFrom(fvk.getNk()));
    paramBuilder.setIvk(ByteString.copyFrom(ivk));
    paramBuilder.setStartBlockIndex(statNum);
    paramBuilder.setEndBlockIndex(endNum);
    paramBuilder.setShieldedTRC20ContractAddress(ByteString.copyFrom(contractAddress));
    GrpcAPI.DecryptNotesTRC20 scannedNotes = blockingStubSolidity.scanShieldedTRC20NotesbyIvk(
        paramBuilder.build());
    for (GrpcAPI.DecryptNotesTRC20.NoteTx noteTx : scannedNotes.getNoteTxsList()) {
      logger.info(noteTx.toString());
    }
  }

  @Ignore
  @Test
  public void testSolidityScanShieldedTRC20NotesbyOvk() throws ZksnarkException {
    int statNum = 9200;
    int endNum = 9240;
    byte[] contractAddress = WalletClient
        .decodeFromBase58Check(shieldedTRC20ContractAddress);
    SpendingKey sk = SpendingKey.decode(privateKey);
    FullViewingKey fvk = sk.fullViewingKey();
    GrpcAPI.OvkDecryptTRC20Parameters.Builder paramBuilder = GrpcAPI.OvkDecryptTRC20Parameters
        .newBuilder();
    logger.info(Hex.toHexString(contractAddress));
    logger.info(Hex.toHexString(fvk.getOvk()));

    paramBuilder.setOvk(ByteString.copyFrom(fvk.getOvk()));
    paramBuilder.setStartBlockIndex(statNum);
    paramBuilder.setEndBlockIndex(endNum);
    paramBuilder.setShieldedTRC20ContractAddress(ByteString.copyFrom(contractAddress));
    GrpcAPI.DecryptNotesTRC20 scannedNotes = blockingStubSolidity.scanShieldedTRC20NotesbyOvk(
        paramBuilder.build());
    for (GrpcAPI.DecryptNotesTRC20.NoteTx noteTx : scannedNotes.getNoteTxsList()) {
      logger.info(noteTx.toString());
    }
  }

  @Ignore
  @Test
  public void testSolidityIsShieldedTRC20ContractNoteSpent() throws ZksnarkException {
    int statNum = 9200;
    int endNum = 9240;
    librustzcashInitZksnarkParams();
    byte[] contractAddress = WalletClient
        .decodeFromBase58Check(shieldedTRC20ContractAddress);
    SpendingKey sk = SpendingKey.decode(privateKey);
    FullViewingKey fvk = sk.fullViewingKey();
    byte[] ivk = fvk.inViewingKey().value;
    GrpcAPI.IvkDecryptTRC20Parameters.Builder paramBuilder = GrpcAPI.IvkDecryptTRC20Parameters
        .newBuilder();

    paramBuilder.setAk(ByteString.copyFrom(fvk.getAk()));
    paramBuilder.setNk(ByteString.copyFrom(fvk.getNk()));
    paramBuilder.setIvk(ByteString.copyFrom(ivk));
    paramBuilder.setStartBlockIndex(statNum);
    paramBuilder.setEndBlockIndex(endNum);
    paramBuilder.setShieldedTRC20ContractAddress(ByteString.copyFrom(contractAddress));
    GrpcAPI.DecryptNotesTRC20 scannedNotes = blockingStubSolidity.scanShieldedTRC20NotesbyIvk(
        paramBuilder.build());
    for (GrpcAPI.DecryptNotesTRC20.NoteTx noteTx : scannedNotes.getNoteTxsList()) {
      logger.info(noteTx.toString());
    }

    GrpcAPI.NfTRC20Parameters.Builder NfBuilfer;
    NfBuilfer = GrpcAPI.NfTRC20Parameters.newBuilder();
    NfBuilfer.setAk(ByteString.copyFrom(fvk.getAk()));
    NfBuilfer.setNk(ByteString.copyFrom(fvk.getNk()));
    NfBuilfer.setPosition(271);
    NfBuilfer.setShieldedTRC20ContractAddress(ByteString.copyFrom(contractAddress));
    NfBuilfer.setNote(scannedNotes.getNoteTxs(0).getNote());
    GrpcAPI.NullifierResult result = blockingStubSolidity.isShieldedTRC20ContractNoteSpent(
        NfBuilfer.build());
    Assert.assertTrue(result.getIsSpent());

    NfBuilfer = GrpcAPI.NfTRC20Parameters.newBuilder();
    NfBuilfer.setAk(ByteString.copyFrom(fvk.getAk()));
    NfBuilfer.setNk(ByteString.copyFrom(fvk.getNk()));
    NfBuilfer.setPosition(272);
    NfBuilfer.setShieldedTRC20ContractAddress(ByteString.copyFrom(contractAddress));
    NfBuilfer.setNote(scannedNotes.getNoteTxs(1).getNote());
    GrpcAPI.NullifierResult result1 = blockingStubSolidity.isShieldedTRC20ContractNoteSpent(
        NfBuilfer.build());
    Assert.assertTrue(result1.getIsSpent());
    GrpcAPI.NfTRC20Parameters nfParma = NfBuilfer.build();
    logger.info(String.valueOf(nfParma.getNote().getValue()));
    logger.info(nfParma.getNote().getPaymentAddress());
    logger.info(Hex.toHexString(nfParma.getNote().getRcm().toByteArray()));
    logger.info(Hex.toHexString(nfParma.getNote().getMemo().toByteArray()));
    logger.info(Hex.toHexString(nfParma.getAk().toByteArray()));
    logger.info(Hex.toHexString(nfParma.getNk().toByteArray()));
    logger.info(String.valueOf(nfParma.getPosition()));
    logger.info(Hex.toHexString(nfParma.getShieldedTRC20ContractAddress().toByteArray()));

    NfBuilfer = GrpcAPI.NfTRC20Parameters.newBuilder();
    NfBuilfer.setAk(ByteString.copyFrom(fvk.getAk()));
    NfBuilfer.setNk(ByteString.copyFrom(fvk.getNk()));
    NfBuilfer.setPosition(274);
    NfBuilfer.setShieldedTRC20ContractAddress(ByteString.copyFrom(contractAddress));
    NfBuilfer.setNote(scannedNotes.getNoteTxs(2).getNote());
    GrpcAPI.NullifierResult result2 = blockingStubSolidity.isShieldedTRC20ContractNoteSpent(
        NfBuilfer.build());
    Assert.assertFalse(result2.getIsSpent());
  }


  private GrpcAPI.Note getNote(long value, String paymentAddress, byte[] rcm, byte[] memo) {
    GrpcAPI.Note.Builder noteBuilder = GrpcAPI.Note.newBuilder();
    noteBuilder.setValue(value);
    noteBuilder.setPaymentAddress(paymentAddress);
    noteBuilder.setRcm(ByteString.copyFrom(rcm));
    noteBuilder.setMemo(ByteString.copyFrom(memo));
    return noteBuilder.build();
  }

  private String mintParamsToHexString(GrpcAPI.ShieldedTRC20Parameters mintParams, long value) {
    byte[] mergedBytes;
    ShieldContract.ReceiveDescription revDesc = mintParams.getReceiveDescription(0);
    mergedBytes = ByteUtil.merge(
        longTo32Bytes(value),
        revDesc.getNoteCommitment().toByteArray(),
        revDesc.getValueCommitment().toByteArray(),
        revDesc.getEpk().toByteArray(),
        revDesc.getZkproof().toByteArray(),
        mintParams.getBindingSignature().toByteArray(),
        revDesc.getCEnc().toByteArray(),
        revDesc.getCOut().toByteArray(),
        new byte[12]
    );
    return Hex.toHexString(mergedBytes);
  }

  private String triggerMint(WalletGrpc.WalletBlockingStub blockingStubFull, byte[] contractAddress,
                             byte[] callerAddress, String privateKey, String input) {
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String txid = PublicMethed.triggerContract(contractAddress,
        "mint(uint64,bytes32[9],bytes32[2],bytes32[21])",
        input,
        true,
        0L, 1000000000L,
        callerAddress, privateKey,
        blockingStubFull);
    // logger.info(txid);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("Trigger energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());
    Assert.assertEquals(
        Transaction.Result.contractResult.SUCCESS, infoById.get().getReceipt().getResult());
    return txid;
  }

  private GrpcAPI.PrivateShieldedTRC20Parameters mintParams(String privKey,
                                                            long value, String contractAddr)
      throws ZksnarkException, ContractValidateException {
    librustzcashInitZksnarkParams();
    long fromAmount = value;
    SpendingKey sk = SpendingKey.decode(privKey);
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    byte[] ask = expsk.getAsk();
    byte[] nsk = expsk.getNsk();
    byte[] ovk = expsk.getOvk();

    // ReceiveNote
    GrpcAPI.ReceiveNote.Builder revNoteBuilder = GrpcAPI.ReceiveNote.newBuilder();
    // SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = sk.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
    byte[] memo = new byte[512];
    byte[] rcm = Note.generateR();
    String paymentAddressStr = KeyIo.encodePaymentAddress(paymentAddress);
    GrpcAPI.Note revNote = getNote(value, paymentAddressStr, rcm, memo);
    revNoteBuilder.setNote(revNote);
    byte[] contractAddress = WalletClient.decodeFromBase58Check(contractAddr);

    GrpcAPI.PrivateShieldedTRC20Parameters.Builder paramBuilder = GrpcAPI
        .PrivateShieldedTRC20Parameters.newBuilder();
    paramBuilder.setAsk(ByteString.copyFrom(ask));
    paramBuilder.setNsk(ByteString.copyFrom(nsk));
    paramBuilder.setOvk(ByteString.copyFrom(ovk));
    paramBuilder.setFromAmount(fromAmount);
    paramBuilder.addShieldedReceives(revNoteBuilder.build());
    paramBuilder.setShieldedTRC20ContractAddress(ByteString.copyFrom(contractAddress));
    return paramBuilder.build();
  }

  private String transferParamsToHexString(GrpcAPI.ShieldedTRC20Parameters params) {
    byte[] input = new byte[0];
    byte[] spendAuthSig = new byte[0];
    byte[] output = new byte[0];
    byte[] c = new byte[0];
    byte[] bindingSig;
    byte[] mergedBytes;
    List<ShieldContract.SpendDescription> spendDescs = params.getSpendDescriptionList();
    for (ShieldContract.SpendDescription spendDesc : spendDescs) {
      input = ByteUtil.merge(input,
          spendDesc.getNullifier().toByteArray(),
          spendDesc.getAnchor().toByteArray(),
          spendDesc.getValueCommitment().toByteArray(),
          spendDesc.getRk().toByteArray(),
          spendDesc.getZkproof().toByteArray()
      );
      spendAuthSig = ByteUtil.merge(
          spendAuthSig, spendDesc.getSpendAuthoritySignature().toByteArray());
    }
    byte[] inputOffsetbytes = longTo32Bytes(192);
    long spendCount = spendDescs.size();
    byte[] spendCountBytes = longTo32Bytes(spendCount);
    byte[] authOffsetBytes = longTo32Bytes(192 + 32 + 320 * spendCount);
    List<ShieldContract.ReceiveDescription> recvDescs = params.getReceiveDescriptionList();
    for (ShieldContract.ReceiveDescription recvDesc : recvDescs) {
      output = ByteUtil.merge(output,
          recvDesc.getNoteCommitment().toByteArray(),
          recvDesc.getValueCommitment().toByteArray(),
          recvDesc.getEpk().toByteArray(),
          recvDesc.getZkproof().toByteArray()
      );
      c = ByteUtil.merge(c,
          recvDesc.getCEnc().toByteArray(),
          recvDesc.getCOut().toByteArray(),
          new byte[12]
      );
    }
    long recvCount = recvDescs.size();
    byte[] recvCountBytes = longTo32Bytes(recvCount);
    byte[] outputOffsetbytes = longTo32Bytes(192 + 32 + 320 * spendCount + 32 + 64 * spendCount);
    byte[] coffsetBytes = longTo32Bytes(192 + 32 + 320 * spendCount + 32 + 64 * spendCount + 32
        + 288 * recvCount);
    bindingSig = params.getBindingSignature().toByteArray();
    mergedBytes = ByteUtil.merge(inputOffsetbytes,
        authOffsetBytes,
        outputOffsetbytes,
        bindingSig,
        coffsetBytes,
        spendCountBytes,
        input,
        spendCountBytes,
        spendAuthSig,
        recvCountBytes,
        output,
        recvCountBytes,
        c
    );
    //logger.info(ByteArray.toHexString(mergedBytes));
    return Hex.toHexString(mergedBytes);
  }

  private String triggerTransfer(WalletGrpc.WalletBlockingStub blockingStubFull,
                                 byte[] contractAddress,
                                 byte[] callerAddress, String privateKey, String input) {
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String txid = PublicMethed.triggerContract(contractAddress,
        "transfer(bytes32[10][],bytes32[2][],bytes32[9][],bytes32[2],bytes32[21][])",
        input,
        true,
        0L, 1000000000L,
        callerAddress, privateKey,
        blockingStubFull);
    //  logger.info(txid);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("Trigger energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());
    Assert.assertEquals(
        Transaction.Result.contractResult.SUCCESS, infoById.get().getReceipt().getResult());
    return txid;
  }

  private byte[] triggerGetPath(WalletGrpc.WalletBlockingStub blockingStubFull,
                                byte[] contractAddress,
                                byte[] callerAddress, String privateKey, long pos) {
    GrpcAPI.TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtention(contractAddress,
            "getPath(uint256)",
            Hex.toHexString(longTo32Bytes(pos)),
            true,
            0L,
            0,
            "0",
            0,
            callerAddress, privateKey, blockingStubFull);
    Assert.assertEquals(0, transactionExtention.getResult().getCodeValue());
    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    Assert.assertEquals(1056, result.length);
    return result;
  }

  private String triggerBurn(WalletGrpc.WalletBlockingStub blockingStubFull, byte[] contractAddress,
                             byte[] callerAddress, String privateKey, String input) {
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String txid = PublicMethed.triggerContract(contractAddress,
        "burn(bytes32[10],bytes32[2],uint64,bytes32[2],address)",
        input,
        true,
        0L, 1000000000L,
        callerAddress, privateKey,
        blockingStubFull);
    // logger.info(txid);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("Trigger energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());
    Assert.assertEquals(
        Transaction.Result.contractResult.SUCCESS, infoById.get().getReceipt().getResult());
    return txid;
  }

  private String burnParamsToHexString(GrpcAPI.ShieldedTRC20Parameters burnParams, long value,
                                       byte[] transparentToAddress) {
    byte[] mergedBytes;
    byte[] payTo = new byte[32];
    System.arraycopy(transparentToAddress, 0, payTo, 11, 21);
    ShieldContract.SpendDescription spendDesc = burnParams.getSpendDescription(0);
    mergedBytes = ByteUtil.merge(
        spendDesc.getNullifier().toByteArray(),
        spendDesc.getAnchor().toByteArray(),
        spendDesc.getValueCommitment().toByteArray(),
        spendDesc.getRk().toByteArray(),
        spendDesc.getZkproof().toByteArray(),
        spendDesc.getSpendAuthoritySignature().toByteArray(),
        longTo32Bytes(value),
        burnParams.getBindingSignature().toByteArray(),
        payTo
    );
    logger.info("merged bytes: " + ByteArray.toHexString(mergedBytes));
    return Hex.toHexString(mergedBytes);
  }

  private byte[] longTo32Bytes(long value) {
    byte[] longBytes = ByteArray.fromLong(value);
    byte[] zeroBytes = new byte[24];
    return ByteUtil.merge(zeroBytes, longBytes);
  }

  private long bytes32Tolong(byte[] value) {
    return ByteArray.toLong(value);
  }

}
