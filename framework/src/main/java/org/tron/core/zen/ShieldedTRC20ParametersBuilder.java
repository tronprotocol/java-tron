package org.tron.core.zen;

import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.spongycastle.util.encoders.Hex;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.ShieldedTRC20Parameters;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.zksnark.JLibrustzcash;
import org.tron.common.zksnark.LibrustzcashParam;
import org.tron.common.zksnark.ZksnarkUtils;
import org.tron.core.Wallet;
import org.tron.core.capsule.ReceiveDescriptionCapsule;
import org.tron.core.capsule.SpendDescriptionCapsule;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.zen.address.DiversifierT;
import org.tron.core.zen.address.ExpandedSpendingKey;
import org.tron.core.zen.address.PaymentAddress;
import org.tron.core.zen.note.Note;
import org.tron.core.zen.note.NoteEncryption;
import org.tron.core.zen.note.OutgoingPlaintext;
import org.tron.protos.contract.ShieldContract;
import org.tron.protos.contract.ShieldContract.ReceiveDescription;


@Slf4j
public class ShieldedTRC20ParametersBuilder {

  private static final int MERKLE_TREE_PATH_LENGTH = 1024; // 32*32
  private static final String MERKLE_TREE_PATH_LENGTH_ERROR = "Merkle tree path format is wrong";

  @Setter
  private List<SpendDescriptionInfo> spends = new ArrayList<>();
  @Setter
  private List<ReceiveDescriptionInfo> receives = new ArrayList<>();
  @Getter
  private ShieldedTRC20Parameters.Builder builder = ShieldedTRC20Parameters.newBuilder();
  @Getter
  private long valueBalance = 0;
  @Getter
  @Setter
  private ShieldedTRC20ParametersType shieldedTRC20ParametersType;
  @Setter
  private byte[] shieldedTRC20Address;
  @Setter
  private BigInteger transparentFromAmount;
  @Setter
  private byte[] transparentToAddress;
  @Setter
  private BigInteger transparentToAmount;
  @Setter
  private byte[] burnCiphertext = new byte[80];

  public ShieldedTRC20ParametersBuilder() {

  }

  public ShieldedTRC20ParametersBuilder(String type) throws ZksnarkException {
    switch (type) {
      case "mint":
        shieldedTRC20ParametersType = ShieldedTRC20ParametersType.MINT;
        break;
      case "transfer":
        shieldedTRC20ParametersType = ShieldedTRC20ParametersType.TRANSFER;
        break;
      case "burn":
        shieldedTRC20ParametersType = ShieldedTRC20ParametersType.BURN;
        break;
      default:
        throw new ZksnarkException("invalid shielded TRC-20 parameters type");
    }
  }

  private byte[] formatPath(byte[] path, long position) throws ZksnarkException {
    if (path.length != MERKLE_TREE_PATH_LENGTH) {
      throw new ZksnarkException(MERKLE_TREE_PATH_LENGTH_ERROR);
    }

    byte[] result = new byte[1065];
    result[0] = 0x20;
    for (int i = 0; i < 32; i++) {
      result[1 + i * 33] = 0x20;
      System.arraycopy(path, i * 32, result, 2 + i * 33, 32);
    }

    byte[] positionBytes = ByteArray.fromLong(position);
    ZksnarkUtils.sort(positionBytes);
    System.arraycopy(positionBytes, 0, result, 1057, 8);
    return result;
  }

