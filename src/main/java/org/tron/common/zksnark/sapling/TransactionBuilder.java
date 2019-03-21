package org.tron.common.zksnark.sapling;

import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.tron.common.utils.ByteArray;
import org.tron.common.zksnark.merkle.IncrementalMerkleVoucherContainer;
import org.tron.common.zksnark.sapling.address.ExpandedSpendingKey;
import org.tron.common.zksnark.sapling.address.PaymentAddress;
import org.tron.common.zksnark.sapling.note.BaseNote.Note;
import org.tron.common.zksnark.sapling.note.BaseNotePlaintext.NotePlaintext;
import org.tron.common.zksnark.sapling.note.BaseNotePlaintext.SaplingNotePlaintextEncryptionResult;
import org.tron.common.zksnark.sapling.note.SaplingNoteEncryption;
import org.tron.common.zksnark.sapling.note.SaplingOutgoingPlaintext;
import org.tron.common.zksnark.sapling.transaction.OutDesc;
import org.tron.common.zksnark.sapling.transaction.SpendDescription;

public class TransactionBuilder {

  //  CKeyStore keystore;
  //  CMutableTransaction mtx;

  String from;
  List<SpendDescriptionInfo> spends;
  List<OutputDescriptionInfo> outputs;
  //  List<TransparentInputInfo> tIns;

  //  Optional<pair<byte[], PaymentAddress>> zChangeAddr;

  //  Optional<CTxDestination> tChangeAddr;

  // Throws if the anchor does not match the anchor used by
  // previously-added Sapling spends.
  void AddNoteSpend(
      ExpandedSpendingKey expsk,
      Note note,
      byte[] anchor,
      IncrementalMerkleVoucherContainer voucher) {
    spends.add(new SpendDescriptionInfo(expsk, note, anchor, voucher));
    //    mtx.valueBalance += note.value;

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

    //
    // Sapling spends and outputs
    //

    ProvingContext ctx = Librustzcash.librustzcashSaplingProvingCtxInit();

    // Create Sapling SpendDescriptions
    for (SpendDescriptionInfo spend : spends) {
      byte[] cm = spend.note.cm();
      byte[] nf = spend.note.nullifier(spend.expsk.fullViewingKey(), spend.voucher.position());

      if (ByteArray.isEmpty(cm) || ByteArray.isEmpty(nf)) {
        Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
        throw new RuntimeException("Spend is invalid");
      }

      byte[] voucherPath = spend.voucher.path().encode();

      SpendDescription sdesc = new SpendDescription();
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
          sdesc.cv,
          sdesc.rk,
          sdesc.zkproof.value)) {
        Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
        throw new RuntimeException("Spend proof failed");
      }

      sdesc.anchor = spend.anchor;
      sdesc.nullifier = nf;
      // todo: add sdesc into tx
      //      mtx.vShieldedSpend.push_back(sdesc);
    }

    // Create Sapling OutputDescriptions
    for (OutputDescriptionInfo output : outputs) {
      byte[] cm = output.note.cm();
      if (ByteArray.isEmpty(cm)) {
        Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
        throw new RuntimeException("Output is invalid");
      }

      NotePlaintext notePlaintext = new NotePlaintext(output.note, output.memo);

      Optional<SaplingNotePlaintextEncryptionResult> res = notePlaintext.encrypt(output.note.pk_d);
      if (!res.isPresent()) {
        Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
        throw new RuntimeException("Failed to encrypt note");
      }

      SaplingNotePlaintextEncryptionResult enc = res.get();
      SaplingNoteEncryption encryptor = enc.noteEncryption;

      OutDesc odesc = new OutDesc();
      if (!Librustzcash.librustzcashSaplingOutputProof(
          ctx,
          encryptor.esk,
          output.note.d.getData(),
          output.note.pk_d,
          output.note.r,
          output.note.value,
          odesc.cv,
          odesc.zkproof.value)) {
        Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
        throw new RuntimeException("Output proof failed");
      }

      odesc.cm = cm;
      odesc.ephemeralKey = encryptor.epk;
      odesc.encCiphertext = enc.encCiphertext;

      SaplingOutgoingPlaintext outPlaintext =
          new SaplingOutgoingPlaintext(output.note.pk_d, encryptor.esk);
      odesc.outCiphertext = outPlaintext.encrypt(output.ovk, odesc.cv, odesc.cm, encryptor);
      //todo: add odesc into tx
//      mtx.vShieldedOutput.push_back(odesc);
    }
    return null;
  }

  public class SpendDescriptionInfo {

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
  public class OutputDescriptionInfo {

    public byte[] ovk;

    public Note note;
    public byte[] memo; // 256
  }

  class TransactionBuilderResult {

  }
}
