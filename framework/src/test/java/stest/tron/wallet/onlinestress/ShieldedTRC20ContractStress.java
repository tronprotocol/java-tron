package stest.tron.wallet.onlinestress;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
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
import org.tron.common.zksnark.JLibrustzcash;
import org.tron.common.zksnark.LibrustzcashParam;
import org.tron.core.Wallet;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.zen.address.*;
import org.tron.core.zen.note.Note;
import org.tron.protos.Protocol;
import org.tron.protos.contract.ShieldContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.PublicMethed;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.tron.core.zksnark.LibrustzcashTest.librustzcashInitZksnarkParams;

@Slf4j
public class ShieldedTRC20ContractStress {
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
            .get(0);
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
            .get(0);
  private final String testKey1 = Configuration.getByPath("testng.conf")
            .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey1);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] toAddress = ecKey1.getAddress();
  Long amount = 2048000000L;
  private HttpResponse response;
  String TRC20ContractAddress = "TG3Tj3hwXMgE33zWugDTh2NWshRexU4QtN";
  String shieldedTRC20ContractAddressStr = "TX29caJFwDPZ9tzjuQ1GB6Ci59ocxQThKN";
  byte[] shieldedTRC20ContractAddress = WalletClient
            .decodeFromBase58Check(shieldedTRC20ContractAddressStr);


  /**
   *  constructor
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
      long from_amount = 50;
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
      paramBuilder.setFromAmount(from_amount);
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


  private boolean triggerMint(WalletGrpc.WalletBlockingStub blockingStubFull, byte[] contractAddress,
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
     logger.info("Trigger energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());
     return infoById.get().getReceipt().getResult()
                ==  infoById.get().getReceipt().getResult();
  }

  private String triggerMintGetTxID(WalletGrpc.WalletBlockingStub blockingStubFull, byte[] contractAddress,
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
        logger.info("Trigger energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());
        Assert.assertEquals(
                Protocol.Transaction.Result.contractResult.SUCCESS, infoById.get().getReceipt().getResult());
        return txid;
  }


  public String getShieldedTRC20TransferInput()
            throws ZksnarkException, ContractValidateException {
    SpendingKey sk = SpendingKey.decode(testKey1);
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
    logger.info(Hex.toHexString(contractResult1));
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
    privateTRC20Builder.setShieldedTRC20ContractAddress(ByteString.copyFrom(shieldedTRC20ContractAddress));
    //GrpcAPI.ShieldedTRC20Parameters transferParam = wallet.createShieldedContractParameters(
    // privateTRC20Builder.build());
    GrpcAPI.ShieldedTRC20Parameters transferParam = blockingStubFull.createShieldedContractParameters(
                privateTRC20Builder.build());
    return transferParamsToHexString(transferParam);
  }

    private GrpcAPI.PrivateShieldedTRC20Parameters mintParams(String privKey,
                                                              long value, String contractAddr)
            throws ZksnarkException, ContractValidateException {
        librustzcashInitZksnarkParams();
        long from_amount = value;
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
        long revValue = value;
        byte[] memo = new byte[512];
        byte[] rcm = Note.generateR();
        String paymentAddressStr = KeyIo.encodePaymentAddress(paymentAddress);
        GrpcAPI.Note revNote = getNote(revValue, paymentAddressStr, rcm, memo);
        revNoteBuilder.setNote(revNote);
        byte[] contractAddress = WalletClient.decodeFromBase58Check(contractAddr);

        GrpcAPI.PrivateShieldedTRC20Parameters.Builder paramBuilder = GrpcAPI
                .PrivateShieldedTRC20Parameters.newBuilder();
        paramBuilder.setAsk(ByteString.copyFrom(ask));
        paramBuilder.setNsk(ByteString.copyFrom(nsk));
        paramBuilder.setOvk(ByteString.copyFrom(ovk));
        paramBuilder.setFromAmount(from_amount);
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


  private String triggerGetPath(WalletGrpc.WalletBlockingStub blockingStubFull, byte[] contractAddress,
                                  byte[] callerAddress, String privateKey, long pos) {
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
        logger.info("Trigger energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());
        Assert.assertEquals(
                Protocol.Transaction.Result.contractResult.SUCCESS, infoById.get().getReceipt().getResult());
        return txid;
  }

  private boolean triggerTransfer(WalletGrpc.WalletBlockingStub blockingStubFull, byte[] contractAddress,
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
        Optional<Protocol.TransactionInfo> infoById = PublicMethed
                .getTransactionInfoById(txid, blockingStubFull);
        logger.info("Trigger energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());
        return  infoById.get().getReceipt().getResult()
                ==  Protocol.Transaction.Result.contractResult.SUCCESS;

  }

  private String getShieldedTRC20BurnInput() throws ZksnarkException, ContractValidateException {
      SpendingKey sk = SpendingKey.decode(testKey1);
      GrpcAPI.PrivateShieldedTRC20Parameters mintPrivateParam1 = mintParams(
              testKey1, 60, shieldedTRC20ContractAddressStr);
      GrpcAPI.ShieldedTRC20Parameters mintParam1 = blockingStubFull.createShieldedContractParameters(
              mintPrivateParam1);
      long value = 60;
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
      privateTRC20Builder.setTransparentToAddress(ByteString.copyFrom(fromAddress));
      privateTRC20Builder.setShieldedTRC20ContractAddress(ByteString.copyFrom(shieldedTRC20ContractAddress));
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
        logger.info("merged bytes: " + ByteArray.toHexString(mergedBytes));
        return Hex.toHexString(mergedBytes);
    }

  private boolean triggerBurn(WalletGrpc.WalletBlockingStub blockingStubFull, byte[] contractAddress,
                               byte[] callerAddress, String privateKey, String input) {
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        String txid = PublicMethed.triggerContract(contractAddress,
                "burn(bytes32[10],bytes32[2],uint64,bytes32[2],uint256)",
                input,
                true,
                0L, 1000000000L,
                callerAddress, privateKey,
                blockingStubFull);
        // logger.info(txid);
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        Optional<Protocol.TransactionInfo> infoById = PublicMethed
                .getTransactionInfoById(txid, blockingStubFull);
        logger.info("Trigger energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());
        return infoById.get().getReceipt().getResult() ==
                Protocol.Transaction.Result.contractResult.SUCCESS;
  }

  private boolean sendTRXTx() {
        response = HttpMethed
                .sendCoin(httpnode, fromAddress, toAddress, amount, testKey1);
        return HttpMethed.verificationResult(response);
    }

  private boolean sendTRC20Tx() {
        byte[] contractAddress = WalletClient
                .decodeFromBase58Check(TRC20ContractAddress);
        byte[] toAddressPadding = new byte[32];
        System.arraycopy(toAddress, 0, toAddressPadding, 11, 21);
        byte[] valueBytes = longTo32Bytes(100_000L);
        String input = Hex.toHexString(ByteUtil.merge(toAddressPadding, valueBytes));
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        String txid = PublicMethed.triggerContract(contractAddress,
                "transfer(address,uint256)",
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
        //Assert.assertEquals(
        //Protocol.Transaction.Result.contractResult.SUCCESS, infoById.get().getReceipt().getResult());
        return  infoById.get().getReceipt().getResult()
                == Protocol.Transaction.Result.contractResult.SUCCESS;
    }

  private boolean sendShieldedTRC20MintTx(String mintInput)
            throws ZksnarkException {
    return triggerMint(blockingStubFull, shieldedTRC20ContractAddress,
                fromAddress, testKey1, mintInput);
  }

  private boolean sendShieldedTRC20TransferTx(String transferInput) {
    return triggerTransfer(
              blockingStubFull, shieldedTRC20ContractAddress, fromAddress, testKey1, transferInput);
  }

  private boolean sendShieldedTRC20BurnTx(String burnInput) {
      return triggerBurn(
              blockingStubFull, shieldedTRC20ContractAddress, fromAddress, testKey1, burnInput);
  }

  @Test(enabled = true)
  public void test1ShieldedTRC20Transaction() throws InterruptedException, ZksnarkException, ContractValidateException {
    int total_num = 1000;
    boolean bool;
    String mintInput,transferInput, burnInput;
    int trxTrue = 0;
    int trxFalse = 0;
    int trc20True = 0;
    int trc20False = 0;
    int mintTrue = 0;
    int mintFalse = 0;
    int transferTrue = 0;
    int transferFalse = 0;
    int burnTrue = 0;
    int burnFalse = 0;

    for (int i = 0; i < total_num; i++) {
      for (int j = 0; j < 4; j++) {
        if (sendTRXTx())
          trxTrue++;
        else
          trxFalse ++;
        if (sendTRC20Tx())
          trc20True++;
        else
           trc20False++;
      }
      mintInput = getMintInput();
      if (sendShieldedTRC20MintTx(mintInput))
        mintTrue++;
      else
        mintFalse++;
      transferInput = getShieldedTRC20TransferInput();
      if (sendShieldedTRC20TransferTx(transferInput)) {
        mintTrue += 2;
        transferTrue++;
      } else
        transferFalse++;

      burnInput = getShieldedTRC20BurnInput();
      if (sendShieldedTRC20MintTx(burnInput)) {
        mintTrue++;
        burnTrue++;
      } else
        burnFalse ++;

      if (i % 100 == 0) {
        logger.info("total transactions: ", i * 14);
        logger.info("total true TRX transactions: ", trxTrue);
        logger.info("total false TRX transactions: ", trxFalse);
        logger.info("total true TRC20 transactions: ", trc20True);
        logger.info("total false TRC20 transactions: ", trc20False);
        logger.info("total true ShieldedTRC20 transactions: ", mintTrue + transferTrue + burnTrue);
        logger.info("total false ShieldedTRC20 transactions: ", mintFalse + transferFalse + mintFalse);
        logger.info(".................................................");
        logger.info("total true mint transactions: ", mintTrue);
        logger.info("total false mint transactions: ", mintFalse);
        logger.info("total true transfer transactions: ", transferTrue);
        logger.info("total false transfer transactions: ", transferFalse);
        logger.info("total true burn transactions: ", burnTrue);
        logger.info("total false burn transactions: ", burnFalse);
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