  // Note: should call librustzcashSaplingProvingCtxFree in the caller
  private SpendDescriptionCapsule generateSpendProof(SpendDescriptionInfo spend,
      long ctx) throws ZksnarkException {
    byte[] cm = spend.note.cm();
    // check if ak exists
    byte[] ak;
    byte[] nf;
    byte[] nsk;
    byte[] path = formatPath(spend.path, spend.position);

    if (!ArrayUtils.isEmpty(spend.ak)) {
      ak = spend.ak;
      nf = spend.note.nullifier(ak, JLibrustzcash.librustzcashNskToNk(spend.nsk), spend.position);
      nsk = spend.nsk;
    } else {
      ak = spend.expsk.fullViewingKey().getAk();
      nf = spend.note.nullifier(spend.expsk.fullViewingKey(), spend.position);
      nsk = spend.expsk.getNsk();
    }

    if (ByteArray.isEmpty(cm) || ByteArray.isEmpty(nf)) {
      throw new ZksnarkException("Spend is invalid");
    }

    byte[] cv = new byte[32];
    byte[] rk = new byte[32];
    byte[] zkproof = new byte[192];
    if (!JLibrustzcash.librustzcashSaplingSpendProof(
        new LibrustzcashParam.SpendProofParams(ctx,
            ak,
            nsk,
            spend.note.getD().getData(),
            spend.note.getRcm(),
            spend.alpha,
            spend.note.getValue(),
            spend.anchor,
            path,
            cv,
            rk,
            zkproof))) {
      throw new ZksnarkException("Spend proof failed");
    }

    SpendDescriptionCapsule spendDescriptionCapsule = new SpendDescriptionCapsule();
    spendDescriptionCapsule.setValueCommitment(cv);
    spendDescriptionCapsule.setRk(rk);
    spendDescriptionCapsule.setZkproof(zkproof);
    spendDescriptionCapsule.setAnchor(spend.anchor);
    spendDescriptionCapsule.setNullifier(nf);
    return spendDescriptionCapsule;
  }

  // Note: should call librustzcashSaplingProvingCtxFree in the caller
  private ReceiveDescriptionCapsule generateOutputProof(ReceiveDescriptionInfo output, long ctx)
      throws ZksnarkException {
    byte[] cm = output.getNote().cm();
    byte[] pkD = output.getNote().getPkD();
    if (ByteArray.isEmpty(cm) || ByteArray.isEmpty(pkD)) {
      throw new ZksnarkException("Output is invalid");
    }

    Optional<Note.NotePlaintextEncryptionResult> res = output.getNote().encrypt(pkD);
    if (!res.isPresent()) {
      throw new ZksnarkException("Failed to encrypt note");
    }

    Note.NotePlaintextEncryptionResult enc = res.get();
    NoteEncryption encryptor = enc.getNoteEncryption();
    byte[] cv = new byte[32];
    byte[] zkProof = new byte[192];
    if (!JLibrustzcash.librustzcashSaplingOutputProof(
        new LibrustzcashParam.OutputProofParams(ctx,
            encryptor.getEsk(),
            output.getNote().getD().getData(),
            output.getNote().getPkD(),
            output.getNote().getRcm(),
            output.getNote().getValue(),
            cv,
            zkProof))) {
      throw new ZksnarkException("Output proof failed");
    }

    if (ArrayUtils.isEmpty(output.ovk) || output.ovk.length != 32) {
      throw new ZksnarkException("ovk is null or invalid and ovk should be 32 bytes (256 bit)");
    }

    OutgoingPlaintext outPlaintext = new OutgoingPlaintext(output.getNote().getPkD(),
        encryptor.getEsk());
    byte[] cOut = outPlaintext.encrypt(output.ovk,
        cv,
        cm,
        encryptor).getData();

    ReceiveDescriptionCapsule receiveDescriptionCapsule = new ReceiveDescriptionCapsule();
    receiveDescriptionCapsule.setValueCommitment(cv);
    receiveDescriptionCapsule.setNoteCommitment(cm);
    receiveDescriptionCapsule.setEpk(encryptor.getEpk());
    receiveDescriptionCapsule.setCEnc(enc.getEncCiphertext());
    receiveDescriptionCapsule.setZkproof(zkProof);
    receiveDescriptionCapsule.setCOut(cOut);

    return receiveDescriptionCapsule;
  }

  private void createSpendAuth(byte[] dataToBeSigned) throws ZksnarkException {
    for (int i = 0; i < spends.size(); i++) {
      byte[] result = new byte[64];
      JLibrustzcash.librustzcashSaplingSpendSig(
          new LibrustzcashParam.SpendSigParams(spends.get(i).expsk.getAsk(),
              spends.get(i).alpha,
              dataToBeSigned,
              result));
      builder.getSpendDescriptionBuilder(i)
          .setSpendAuthoritySignature(ByteString.copyFrom(result));
    }
  }

