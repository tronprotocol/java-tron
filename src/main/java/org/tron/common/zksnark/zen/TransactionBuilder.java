package org.tron.common.zksnark.zen;

import com.sun.jna.Pointer;
import java.util.List;
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
import org.tron.common.zksnark.zen.transaction.MutableTransactionCapsule;
import org.tron.common.zksnark.zen.transaction.ReceiveDescriptionCapsule;
import org.tron.common.zksnark.zen.transaction.SpendDescriptionCapsule;
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
  private List<ReceiveDescriptionInfo> outputs;

  private MutableTransactionCapsule mutableTransactionCapsule;
  //  List<TransparentInputInfo> tIns;

  //  Optional<pair<byte[], PaymentAddress>> zChangeAddr;

  //  Optional<CTxDestination> tChangeAddr;

  // Throws if the anchor does not match the anchor used by
  // previously-added Sapling spends.
  public void AddNoteSpend(
      ExpandedSpendingKey expsk,
      Note note,
      byte[] anchor,
      IncrementalMerkleVoucherContainer voucher) {
    spends.add(new SpendDescriptionInfo(expsk, note, anchor, voucher));
    //    mtx.valueBalance += note.value;
    mutableTransactionCapsule.getAndAddBalance(note.value);
  }

  public void addOutputs(byte[] ovk, PaymentAddress to, long value, byte[] memo) {
    outputs.add(new ReceiveDescriptionInfo(ovk, new Note(to, value), memo));
    mutableTransactionCapsule.getAndAddBalance(-value);
  }

  public void AddTransparentInput(String address, long value) {
  }

  public void AddTransparentOutput(String address, long value) {
  }

  public void AddSaplingOutput(byte[] ovk, PaymentAddress to, long value, byte[] memo) {
    //    {
    //      Note note = new Note(to, value);
    //      outputs.add(new OutputDescriptionInfo(ovk, note, memo));
    //      mtx.valueBalance -= value;
  }

  // Assumes that the value correctly corresponds to the provided UTXO.
  //  void AddTransparentInput(COutPoint utxo, CScript scriptPubKey, CAmount value);
  //
  //  void AddTransparentOutput(CTxDestination&to, CAmount value);
  //
  //  void SendChangeTo(PaymentAddress changeAddr, byte[] ovk);
  //
  //  void SendChangeTo(CTxDestination&changeAddr);

  public TransactionBuilderResult Build() {
    ShieldedTransferContract.Builder zkBuilder = ShieldedTransferContract.newBuilder();

    //
    // Sapling spends and outputs
    //

    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();

    // Create Sapling SpendDescriptions
    for (SpendDescriptionInfo spend : spends) {
      SpendDescriptionCapsule sdesc = generateSpendProof(spend, ctx);
      // todo: add sdesc into tx
      //      mtx.vShieldedSpend.push_back(sdesc);
      mutableTransactionCapsule.getSpends().add(sdesc);
    }

    // Create Sapling OutputDescriptions
    for (ReceiveDescriptionInfo output : outputs) {
      ReceiveDescriptionCapsule rdesc = generateOutputProof(output, ctx);
      mutableTransactionCapsule.getReceives().add(rdesc);
    }

    // Empty output script.
    byte[] dataToBeSigned;//256 
    try {
//      dataToBeSigned = SignatureHash(scriptCode, mtx, NOT_AN_INPUT, SIGHASH_ALL, 0,
//          consensusBranchId);
      dataToBeSigned = null;
    } catch (Exception ex) {
      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Could not construct signature hash: " + ex.getMessage());
    }

    // Create Sapling spendAuth and binding signatures
    for (int i = 0; i < spends.size(); i++) {
      byte[] result = null;
      Librustzcash.librustzcashSaplingSpendSig(
          spends.get(i).expsk.getAsk(),
          spends.get(i).alpha,
          dataToBeSigned,
//          mtx.vShieldedSpend[i].spendAuthSig.data());
          result);
    }

    long valueBalance = 0;
    byte[] bindingSig = null;
    Librustzcash.librustzcashSaplingBindingSig(
        ctx,
//        mtx.valueBalance,
        valueBalance,
        dataToBeSigned,
//        mtx.bindingSig.data()
        bindingSig
    );

    Librustzcash.librustzcashSaplingProvingCtxFree(ctx);

    // Transparent signatures
//    for (int nIn = 0; nIn < mtx.vin.size(); nIn++) {
//
//    }

    return null;
  }

  public static SpendDescriptionCapsule generateSpendProof(SpendDescriptionInfo spend,
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
    SpendDescriptionCapsule sdesc = new SpendDescriptionCapsule();
    sdesc.setValueCommitment(cv);
    sdesc.setRk(rk);
    sdesc.setZkproof(zkproof);
    sdesc.setAnchor(spend.anchor);
    sdesc.setNullifier(nf);
    return sdesc;
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

  public static class TransactionBuilderResult {

  }
}
