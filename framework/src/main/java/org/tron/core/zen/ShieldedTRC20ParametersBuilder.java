package org.tron.core.zen;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.api.GrpcAPI.ShieldedTRC20Parameters;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.zksnark.JLibrustzcash;
import org.tron.common.zksnark.LibrustzcashParam;
import org.tron.common.zksnark.ZksnarkUtils;
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

@Slf4j
public class ShieldedTRC20ParametersBuilder {

  private static final int MERKLE_TREE_PATH_LENGTH = 1024; // 32*32
  private static final String MERKLE_TREE_PATH_LENGTH_ERROR = "Merkle tree path format is wrong";
  @Setter
  @Getter
  private List<SpendDescriptionInfo> spends = new ArrayList<>();
  @Setter
  @Getter
  private List<ReceiveDescriptionInfo> receives = new ArrayList<>();
  @Getter
  private ShieldedTRC20Parameters.Builder builder = ShieldedTRC20Parameters.newBuilder();
  @Getter
  private long valueBalance = 0;
  @Setter
  private ShieldedTRC20ParametersType shieldedTRC20ParametersType;
  @Setter
  private byte[] shieldedTRC20Address;
  @Setter
  private long transparentFromAmount;
  @Setter
  private byte[] transparentToAddress;
  @Setter
  private long transparentToAmount;

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
    //check if ak exists
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
    if (ByteArray.isEmpty(cm)) {
      throw new ZksnarkException("Output is invalid");
    }
    Optional<Note.NotePlaintextEncryptionResult> res = output.getNote()
        .encrypt(output.getNote().getPkD());
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
    ReceiveDescriptionCapsule receiveDescriptionCapsule = new ReceiveDescriptionCapsule();
    receiveDescriptionCapsule.setValueCommitment(cv);
    receiveDescriptionCapsule.setNoteCommitment(cm);
    receiveDescriptionCapsule.setEpk(encryptor.getEpk());
    receiveDescriptionCapsule.setCEnc(enc.getEncCiphertext());
    receiveDescriptionCapsule.setZkproof(zkProof);
    OutgoingPlaintext outPlaintext =
        new OutgoingPlaintext(output.getNote().getPkD(), encryptor.getEsk());
    receiveDescriptionCapsule.setCOut(outPlaintext
        .encrypt(output.ovk, receiveDescriptionCapsule.getValueCommitment().toByteArray(),
            receiveDescriptionCapsule.getCm().toByteArray(),
            encryptor).getData());
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
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();
    // Empty output script
    byte[] mergedBytes;
    byte[] dataHashToBeSigned; //256
    ShieldContract.SpendDescription spendDescription;
    ShieldContract.ReceiveDescription receiveDescription;

    switch (shieldedTRC20ParametersType) {
      case MINT:
        ReceiveDescriptionInfo receive = receives.get(0);
        receiveDescription = generateOutputProof(receive, ctx).getInstance();
        builder.addReceiveDescription(receiveDescription);
        mergedBytes = ByteUtil.merge(shieldedTRC20Address, ByteArray
                .fromLong(transparentFromAmount),
            encodeReceiveDescriptionWithoutC(receiveDescription),
            encodeCencCout(receiveDescription));
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
        break;
      case BURN:
        SpendDescriptionInfo spend = spends.get(0);
        spendDescription = generateSpendProof(spend, ctx).getInstance();
        builder.addSpendDescription(spendDescription);
        mergedBytes = ByteUtil.merge(shieldedTRC20Address,
            encodeSpendDescriptionWithoutSpendAuthSig(spendDescription),
            transparentToAddress, ByteArray.fromLong(transparentToAmount));
        break;
      default:
        mergedBytes = null;
    }
    dataHashToBeSigned = Sha256Hash.of(true, mergedBytes).getBytes();
    if (dataHashToBeSigned == null) {
      throw new ZksnarkException("cal transaction hash failed");
    }
    if (withAsk) {
      createSpendAuth(dataHashToBeSigned);
    }
    builder.setMessageHash(ByteString.copyFrom(dataHashToBeSigned));
    try {
      byte[] bindingSig = new byte[64];
      JLibrustzcash.librustzcashSaplingBindingSig(
          new LibrustzcashParam.BindingSigParams(ctx,
              valueBalance,
              dataHashToBeSigned,
              bindingSig)
      );
      builder.setBindingSignature(ByteString.copyFrom(bindingSig));
    } catch (ZksnarkException e) {
      throw e;
    } finally {
      JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
    }
    return builder.build();
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
      byte[] ovk,
      Note note,
      byte[] alpha,
      byte[] anchor,
      byte[] path,
      long position) {
    spends.add(new SpendDescriptionInfo(ak, nsk, ovk, note, alpha, anchor, path, position));
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

    @Getter
    @Setter
    private ExpandedSpendingKey expsk;
    @Getter
    @Setter
    private Note note;
    @Getter
    @Setter
    private byte[] alpha;
    @Getter
    @Setter
    private byte[] anchor;
    @Getter
    @Setter
    private byte[] path;
    @Getter
    @Setter
    private long position;
    @Getter
    @Setter
    private byte[] ak;
    @Getter
    @Setter
    private byte[] nsk;
    @Getter
    @Setter
    private byte[] ovk;

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
        byte[] ovk,
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
      this.ovk = ovk;
    }
  }

  @AllArgsConstructor
  private class ReceiveDescriptionInfo {

    @Getter
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