  private byte[] encodeSpendDescriptionWithoutSpendAuthSig(
      ShieldContract.SpendDescription spendDescription) {
    return ByteUtil.merge(spendDescription.getNullifier().toByteArray(),
        spendDescription.getAnchor().toByteArray(),
        spendDescription.getValueCommitment().toByteArray(),
        spendDescription.getRk().toByteArray(),
        spendDescription.getZkproof().toByteArray());
  }

  private byte[] encodeReceiveDescriptionWithoutC(
      ShieldContract.ReceiveDescription receiveDescription) {
    return ByteUtil.merge(receiveDescription.getNoteCommitment().toByteArray(),
        receiveDescription.getValueCommitment().toByteArray(),
        receiveDescription.getEpk().toByteArray(),
        receiveDescription.getZkproof().toByteArray());
  }

  private byte[] encodeCencCout(ShieldContract.ReceiveDescription receiveDescription) {
    byte[] padding = new byte[12];
    return ByteUtil.merge(receiveDescription.getCEnc().toByteArray(),
        receiveDescription.getCOut().toByteArray(),
        padding);
  }

  public ShieldedTRC20Parameters build(boolean withAsk) throws ZksnarkException {
    // Empty output script
    byte[] mergedBytes;
    byte[] dataHashToBeSigned; //256
    BigInteger value = BigInteger.ZERO;
    ShieldContract.SpendDescription spendDescription;
    ShieldContract.ReceiveDescription receiveDescription;
    ShieldedTRC20Parameters shieldedTRC20Parameters;

    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();
    try {
      switch (shieldedTRC20ParametersType) {
        case MINT:
          ReceiveDescriptionInfo receive = receives.get(0);
          receiveDescription = generateOutputProof(receive, ctx).getInstance();
          builder.addReceiveDescription(receiveDescription);

          mergedBytes = ByteUtil.merge(shieldedTRC20Address,
              ByteArray.fromLong(receive.getNote().getValue()),
              encodeReceiveDescriptionWithoutC(receiveDescription),
              encodeCencCout(receiveDescription));
          value = transparentFromAmount;
          builder.setParameterType("mint");
          break;
        case TRANSFER:
          // Create SpendDescriptions
          mergedBytes = shieldedTRC20Address;
          for (SpendDescriptionInfo spend : spends) {
            spendDescription = generateSpendProof(spend, ctx).getInstance();
            builder.addSpendDescription(spendDescription);
            mergedBytes = ByteUtil.merge(mergedBytes,
                encodeSpendDescriptionWithoutSpendAuthSig(spendDescription));
          }

          // Create OutputDescriptions
          byte[] cencCout = new byte[0];
          for (ReceiveDescriptionInfo receiveD : receives) {
            receiveDescription = generateOutputProof(receiveD, ctx).getInstance();
            builder.addReceiveDescription(receiveDescription);
            mergedBytes = ByteUtil.merge(mergedBytes,
                encodeReceiveDescriptionWithoutC(receiveDescription));
            cencCout = ByteUtil.merge(cencCout, encodeCencCout(receiveDescription));
          }
          mergedBytes = ByteUtil.merge(mergedBytes, cencCout);
          builder.setParameterType("transfer");
          break;
        case BURN:
          SpendDescriptionInfo spend = spends.get(0);
          spendDescription = generateSpendProof(spend, ctx).getInstance();
          builder.addSpendDescription(spendDescription);
          mergedBytes = ByteUtil.merge(shieldedTRC20Address,
              encodeSpendDescriptionWithoutSpendAuthSig(spendDescription));
          if (receives.size() == 1) {
            receiveDescription = generateOutputProof(receives.get(0), ctx).getInstance();
            builder.addReceiveDescription(receiveDescription);
            mergedBytes = ByteUtil
                .merge(mergedBytes, encodeReceiveDescriptionWithoutC(receiveDescription),
                    encodeCencCout(receiveDescription));
          }
          mergedBytes = ByteUtil
              .merge(mergedBytes, transparentToAddress, ByteArray.fromLong(valueBalance));
          value = transparentToAmount;
          builder.setParameterType("burn");
          break;
        default:
          throw new ZksnarkException("unknown parameters type");
      }

      dataHashToBeSigned = Sha256Hash.of(true, mergedBytes).getBytes();
      if (dataHashToBeSigned == null) {
        throw new ZksnarkException("calculate transaction hash failed");
      }

      if (withAsk) {
        createSpendAuth(dataHashToBeSigned);
      }
      builder.setMessageHash(ByteString.copyFrom(dataHashToBeSigned));

      byte[] bindingSig = new byte[64];
      JLibrustzcash.librustzcashSaplingBindingSig(
          new LibrustzcashParam.BindingSigParams(ctx,
              valueBalance,
              dataHashToBeSigned,
              bindingSig)
      );
      builder.setBindingSignature(ByteString.copyFrom(bindingSig));
    } catch (Exception e) {
      throw new ZksnarkException("build the shielded TRC-20 parameters error: " + e.getMessage());
    } finally {
      JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
    }

    if (withAsk || shieldedTRC20ParametersType == ShieldedTRC20ParametersType.MINT) {
      shieldedTRC20Parameters = builder.build();
      builder.setTriggerContractInput(
          getTriggerContractInput(shieldedTRC20Parameters, null, value, true,
              transparentToAddress));
    }

    if (!withAsk && shieldedTRC20ParametersType == ShieldedTRC20ParametersType.BURN) {
      builder.setTriggerContractInput(Hex.toHexString(burnCiphertext));
    }
    return builder.build();
  }

