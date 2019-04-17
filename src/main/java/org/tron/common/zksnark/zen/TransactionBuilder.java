package org.tron.common.zksnark.zen;

import com.google.protobuf.ByteString;
import com.sun.jna.Pointer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.utils.ByteArray;
import org.tron.common.zksnark.merkle.IncrementalMerkleVoucherContainer;
import org.tron.common.zksnark.zen.address.ExpandedSpendingKey;
import org.tron.common.zksnark.zen.address.PaymentAddress;
import org.tron.common.zksnark.zen.note.BaseNote.Note;
import org.tron.common.zksnark.zen.note.BaseNotePlaintext.NotePlaintext;
import org.tron.common.zksnark.zen.note.BaseNotePlaintext.SaplingNotePlaintextEncryptionResult;
import org.tron.common.zksnark.zen.note.SaplingNoteEncryption;
import org.tron.common.zksnark.zen.note.SaplingOutgoingPlaintext;
import org.tron.common.zksnark.zen.transaction.ReceiveDescriptionCapsule;
import org.tron.common.zksnark.zen.transaction.SpendDescriptionCapsule;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract.ShieldedTransferContract;

public class TransactionBuilder {

  //  CKeyStore keystore;
  //  CMutableTransaction mtx;

  @Setter
  @Getter
  private String from;
  @Setter
  @Getter
  private List<SpendDescriptionInfo> spends;
  @Setter
  @Getter
  private List<ReceiveDescriptionInfo> receives;

  private ShieldedTransferContract.Builder contractBuilder = ShieldedTransferContract.newBuilder();

  //  List<TransparentInputInfo> tIns;

  //  Optional<pair<byte[], PaymentAddress>> zChangeAddr;

  //  Optional<CTxDestination> tChangeAddr;

  // Throws if the anchor does not match the anchor used by
  // previously-added Sapling spends.
  public void addSaplingSpend(
      ExpandedSpendingKey expsk,
      Note note,
      byte[] anchor,
      IncrementalMerkleVoucherContainer voucher) {
    spends.add(new SpendDescriptionInfo(expsk, note, anchor, voucher));
    contractBuilder.setValueBalance(contractBuilder.getValueBalance() + note.value);
  }

  public void addSaplingOutput(byte[] ovk, PaymentAddress to, long value, byte[] memo) {
    receives.add(new ReceiveDescriptionInfo(ovk, new Note(to, value), memo));
    contractBuilder.setValueBalance(contractBuilder.getValueBalance() - value);
  }

  // TODO
  public void setTransparentInput(String address, long value) {
    setTransparentInput(address.getBytes(), value);
  }
  public void setTransparentInput(byte[] address, long value) {
    contractBuilder.setTransparentFromAddress(ByteString.copyFrom(address))
        .setFromAmount(value);
  }

  // TODO
  public void setTransparentOutput(String address, long value) {
    setTransparentOutput(address.getBytes(), value);
  }

  public void setTransparentOutput(byte[] address, long value) {
    contractBuilder.setTransparentToAddress(ByteString.copyFrom(address))
        .setToAmount(value);
  }

  //
  //  void SendChangeTo(PaymentAddress changeAddr, byte[] ovk);
  //
  //  void SendChangeTo(CTxDestination&changeAddr);

  public TransactionBuilderResult Build() {

    //
    // Sapling spends and outputs
    //

    long change = contractBuilder.getValueBalance();
    change += contractBuilder.getFromAmount();
    change -= contractBuilder.getToAmount();

    if (change < 0) {
      // TODO
      throw new RuntimeException("change cannot be negative");
    }

    if (change > 0) {
      // TODO
    }

    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();

    // Create Sapling SpendDescriptions
    for (SpendDescriptionInfo spend : spends) {
      SpendDescriptionCapsule spendDescriptionCapsule = generateSpendProof(spend, ctx);
      contractBuilder.addSpendDescription(spendDescriptionCapsule.getInstance());
    }

    // Create Sapling OutputDescriptions
    for (ReceiveDescriptionInfo receive : receives) {
      ReceiveDescriptionCapsule receiveDescriptionCapsule = generateOutputProof(receive, ctx);
      contractBuilder.addReceiveDescription(receiveDescriptionCapsule.getInstance());
    }

    // Empty output script.
    byte[] dataToBeSigned;//256 
    try {
//      dataToBeSigned = SignatureHash(scriptCode, mtx, NOT_AN_INPUT, SIGHASH_ALL, 0,
////          consensusBranchId);
      dataToBeSigned = null;
    } catch (Exception ex) {
      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Could not construct signature hash: " + ex.getMessage());
    }

    // Create Sapling spendAuth and binding signatures
    for (SpendDescriptionInfo spend : spends) {
      byte[] result = null;
      Librustzcash.librustzcashSaplingSpendSig(
          spend.expsk.getAsk(),
          spend.alpha,
          dataToBeSigned,
          result);
    }

    long valueBalance = 0;
    byte[] bindingSig = null;
    Librustzcash.librustzcashSaplingBindingSig(
        ctx,
        valueBalance,
        dataToBeSigned,
        bindingSig
    );

    Librustzcash.librustzcashSaplingProvingCtxFree(ctx);

    return new TransactionBuilderResult(contractBuilder.build());
  }

