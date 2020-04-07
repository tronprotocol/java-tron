package stest.tron.wallet.onlinestress;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.spongycastle.util.encoders.Hex;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.core.config.args.Args;
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
import org.tron.protos.Protocol;
import org.tron.protos.contract.ShieldContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ShieldedTRC20ContractStress {
  private AtomicLong[] count = new AtomicLong[5];//[trx, trx20, mint, transfer, burn]
  private AtomicLong[] errorCount = new AtomicLong[5];//[trx, trx20, mint, transfer, burn]
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList(
      "fullnode.ip.list").get(0);
  // private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip" +
  //     ".list").get(0);
  private final String testKey1 = Configuration.getByPath("testng.conf")
            .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey1);
  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] toAddress = ecKey1.getAddress();
  // private HttpResponse response;
  private String trc20ContractAddressStr = "TD7xDWSCuFXsqnh1gn7K7SnHgdhQfYGTVn";
  private byte[] trc20ContractAddress = WalletClient
      .decodeFromBase58Check(trc20ContractAddressStr);
  private String shieldedTRC20ContractAddressStr = "TGAmX5AqVUoXCf8MoHxbuhQPmhGfWTnEgA";
  private byte[] shieldedTRC20ContractAddress = WalletClient
            .decodeFromBase58Check(shieldedTRC20ContractAddressStr);
  private String[] errorMessage = {"Mint", "Transfer", "Burn"};
  private final int TRC10_TRC20_NUM = 30;
  private final int SHIELDED_CONTRACT_NUM = 10;

  /**
   * constructor
  */
  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true)
            .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    Args.getInstance().setFullNodeAllowShieldedTransaction(true);
  }

  private String getMintInput() throws ZksnarkException {
    long fromAmount = 50;
    SpendingKey sk = SpendingKey.random();
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    byte[] ovk = expsk.getOvk();
    //ReceiveNote
    GrpcAPI.ReceiveNote.Builder revNoteBuilder = GrpcAPI.ReceiveNote.newBuilder();
    SpendingKey spendingKey = SpendingKey.decode(testKey1);
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
    long revValue = 50;
    byte[] memo = new byte[512];
    byte[] rcm = Note.generateR();
    String paymentAddressStr = KeyIo.encodePaymentAddress(paymentAddress);
    GrpcAPI.Note revNote = getNote(revValue, paymentAddressStr, rcm, memo);
    revNoteBuilder.setNote(revNote);

    GrpcAPI.PrivateShieldedTRC20Parameters.Builder paramBuilder = GrpcAPI
            .PrivateShieldedTRC20Parameters.newBuilder();
    paramBuilder.setOvk(ByteString.copyFrom(ovk));
    paramBuilder.setFromAmount(fromAmount);
    paramBuilder.addShieldedReceives(revNoteBuilder.build());
    paramBuilder.setShieldedTRC20ContractAddress(
            ByteString.copyFrom(shieldedTRC20ContractAddress));

    GrpcAPI.ShieldedTRC20Parameters trc20MintParams = blockingStubFull
            .createShieldedContractParameters(paramBuilder.build());
    return mintParamsToHexString(trc20MintParams, revValue);
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

  private GrpcAPI.Note getNote(long value, String paymentAddress, byte[] rcm, byte[] memo) {
    GrpcAPI.Note.Builder noteBuilder = GrpcAPI.Note.newBuilder();
    noteBuilder.setValue(value);
    noteBuilder.setPaymentAddress(paymentAddress);
    noteBuilder.setRcm(ByteString.copyFrom(rcm));
    noteBuilder.setMemo(ByteString.copyFrom(memo));
    return noteBuilder.build();
  }


  private String triggerMint(WalletGrpc.WalletBlockingStub blockingStubFull,
      byte[] contractAddress,
      byte[] callerAddress, String privateKey, String input) {
    // PublicMethed.waitProduceNextBlock(blockingStubFull);
    String txid = PublicMethed.triggerContract(contractAddress,
        "mint(uint64,bytes32[9],bytes32[2],bytes32[21])",
        input,
        true,
        0L, 1000000000L,
        callerAddress, privateKey,
        blockingStubFull);
    return txid;
  }

  private String triggerMintGetTxID(WalletGrpc.WalletBlockingStub blockingStubFull,
      byte[] contractAddress,
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
    Optional<Protocol.TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    // logger.info("Trigger energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());
    Assert.assertEquals(
        Protocol.Transaction.Result.contractResult.SUCCESS,
        infoById.get().getReceipt().getResult());
    return txid;
  }

  public String getShieldedTRC20TransferInput()
      throws ZksnarkException, ContractValidateException {
    GrpcAPI.PrivateShieldedTRC20Parameters mintPrivateParam1 = mintParams(
        testKey1, 60, shieldedTRC20ContractAddressStr);
    GrpcAPI.ShieldedTRC20Parameters mintParam1 = blockingStubFull.createShieldedContractParameters(
        mintPrivateParam1);
    GrpcAPI.PrivateShieldedTRC20Parameters mintPrivateParam2 = mintParams(
        testKey1, 40, shieldedTRC20ContractAddressStr);
    GrpcAPI.ShieldedTRC20Parameters mintParam2 = blockingStubFull.createShieldedContractParameters(
        mintPrivateParam2);
    String mintInput1 = mintParamsToHexString(mintParam1, 60);
    String mintInput2 = mintParamsToHexString(mintParam2, 40);
    String txid1 = triggerMintGetTxID(
        blockingStubFull, shieldedTRC20ContractAddress, fromAddress, testKey1, mintInput1);
    String txid2 = triggerMintGetTxID(
        blockingStubFull, shieldedTRC20ContractAddress, fromAddress, testKey1, mintInput2);

    // SpendNoteTRC20 1
    Optional<Protocol.TransactionInfo> infoById1 = PublicMethed
        .getTransactionInfoById(txid1, blockingStubFull);
    byte[] tx1Data = infoById1.get().getLog(0).getData().toByteArray();
    long pos1 = bytes32Tolong(ByteArray.subArray(tx1Data, 0, 32));
    String txid3 = triggerGetPath(
        blockingStubFull, shieldedTRC20ContractAddress, fromAddress, testKey1, pos1);
    Optional<Protocol.TransactionInfo> infoById3 = PublicMethed
        .getTransactionInfoById(txid3, blockingStubFull);
    byte[] contractResult1 = infoById3.get().getContractResult(0).toByteArray();
    byte[] path1 = ByteArray.subArray(contractResult1, 32, 1056);
    byte[] root1 = ByteArray.subArray(contractResult1, 0, 32);
    // logger.info(Hex.toHexString(contractResult1));
    GrpcAPI.SpendNoteTRC20.Builder note1Builder = GrpcAPI.SpendNoteTRC20.newBuilder();
    note1Builder.setAlpha(ByteString.copyFrom(Note.generateR()));
    note1Builder.setPos(pos1);
    note1Builder.setPath(ByteString.copyFrom(path1));
    note1Builder.setRoot(ByteString.copyFrom(root1));
    note1Builder.setNote(mintPrivateParam1.getShieldedReceives(0).getNote());

    // SpendNoteTRC20 2
    Optional<Protocol.TransactionInfo> infoById2 = PublicMethed
        .getTransactionInfoById(txid2, blockingStubFull);
    byte[] tx2Data = infoById2.get().getLog(0).getData().toByteArray();
    long pos2 = bytes32Tolong(ByteArray.subArray(tx2Data, 0, 32));
    String txid4 = triggerGetPath(
        blockingStubFull, shieldedTRC20ContractAddress, fromAddress, testKey1, pos2);
    Optional<Protocol.TransactionInfo> infoById4 = PublicMethed
        .getTransactionInfoById(txid4, blockingStubFull);
    byte[] contractResult2 = infoById4.get().getContractResult(0).toByteArray();
    byte[] path2 = ByteArray.subArray(contractResult2, 32, 1056);
    byte[] root2 = ByteArray.subArray(contractResult2, 0, 32);
    // logger.info(Hex.toHexString(contractResult2));
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
    long revValue = 30;
    byte[] memo = new byte[512];
    byte[] rcm = Note.generateR();
    String paymentAddressStr = KeyIo.encodePaymentAddress(paymentAddress);
    GrpcAPI.Note revNote = getNote(revValue, paymentAddressStr, rcm, memo);
    revNoteBuilder.setNote(revNote);

    //ReceiveNote 2
    GrpcAPI.ReceiveNote.Builder revNoteBuilder2 = GrpcAPI.ReceiveNote.newBuilder();
    SpendingKey sk = SpendingKey.decode(testKey1);
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
    privateTRC20Builder
        .setShieldedTRC20ContractAddress(ByteString.copyFrom(shieldedTRC20ContractAddress));
    GrpcAPI.ShieldedTRC20Parameters transferParam = blockingStubFull
        .createShieldedContractParameters(
            privateTRC20Builder.build());
    return transferParamsToHexString(transferParam);
  }

  private GrpcAPI.PrivateShieldedTRC20Parameters mintParams(String privKey,
      long value, String contractAddr)
      throws ZksnarkException, ContractValidateException {
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
    return Hex.toHexString(mergedBytes);
  }


  private String triggerGetPath(WalletGrpc.WalletBlockingStub blockingStubFull,
      byte[] contractAddress, byte[] callerAddress, String privateKey, long pos) {
    String txid = PublicMethed.triggerContract(contractAddress,
        "getPath(uint256)",
        Hex.toHexString(longTo32Bytes(pos)),
        true,
        0L,
        1000000000L,
        callerAddress, privateKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    // logger.info("Trigger energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());
    Assert.assertEquals(
        Protocol.Transaction.Result.contractResult.SUCCESS,
        infoById.get().getReceipt().getResult());
    return txid;
  }

  private String triggerTransfer(WalletGrpc.WalletBlockingStub blockingStubFull,
      byte[] contractAddress,
      byte[] callerAddress, String privateKey, String input) {
    // PublicMethed.waitProduceNextBlock(blockingStubFull);
    String txid = PublicMethed.triggerContract(contractAddress,
        "transfer(bytes32[10][],bytes32[2][],bytes32[9][],bytes32[2],bytes32[21][])",
        input,
        true,
        0L, 1000000000L,
        callerAddress, privateKey,
        blockingStubFull);
    return txid;
  }

  private String getShieldedTRC20BurnInput() throws ZksnarkException, ContractValidateException {
    SpendingKey sk = SpendingKey.decode(testKey1);
    GrpcAPI.PrivateShieldedTRC20Parameters mintPrivateParam1 = mintParams(
        testKey1, 50, shieldedTRC20ContractAddressStr);
    GrpcAPI.ShieldedTRC20Parameters mintParam1 = blockingStubFull.createShieldedContractParameters(
        mintPrivateParam1);
    long value = 50;
    String mintInput1 = mintParamsToHexString(mintParam1, value);
    String txid1 = triggerMintGetTxID(
        blockingStubFull, shieldedTRC20ContractAddress, fromAddress, testKey1, mintInput1);

    // SpendNoteTRC20 1
    Optional<Protocol.TransactionInfo> infoById1 = PublicMethed
        .getTransactionInfoById(txid1, blockingStubFull);
    byte[] tx1Data = infoById1.get().getLog(0).getData().toByteArray();
    long pos1 = bytes32Tolong(ByteArray.subArray(tx1Data, 0, 32));
    String txid3 = triggerGetPath(
        blockingStubFull, shieldedTRC20ContractAddress, fromAddress, testKey1, pos1);
    Optional<Protocol.TransactionInfo> infoById3 = PublicMethed
        .getTransactionInfoById(txid3, blockingStubFull);
    byte[] contractResult1 = infoById3.get().getContractResult(0).toByteArray();
    byte[] path1 = ByteArray.subArray(contractResult1, 32, 1056);
    byte[] root1 = ByteArray.subArray(contractResult1, 0, 32);
    // logger.info(Hex.toHexString(contractResult1));
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
    privateTRC20Builder.setToAmount(50);
    privateTRC20Builder.setTransparentToAddress(ByteString.copyFrom(fromAddress));
    privateTRC20Builder
        .setShieldedTRC20ContractAddress(ByteString.copyFrom(shieldedTRC20ContractAddress));
    GrpcAPI.ShieldedTRC20Parameters burnParam = blockingStubFull
        .createShieldedContractParameters(privateTRC20Builder.build());
    return burnParamsToHexString(burnParam, value, fromAddress);
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
    return Hex.toHexString(mergedBytes);
  }

  @SuppressWarnings("checkstyle:OperatorWrap")
  private String triggerBurn(WalletGrpc.WalletBlockingStub blockingStubFull,
                              byte[] contractAddress, byte[] callerAddress, String privateKey,
                              String input) {
    // PublicMethed.waitProduceNextBlock(blockingStubFull);
    String txid = PublicMethed.triggerContract(contractAddress,
        "burn(bytes32[10],bytes32[2],uint64,bytes32[2],uint256)",
        input,
        true,
        0L, 1000000000L,
        callerAddress, privateKey,
        blockingStubFull);
    return txid;
  }

  public void setAllowance() {
    byte[] shieldedContractAddressPadding = new byte[32];
    System.arraycopy(shieldedTRC20ContractAddress, 0, shieldedContractAddressPadding, 11, 21);
    byte[] valueBytes = longTo32Bytes(100_000_000_000_000L);
    String input = Hex.toHexString(ByteUtil.merge(shieldedContractAddressPadding, valueBytes));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String txid = PublicMethed.triggerContract(trc20ContractAddress,
        "approve(address,uint256)",
        input,
        true,
        0L,
        1000000000L,
        fromAddress,
        testKey1,
        blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(
        Protocol.Transaction.Result.contractResult.SUCCESS,
        infoById.get().getReceipt().getResult());
  }

  private String sendCoinTx() {
    return PublicMethed.sendcoinGetTransactionId(toAddress, 100L, fromAddress, testKey1,
        blockingStubFull);
  }

  private String sendTRC20Tx() {
    byte[] toAddressPadding = new byte[32];
    System.arraycopy(toAddress, 0, toAddressPadding, 11, 21);
    byte[] valueBytes = longTo32Bytes(100L);
    String input = Hex.toHexString(ByteUtil.merge(toAddressPadding, valueBytes));
    // PublicMethed.waitProduceNextBlock(blockingStubFull);
    String txid = PublicMethed.triggerContract(trc20ContractAddress,
        "transfer(address,uint256)",
        input,
        true,
        0L,
        1000000000L,
        fromAddress,
        testKey1,
        blockingStubFull);
    return txid;
  }

  private String sendShieldedTRC20MintTx(String mintInput)
      throws ZksnarkException {
    return triggerMint(blockingStubFull, shieldedTRC20ContractAddress,
        fromAddress, testKey1, mintInput);
  }

  private String sendShieldedTRC20TransferTx(String transferInput) {
    return triggerTransfer(
        blockingStubFull, shieldedTRC20ContractAddress, fromAddress, testKey1, transferInput);
  }

  private String sendShieldedTRC20BurnTx(String burnInput) {
    return triggerBurn(
        blockingStubFull, shieldedTRC20ContractAddress, fromAddress, testKey1, burnInput);
  }

  private boolean executeResult(String txid) {
    Optional<Protocol.TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);

    if (txid == null || !infoById.isPresent()) {
      return false;
    } else {
      return (infoById.get().getReceipt().getResultValue() == 1);
    }
  }

  private boolean sendCointResult(String txid) {
    Optional<Protocol.Transaction> infoById = PublicMethed
        .getTransactionById(txid, blockingStubFull);

    if (txid == null || !infoById.isPresent()) {
      return false;
    } else {
      return (infoById.get().getRet(0).getContractRetValue() == 1);
    }
  }

  @SuppressWarnings("checkstyle:MultipleVariableDeclarations")
  @Test(enabled = false)
  public void testShieldedTRC20Transaction()
      throws ZksnarkException, ContractValidateException {
    String mintInput;
    String transferInput;
    String burnInput;
    String[][] trcTxIds = new String[2][TRC10_TRC20_NUM];
    String[][] shieldedContractTxIds = new String[3][SHIELDED_CONTRACT_NUM];

    setAllowance();
    mintInput = getMintInput();
    transferInput = getShieldedTRC20TransferInput();
    burnInput = getShieldedTRC20BurnInput();

    for (int i = 0; i < 5; i++) {
      count[i] = new AtomicLong();
      errorCount[i] = new AtomicLong();
    }

    int num = 0;
    while (num++ < 1000) {
      for (int j = 0; j < 2; j++) {
        count[j].getAndAdd(TRC10_TRC20_NUM);
      }
      for (int j = 2; j < 5; j++) {
        count[j].getAndAdd(SHIELDED_CONTRACT_NUM);
      }

      for (int j = 0; j < TRC10_TRC20_NUM; j++) {
        trcTxIds[0][j] = sendCoinTx();
        trcTxIds[1][j] = sendTRC20Tx();
      }
      for (int j = 0; j < SHIELDED_CONTRACT_NUM; j++) {
        shieldedContractTxIds[0][j] = sendShieldedTRC20MintTx(mintInput);
        shieldedContractTxIds[1][j] = sendShieldedTRC20TransferTx(transferInput);
        shieldedContractTxIds[2][j] = sendShieldedTRC20BurnTx(burnInput);
      }

      PublicMethed.waitProduceNextBlock(blockingStubFull);

      for (int k = 0; k < TRC10_TRC20_NUM; k++) {
        if (!sendCointResult(trcTxIds[0][k])) {
          logger.info("sendCoinTx error id " + trcTxIds[0][k]);
          errorCount[0].incrementAndGet();
        }
        if (!executeResult(trcTxIds[1][k])) {
          logger.info("sendTRC20Tx error id " + trcTxIds[1][k]);
          errorCount[1].incrementAndGet();
        }
      }

      for (int j = 0; j < 3; j++) {
        for (int k = 0; k < SHIELDED_CONTRACT_NUM; k++) {
          if (!executeResult(shieldedContractTxIds[j][k])) {
            logger.info(errorMessage[j] + " error id " + shieldedContractTxIds[j][k]);
            errorCount[j + 2].incrementAndGet();
          }
        }
      }

      logger.info("..........test number: " + num + ".........");
      if (num % 10 == 0) {
        // logger.info("total transactions: " + num * 90);
        logger.info("total TRX transactions: " + count[0].get());
        logger.info("total error TRX transactions: " + errorCount[0].get());
        logger.info("total TRC20 transactions: " + count[1].get());
        logger.info("total error TRC20 transactions: " + errorCount[1].get());
        logger.info("total mint transactions: " + count[2].get());
        logger.info("total error mint transactions: " + errorCount[2].get());
        logger.info("total transfer transactions: " + count[3].get());
        logger.info("total error transfer transactions: " + errorCount[3].get());
        logger.info("total burn transactions: " + count[4].get());
        logger.info("total error burn transactions: " + errorCount[4].get());
      }
    }
  }

  private byte[] longTo32Bytes(long value) {
    byte[] longBytes = ByteArray.fromLong(value);
    byte[] zeroBytes = new byte[24];
    return ByteUtil.merge(zeroBytes, longBytes);
  }

  private long bytes32Tolong(byte[] value) {
    return ByteArray.toLong(value);
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