  public String getTriggerContractInput(ShieldedTRC20Parameters shieldedTRC20Parameters,
      List<BytesMessage> spendAuthoritySignature,
      BigInteger value, boolean withAsk,
      byte[] transparentToAddress) {
    switch (shieldedTRC20ParametersType) {
      case MINT:
        return mintParamsToHexString(shieldedTRC20Parameters, value);
      case TRANSFER:
        return transferParamsToHexString(shieldedTRC20Parameters, spendAuthoritySignature, withAsk);
      case BURN:
        return burnParamsToHexString(shieldedTRC20Parameters, spendAuthoritySignature, value,
            transparentToAddress, withAsk);
      default:
        return null;
    }
  }

  private String mintParamsToHexString(GrpcAPI.ShieldedTRC20Parameters mintParams,
      BigInteger value) {
    if (value.compareTo(BigInteger.ZERO) <= 0) {
      throw new IllegalArgumentException("require the value be positive");
    }

    ShieldContract.ReceiveDescription revDesc = mintParams.getReceiveDescription(0);
    byte[] zeros = new byte[12];
    byte[] mergedBytes = ByteUtil.merge(
        ByteUtil.bigIntegerToBytes(value, 32),
        revDesc.getNoteCommitment().toByteArray(),
        revDesc.getValueCommitment().toByteArray(),
        revDesc.getEpk().toByteArray(),
        revDesc.getZkproof().toByteArray(),
        mintParams.getBindingSignature().toByteArray(),
        revDesc.getCEnc().toByteArray(),
        revDesc.getCOut().toByteArray(),
        zeros
    );
    return Hex.toHexString(mergedBytes);
  }