  public SpendDescriptionCapsule generateSpendProof(SpendDescriptionInfo spend,
      Pointer ctx) {

    byte[] cm = spend.note.cm();
    byte[] nf = spend.note.nullifier(spend.expsk.fullViewingKey(), spend.voucher.position());

    if (ByteArray.isEmpty(cm) || ByteArray.isEmpty(nf)) {
      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Spend is invalid");
    }

    byte[] voucherPath = spend.voucher.path().encode();

    byte[] cv = new byte[32];
    byte[] rk = new byte[32];
    byte[] zkproof = new byte[32];
    if (!Librustzcash.librustzcashSaplingSpendProof(
        ctx,
        spend.expsk.fullViewingKey().getAk(),
        spend.expsk.getNsk(),
        spend.note.d.getData(),
        spend.note.r,
        spend.alpha,
        spend.note.value,
        spend.anchor,
        voucherPath,
        cv,
        rk,
        zkproof)) {
      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Spend proof failed");
    }
    SpendDescriptionCapsule spendDescriptionCapsule = new SpendDescriptionCapsule();
    spendDescriptionCapsule.setValueCommitment(cv);
    spendDescriptionCapsule.setRk(rk);
    spendDescriptionCapsule.setZkproof(zkproof);
    spendDescriptionCapsule.setAnchor(spend.anchor);
    spendDescriptionCapsule.setNullifier(nf);
    return spendDescriptionCapsule;
  }

  public ReceiveDescriptionCapsule generateOutputProof(ReceiveDescriptionInfo output, Pointer ctx) {
    byte[] cm = output.getNote().cm();
    if (ByteArray.isEmpty(cm)) {
      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Output is invalid");
    }

    NotePlaintext notePlaintext = new NotePlaintext(output.getNote(), output.getMemo());

    Optional<SaplingNotePlaintextEncryptionResult> res = notePlaintext
        .encrypt(output.getNote().pkD);
    if (!res.isPresent()) {
      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Failed to encrypt note");
    }

    SaplingNotePlaintextEncryptionResult enc = res.get();
    SaplingNoteEncryption encryptor = enc.noteEncryption;

    byte[] cv = new byte[32];
    byte[] zkproof = new byte[32];
    if (!Librustzcash.librustzcashSaplingOutputProof(
        ctx,
        encryptor.esk,
        output.getNote().d.getData(),
        output.getNote().pkD,
        output.getNote().r,
        output.getNote().value,
        cv,
        zkproof)) {
      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Ourtput proof failed");
    }

    ReceiveDescriptionCapsule receiveDescriptionCapsule = new ReceiveDescriptionCapsule();
    receiveDescriptionCapsule.setValueCommitment(cv);
    receiveDescriptionCapsule.setNoteCommitment(cm);
    receiveDescriptionCapsule.setEpk(encryptor.epk);
    receiveDescriptionCapsule.setCEnc(enc.encCiphertext);
    receiveDescriptionCapsule.setZkproof(zkproof);

    SaplingOutgoingPlaintext outPlaintext =
        new SaplingOutgoingPlaintext(output.getNote().pkD, encryptor.esk);
    receiveDescriptionCapsule.setCOut(outPlaintext
        .encrypt(output.ovk, receiveDescriptionCapsule.getValueCommitment().toByteArray(),
            receiveDescriptionCapsule.getCm().toByteArray(),
            encryptor).data);
    return receiveDescriptionCapsule;
  }

  public static class SpendDescriptionInfo {

    public ExpandedSpendingKey expsk;

    public Note note;
    public byte[] alpha;
    public byte[] anchor;
    public IncrementalMerkleVoucherContainer voucher;

    public SpendDescriptionInfo(
        ExpandedSpendingKey expsk,
        Note note,
        byte[] anchor,
        IncrementalMerkleVoucherContainer voucher) {
      this.expsk = expsk;
      this.note = note;
      this.anchor = anchor;
      this.voucher = voucher;
    }
  }

  @AllArgsConstructor
  public class ReceiveDescriptionInfo {

    @Getter
    private byte[] ovk;
    @Getter
    private Note note;
    @Getter
    private byte[] memo; // 256
  }

  @AllArgsConstructor
  public static class TransactionBuilderResult {
    @Getter
    private ShieldedTransferContract shieldedTransferContract;
  }
}