  private String transferParamsToHexString(GrpcAPI.ShieldedTRC20Parameters transferParams,
      List<BytesMessage> spendAuthoritySignature,
      boolean withAsk) {
    byte[] input = new byte[0];
    byte[] spendAuthSig = new byte[0];
    byte[] output = new byte[0];
    byte[] c = new byte[0];
    byte[] bindingSig;
    List<ShieldContract.SpendDescription> spendDescs = transferParams.getSpendDescriptionList();
    for (ShieldContract.SpendDescription spendDesc : spendDescs) {
      input = ByteUtil.merge(input,
          spendDesc.getNullifier().toByteArray(),
          spendDesc.getAnchor().toByteArray(),
          spendDesc.getValueCommitment().toByteArray(),
          spendDesc.getRk().toByteArray(),
          spendDesc.getZkproof().toByteArray()
      );
      if (withAsk) {
        spendAuthSig = ByteUtil.merge(
            spendAuthSig, spendDesc.getSpendAuthoritySignature().toByteArray());
      }
    }
    long spendCount = spendDescs.size();
    if (spendCount < 1 || spendCount > 2) {
      throw new IllegalArgumentException("invalid transfer input number");
    }
    if (!withAsk) {
      if (spendCount == 1) {
        spendAuthSig = spendAuthoritySignature.get(0).getValue().toByteArray();
      } else {
        spendAuthSig = ByteUtil.merge(spendAuthoritySignature.get(0).getValue().toByteArray(),
            spendAuthoritySignature.get(1).getValue().toByteArray());
      }
    }

    byte[] inputOffsetbytes = ByteUtil.longTo32Bytes(192);
    byte[] spendCountBytes = ByteUtil.longTo32Bytes(spendCount);
    byte[] authOffsetBytes = ByteUtil.longTo32Bytes(192 + 32 + 320 * spendCount);

    List<ShieldContract.ReceiveDescription> recvDescs = transferParams.getReceiveDescriptionList();
    for (ShieldContract.ReceiveDescription recvDesc : recvDescs) {
      output = ByteUtil.merge(output,
          recvDesc.getNoteCommitment().toByteArray(),
          recvDesc.getValueCommitment().toByteArray(),
          recvDesc.getEpk().toByteArray(),
          recvDesc.getZkproof().toByteArray()
      );
      byte[] zeros = new byte[12];
      c = ByteUtil.merge(c,
          recvDesc.getCEnc().toByteArray(),
          recvDesc.getCOut().toByteArray(),
          zeros
      );
    }

    long recvCount = recvDescs.size();
    byte[] recvCountBytes = ByteUtil.longTo32Bytes(recvCount);
    byte[] outputOffsetbytes = ByteUtil
        .longTo32Bytes(192 + 32 + 320 * spendCount + 32 + 64 * spendCount);
    byte[] coffsetBytes = ByteUtil
        .longTo32Bytes(192 + 32 + 320 * spendCount + 32 + 64 * spendCount + 32
            + 288 * recvCount);
    bindingSig = transferParams.getBindingSignature().toByteArray();

    return Hex.toHexString(ByteUtil.merge(
        inputOffsetbytes,
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
    ));
  }

  private String burnParamsToHexString(GrpcAPI.ShieldedTRC20Parameters burnParams,
      List<BytesMessage> spendAuthoritySignature,
      BigInteger value, byte[] transparentToAddress,
      boolean withAsk) {
    byte[] payTo = new byte[32];
    if (value.compareTo(BigInteger.ZERO) <= 0) {
      throw new IllegalArgumentException("the value must be positive");
    }

    if (ArrayUtils.isEmpty(transparentToAddress)) {
      throw new IllegalArgumentException("the transparent payTo address is null");
    }

    payTo[11] = Wallet.getAddressPreFixByte();
    System.arraycopy(transparentToAddress, 0, payTo, 12, 20);
    ShieldContract.SpendDescription spendDesc = burnParams.getSpendDescription(0);

    byte[] spendAuthSign;
    if (withAsk) {
      spendAuthSign = spendDesc.getSpendAuthoritySignature().toByteArray();
    } else {
      spendAuthSign = spendAuthoritySignature.get(0).getValue().toByteArray();
    }

    byte[] mergedBytes;
    byte[] zeros = new byte[16];
    mergedBytes = ByteUtil.merge(
        spendDesc.getNullifier().toByteArray(),
        spendDesc.getAnchor().toByteArray(),
        spendDesc.getValueCommitment().toByteArray(),
        spendDesc.getRk().toByteArray(),
        spendDesc.getZkproof().toByteArray(),
        spendAuthSign,
        ByteUtil.bigIntegerToBytes(value, 32),
        burnParams.getBindingSignature().toByteArray(),
        payTo,
        burnCiphertext,
        zeros
    );

    byte[] outputOffsetBytes; // 32
    byte[] coffsetBytes; // 32
    byte[] outputCountBytes; // 32
    byte[] countBytes; // 32
    if (burnParams.getReceiveDescriptionList().size() == 0) {
      outputOffsetBytes = ByteUtil.longTo32Bytes(mergedBytes.length + 32L * 2);
      outputCountBytes = ByteUtil.longTo32Bytes(0L);
      coffsetBytes = ByteUtil.longTo32Bytes(mergedBytes.length + 32L * 3);
      countBytes = ByteUtil.longTo32Bytes(0L);
      mergedBytes = ByteUtil
          .merge(mergedBytes, outputOffsetBytes, coffsetBytes, outputCountBytes, countBytes);
    } else {
      outputOffsetBytes = ByteUtil.longTo32Bytes(mergedBytes.length + 32L * 2);
      outputCountBytes = ByteUtil.longTo32Bytes(1L);
      coffsetBytes = ByteUtil.longTo32Bytes(mergedBytes.length + 32 * 3 + 32L * 9);
      countBytes = ByteUtil.longTo32Bytes(1L);
      ReceiveDescription recvDesc = burnParams.getReceiveDescription(0);
      zeros = new byte[12];
      mergedBytes = ByteUtil
          .merge(mergedBytes,
              outputOffsetBytes,
              coffsetBytes,
              outputCountBytes,
              recvDesc.getNoteCommitment().toByteArray(),
              recvDesc.getValueCommitment().toByteArray(),
              recvDesc.getEpk().toByteArray(),
              recvDesc.getZkproof().toByteArray(),
              countBytes,
              recvDesc.getCEnc().toByteArray(),
              recvDesc.getCOut().toByteArray(),
              zeros);
    }
    return Hex.toHexString(mergedBytes);
  }

  public void addSpend(
      ExpandedSpendingKey expsk,
      Note note,
      byte[] anchor,
      byte[] path,
      long position) throws ZksnarkException {
    spends.add(new SpendDescriptionInfo(expsk, note, anchor, path, position));
    valueBalance += note.getValue();
  }

  public void addSpend(
      ExpandedSpendingKey expsk,
      Note note,
      byte[] alpha,
      byte[] anchor,
      byte[] path,
      long position) {
    spends.add(new SpendDescriptionInfo(expsk, note, alpha, anchor, path, position));
    valueBalance += note.getValue();
  }

  public void addSpend(
      byte[] ak,
      byte[] nsk,
      Note note,
      byte[] alpha,
      byte[] anchor,
      byte[] path,
      long position) {
    spends.add(new SpendDescriptionInfo(ak, nsk, note, alpha, anchor, path, position));
    valueBalance += note.getValue();
  }

  public void addOutput(byte[] ovk, PaymentAddress to, long value, byte[] memo)
      throws ZksnarkException {
    Note note = new Note(to, value);
    note.setMemo(memo);
    receives.add(new ReceiveDescriptionInfo(ovk, note));
    valueBalance -= value;
  }

  public void addOutput(byte[] ovk, DiversifierT d, byte[] pkD, long value, byte[] r, byte[] memo) {
    Note note = new Note(d, pkD, value, r);
    note.setMemo(memo);
    receives.add(new ReceiveDescriptionInfo(ovk, note));
    valueBalance -= value;
  }

  public static class SpendDescriptionInfo {

    private ExpandedSpendingKey expsk;
    private Note note;
    private byte[] alpha;
    private byte[] anchor;
    private byte[] path;
    private long position;
    private byte[] ak;
    private byte[] nsk;

    private SpendDescriptionInfo(
        ExpandedSpendingKey expsk,
        Note note,
        byte[] anchor,
        byte[] path,
        long position) throws ZksnarkException {
      this.expsk = expsk;
      this.note = note;
      this.anchor = anchor;
      this.path = path;
      this.position = position;
      alpha = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(alpha);
    }

    private SpendDescriptionInfo(
        ExpandedSpendingKey expsk,
        Note note,
        byte[] alpha,
        byte[] anchor,
        byte[] path,
        long position) {
      this.expsk = expsk;
      this.note = note;
      this.anchor = anchor;
      this.path = path;
      this.position = position;
      this.alpha = alpha;
    }

    private SpendDescriptionInfo(
        byte[] ak,
        byte[] nsk,
        Note note,
        byte[] alpha,
        byte[] anchor,
        byte[] path,
        long position) {
      this.ak = ak;
      this.nsk = nsk;
      this.note = note;
      this.anchor = anchor;
      this.path = path;
      this.position = position;
      this.alpha = alpha;
    }
  }

  @AllArgsConstructor
  private class ReceiveDescriptionInfo {

    private byte[] ovk;
    @Getter
    private Note note;
  }

  public enum ShieldedTRC20ParametersType {
    MINT,
    TRANSFER,
    BURN
  }
}
